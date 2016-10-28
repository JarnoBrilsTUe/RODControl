package tue.group215;

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

/**
 * Handles input from a controller and executes appropriate command
 * Assumes a DualShock 3 PS3 controller, connected using ScpToolkit, is used
 * 
 * @author group 215
 */
public class ControllerProcessor extends Thread {
	private CommandProcessor cmd;
	private Controller controller;

	// Values for the joysticks (x and y coordinates from -1 to 1, for left and
	// right joystick)
	private float leftX;
	private float leftY;
	private float rightX;
	private float rightY;

	private boolean xBtn; // X
	private boolean oBtn; // O
	private boolean tBtn; // Triangle
	private boolean sBtn; // Square

	private boolean l1; // L1
	private boolean r1; // L2

	private float lr2; // L2 & R2

	// The directional buttons value
	private float direction;

	private boolean leftUpdated;
	private boolean rightUpdated;
	private boolean dirUpdated;
	private boolean lrUpdated;
	
	private int lastMove;

	public ControllerProcessor(CommandProcessor cmd) {
		this.cmd = cmd;

		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		for (int i = 0; i < ca.length; i++) {
			/* Check if the type of the controller is gamepad */
			if (ca[i].getType().equals(Type.GAMEPAD)) {
				controller = ca[i];
				break;
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			controller.poll();

			EventQueue queue = controller.getEventQueue();
			Event event = new Event();

			float rightVal;
			rightVal = controller.getComponent(Identifier.Axis.RX).getPollData();
			if (rightVal > 0.3 || rightVal < -0.3) {
				rightX = rightVal;
				rightUpdated = true;
			}
			rightVal = controller.getComponent(Identifier.Axis.RY).getPollData();
			if (rightVal > 0.3 || rightVal < -0.3) {
				rightY = rightVal;
				rightUpdated = true;
			}
			float val = controller.getComponent(Identifier.Axis.Z).getPollData();
			if (val < -0.1 || val > 0.1) {
				lr2 = val;
				lrUpdated = true;
			}

			if (controller.getComponent(Identifier.Button._0).getPollData() == 1.0) {
				xBtn = true;
			}
			if (controller.getComponent(Identifier.Button._1).getPollData() == 1.0) {
				oBtn = true;
			}
			if (controller.getComponent(Identifier.Button._2).getPollData() == 1.0) {
				sBtn = true;
			}
			if (controller.getComponent(Identifier.Button._3).getPollData() == 1.0) {
				tBtn = true;
			}
			if (controller.getComponent(Identifier.Button._4).getPollData() == 1.0) {
				l1 = true;
			}
			if (controller.getComponent(Identifier.Button._5).getPollData() == 1.0) {
				r1 = true;
			}
			if (controller.getComponent(Identifier.Axis.POV).getPollData() != 0.0) {
				direction = controller.getComponent(Identifier.Axis.POV).getPollData() * 4; // transform
																								// the
																								// data
																								// to
																								// 4
																								// points

				dirUpdated = true;
			}

			while (queue.getNextEvent(event)) {
				Component comp = event.getComponent();
				Identifier id = comp.getIdentifier();

				if (comp.isAnalog()) {
					if (id.equals(Identifier.Axis.X)) {
						leftX = event.getValue();
						if (leftX > -0.05 && leftX < 0.05)
							leftX = 0;
						leftUpdated = true;
					} else if (id.equals(Identifier.Axis.Y)) {
						leftY = event.getValue();
						if (leftY > -0.05 && leftY < 0.05)
							leftY = 0;
						leftUpdated = true;
					}
				}
			}

			processData();

			try {
				Thread.sleep(1000 / 20); // wait for 1 20th of a second
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void processData() {
		if (leftUpdated && !lrUpdated) {
			// 4 variables that control the motors (left and right wheel,
			// forward and backward)
			int leftF = 0;
			int leftB = 0;
			int rightF = 0;
			int rightB = 0;

			int throttle = -(int) (leftY * 255f);
			if (throttle >= 0) { // move forward
				if (leftX < -0.1) { // move to the left
					leftF = (int) ((leftX + 1) * throttle);
					rightF = throttle;
				} else if (leftX > 0.1) { // move to the right
					leftF = throttle;
					rightF = (int) ((1 - leftX) * throttle);
				} else { // move straight forward
					leftF = throttle;
					rightF = throttle;
				}
			} else { // move backward
				if (leftX < -0.1) { // move to the left
					leftB = (int) ((leftX + 1) * -throttle);
					rightB = -throttle;
				} else if (leftX > 0.1) { // move to the right
					leftB = -throttle;
					rightB = (int) ((1 - leftX) * -throttle);
				} else { // move straight backward
					leftB = -throttle;
					rightB = -throttle;
				}
			}

			// System.out.println(leftF + "," + leftB + "," + rightF + "," +
			// rightB);

			// Put all values into one 32 bit integer
			int value = ((leftF & 0xFF) << 24) | ((leftB & 0xFF) << 16) | ((rightF & 0xFF) << 8) | (rightB & 0xFF);

			if (value != lastMove) {
				lastMove = value;
				cmd.executeCommand("m" + value);
			}
		}

		if (rightUpdated) {
			double xOrig = cmd.status.gimbalX;
			double yOrig = cmd.status.gimbalY;

			double x = xOrig - (rightX * 5); // x is inverted
			double y = yOrig + (rightY * 5);

			cmd.status.gimbalX = x;
			cmd.status.gimbalY = y;

			if ((int) xOrig != (int) x) {
				cmd.executeCommand("lookhor " + (int) x);
			}

			if ((int) yOrig != (int) y) {
				cmd.executeCommand("lookver " + (int) y);
			}
		}

		if (dirUpdated) {
			System.out.println(direction);
			int dir = (int) direction;
			switch (dir) {
			case 1: // up - extend lower servo
				cmd.executeCommand("extendlow " + (cmd.status.servoLower + 1));
				break;
			case 2: // right
				// do nothing
				break;
			case 3: // down - retract lower servo
				cmd.executeCommand("extendlow " + (cmd.status.servoLower - 1));
				break;
			case 4: // left
				// do nothing
				break;
			}
		}

		if (lrUpdated) {
			double zOrig = cmd.status.servoBaseRot;

			double z = zOrig + lr2;

			cmd.status.servoBaseRot = z;

			if ((int) zOrig != (int) z) {
				cmd.executeCommand("rotatebot " + (int) z);
			}
		}

		if (xBtn) { // retract upper servo
			cmd.executeCommand("extendup " + (++cmd.status.servoUpper));
			xBtn = false;
		}
		if (oBtn) { // close hand
			cmd.executeCommand("hand " + (--cmd.status.servoFingers));
			oBtn = false;
		}
		if (sBtn) { // open hand
			cmd.executeCommand("hand " + (++cmd.status.servoFingers));
			sBtn = false;
		}
		if (tBtn) { // extend upper servo
			cmd.executeCommand("extendup " + (--cmd.status.servoUpper));
			tBtn = false;
		}
		if (l1) { // rotate hand left
			cmd.executeCommand("rotatehand " + (--cmd.status.servoHandRot));
			l1 = false;
		}
		if (r1) { // rotate hand right
			cmd.executeCommand("rotatehand " + (++cmd.status.servoHandRot));
			r1 = false;
		}

		leftUpdated = false;
		rightUpdated = false;
		dirUpdated = false;
		lrUpdated = false;
		
		cmd.flush();
	}

}
