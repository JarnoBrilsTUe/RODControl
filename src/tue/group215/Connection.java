package tue.group215;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class that handles the connection to the Arduino
 * 
 * @author group 215
 */
public class Connection {
	private static final int PORT = 2001;

	private Socket socket;
	private PrintWriter out;
	private InputReader reader;

	/**
	 * Constructor, establishes socket and streams
	 * 
	 * @param address
	 *            Server address in string form
	 * @param l
	 *            ChangeListener that fires when reading
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Connection(String address, ChangeListener l) throws UnknownHostException, IOException {
		socket = new Socket(address, PORT);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					socket.close(); // close socket on program termination
				} catch (IOException e) {
					/* failed */ }
			}
		});
		out = new PrintWriter(socket.getOutputStream(), true);
		reader = new InputReader(new BufferedReader(new InputStreamReader(socket.getInputStream())));
		reader.addChangeListener(l);
		new Thread(reader).start();
	}

	/**
	 * Closes all streams and the socket
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		out.close();
		reader.reading = false;
		socket.close();
	}

	/**
	 * Writes a string to the output stream, terminated by a line break, and
	 * flushes it
	 * 
	 * @param message
	 */
	public void write(String message) {
		out.print(message);
		out.flush();
	}

	/**
	 * Runnable reader that passes a ChangeEvent to the added listeners, when a
	 * new line has been read
	 * 
	 * @author group 215
	 */
	private class InputReader implements Runnable {
		private BufferedReader stream;
		private List<ChangeListener> listeners;
		private boolean reading;

		/**
		 * Constructor, sets up variables
		 * 
		 * @param in
		 *            BufferedReader that acts as a source of the data
		 */
		private InputReader(BufferedReader in) {
			this.stream = in;
			this.listeners = new ArrayList<ChangeListener>();
			this.reading = true;
		}

		/**
		 * Method to add a ChangeListener to the list
		 * 
		 * @param l
		 *            The ChangeListener to be added
		 */
		private void addChangeListener(ChangeListener l) {
			this.listeners.add(l);
		}

		/**
		 * The method that is run on the thread. While still requested, it reads
		 * lines from the stream and passes a corresponding ChangeEvent to all
		 * added listeners. Eventually closes the stream.
		 */
		@Override
		public void run() {
			while (reading) {
				ChangeEvent evt;
				try {
					String message = stream.readLine();
					if (message != null) {
						evt = new ChangeEvent(message);
						for (ChangeListener l : listeners) {
							l.stateChanged(evt);
						}
					}
				} catch (SocketException e) {
					System.err.println("Error: " + e.toString() + ", closing connection");
					try {
						close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * This method closes the stream and socket when the program ends
		 */
		@Override
		protected void finalize() {
			try {
				stream.close();
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
