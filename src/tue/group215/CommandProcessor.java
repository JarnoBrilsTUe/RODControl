package tue.group215;

public class CommandProcessor {
	protected Connection conn;
	protected RODStatus status;

	// The driving commands
	private static final String stop = "s";
	private static final String forward = "f";
	private static final String backward = "b";
	private static final String left = "l";
	private static final String right = "r";

	// The camera gimbal commands
	private static final String gimbalHor = "x";
	private static final String gimbalVer = "y";

	// The arm servo commands
	private static final String bottomRotate = "1";
	private static final String lowerExtend = "2";
	private static final String upperExtend = "3";
	private static final String handRotate = "4";
	private static final String handClose = "5";

	public CommandProcessor(Connection conn, RODStatus status) {
		this.conn = conn;
		this.status = status;
	}

	public void executeCommand(String command) {
		String[] comm = command.split(" ");
		String message;
		int value;
		switch (comm[0].toLowerCase()) {
		// driving commands
		case "left":
			if (comm.length < 2) {
				return;
			}
			message = left + comm[1];
			write(message);
			return;
		case "right":
			if (comm.length < 2) {
				return;
			}
			message = right + comm[1];
			write(message);
			return;
		case "forward":
			if (comm.length < 2) {
				return;
			}
			message = forward + comm[1];
			write(message);
			return;
		case "backward":
			if (comm.length < 2) {
				return;
			}
			message = backward + comm[1];
			write(message);
			return;
		case "stop":
			message = stop;
			write(message);
			return;

		// camera gimbal commands
		case "lookhor":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 1)
				value = 1;
			else if (value > 179)
				value = 179;
			message = gimbalHor + value;
			write(message);
			status.gimbalX = value;
			return;
		case "lookver":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 1)
				value = 1;
			else if (value > 179)
				value = 179;
			message = gimbalVer + value;
			write(message);
			status.gimbalY = value;
			return;

		// servo commands
		case "rotatebot":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 1)
				value = 1;
			else if (value > 179)
				value = 179;
			if (value == status.servoBaseRot)
				return;
			message = bottomRotate + value;
			write(message);
			status.servoBaseRot = value;
			return;
		case "rotatehand":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 1)
				value = 1;
			else if (value > 179)
				value = 179;
			message = handRotate + value;
			write(message);
			status.servoHandRot = value;
			return;
		case "extendlow":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 1)
				value = 1;
			else if (value > 179)
				value = 179;
			message = lowerExtend + value;
			write(message);
			status.servoLower = value;
			return;
		case "extendup":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 1)
				value = 1;
			else if (value > 179)
				value = 179;
			message = upperExtend + value;
			write(message);
			status.servoUpper = value;
			return;
		case "hand":
			if (comm.length < 2) {
				return;
			}
			value = Integer.parseInt(comm[1]);
			if (value < 116)
				value = 116;
			else if (value > 170)
				value = 170;
			message = handClose + value;
			write(message);
			status.servoFingers = value;
			return;

		// TODO: custom commands
		}

		// if command not processed, send raw command
		write(command);
	}

	private void write(String command) {
		//if (!command.startsWith("m"))
			System.out.println(command);
		if (!GUI.debug) {
			conn.write(command);
		}
	}
	
	public void flush() {
		conn.flush();
	}

	public void executeShortCommand(String command) {
		switch(command) {
		//camera
		case "w":
			executeCommand("lookver " + (int) --status.gimbalY);
			break;
		case "s":
			executeCommand("lookver " + (int) ++status.gimbalY);
			break;
		case "a":
			executeCommand("lookhor " + (int) ++status.gimbalX);
			break;
		case "d":
			executeCommand("lookhor " + (int) --status.gimbalX);
			break;
			
		//movement
		case "8":
			executeCommand("forward " + 1000);
			break;
		case "2":
			executeCommand("backward " + 1000);
			break;
		case "4":
			executeCommand("left " + 1000);
			break;
		case "6":
			executeCommand("right " + 1000);
			break;
		case "5":
			executeCommand("stop");
			break;
			
		//hand
		case "n":
			executeCommand("hand " + --status.servoFingers);
			break;
		case "m":
			executeCommand("hand " + ++status.servoFingers);
			break;
			
		//hand rotation
		case "j":
			executeCommand("rotatehand " + --status.servoHandRot);
			break;
		case "k":
			executeCommand("rotatehand " + ++status.servoHandRot);
			break;
			
		//arm extension
		case "i":
			executeCommand("extendup " + --status.servoUpper);
			break;
		case "o":
			executeCommand("extendup " + ++status.servoUpper);
		}
		conn.flush();
	}

}
