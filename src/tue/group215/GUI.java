package tue.group215;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;

public class GUI extends JFrame {
	private static final long serialVersionUID = 6563453375981273792L;

	public static final String lineBreak = System.getProperty("line.separator");

	public static final boolean debug = false;
	
	public static final boolean keyboardMode = true;

	private static String address = "192.168.50.248";

	private JPanel contentPanel;
	private JPanel camPanel;
	private JTextArea logArea;
	private Connection conn;
	private CommandProcessor commandProcessor;
	private ControllerProcessor controllerProcessor;
	private RODStatus status;

	public static void main(String[] args) {
		new GUI();
	}

	public GUI() {
		super("ROD Control - group 215");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			/* ignore */ }

		this.setLayout(new GridBagLayout());
		GridBagConstraints c0 = new GridBagConstraints();
		c0.fill = GridBagConstraints.BOTH;
		c0.weightx = c0.weighty = 1.0;

		contentPanel = new JPanel();
		contentPanel.setPreferredSize(new Dimension(640, 480));
		contentPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		logArea = new JTextArea();
		JScrollPane logPanel = new JScrollPane(logArea);
		logPanel.setAutoscrolls(true);
		logArea.setEditable(false);
		DefaultCaret caret = (DefaultCaret) logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		TextAreaOutputStream out = new TextAreaOutputStream(logArea);
		JTextField inputField = new JTextField();
		inputField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String command = inputField.getText();
				if (conn != null)
					commandProcessor.executeCommand(command);
				logArea.append(command + "\n");
				inputField.setText("");
			}
		});
		if (keyboardMode) {
			inputField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					update();
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					update();
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
					update();
				}
				
				private void update() {
					String command = inputField.getText();
					if (command.length() > 0 && conn != null) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								commandProcessor.executeShortCommand(command);
								logArea.append(command + "\n");
								inputField.setText("");
							}
						});
					}
				}
			});
		}
		JTextField addressField = new JTextField(address);
		JButton connectBtn = new JButton("Connect");
		connectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				address = addressField.getText();
				try {
					setupConnection();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		c.weightx = 1.0;
		contentPanel.add(addressField, c);
		c.weightx = 0;
		c.gridx = 1;
		contentPanel.add(connectBtn, c);
		c.weightx = c.weighty = 1.0;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = 1;
		contentPanel.add(logPanel, c);
		c.gridy = 2;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		contentPanel.add(inputField, c);

		this.add(contentPanel, c0);

		c0.gridx = 1;
		camPanel = new JPanel();
		camPanel.setPreferredSize(new Dimension(640, 480));
		this.add(camPanel, c0);

		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(out));

		this.setVisible(true);
		this.pack();
	}

	private void setupConnection() throws IOException {
		if (conn != null) {
			conn.close();
		}

		status = new RODStatus();

		try {
			if (debug) {
				address = "127.0.0.1";
			}
			conn = new Connection(address, new InputChangeListener(logArea));
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
		}

		commandProcessor = new CommandProcessor(conn, status);

		if (!keyboardMode)
			controllerProcessor = new ControllerProcessor(commandProcessor);

		if (!debug) {
			try {
				Webcam.setDriver(new IpCamDriver());
				IpCamDeviceRegistry.register("camera", "http://" + address + ":8080/?action=stream", IpCamMode.PUSH);
				WebcamPanel cam = new WebcamPanel(Webcam.getWebcams().get(Webcam.getWebcams().size() - 1));
				cam.setPreferredSize(camPanel.getPreferredSize());
				camPanel.add(cam);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		this.setVisible(true);
		this.pack();

		if (!keyboardMode)
			controllerProcessor.start();
	}

	private class InputChangeListener implements ChangeListener {
		private JTextArea textArea;

		private InputChangeListener(JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			String message = (String) e.getSource();
			this.textArea.append(message + lineBreak);
		}
	}

	private class TextAreaOutputStream extends OutputStream {
		private final JTextArea textArea;
		private final StringBuilder sb = new StringBuilder();

		public TextAreaOutputStream(final JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}

		@Override
		public void write(int b) throws IOException {
			if (b == '\r')
				return;

			if (b == '\n') {
				final String text = sb.toString() + "\n";
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						textArea.append(text);
					}
				});
				sb.setLength(0);
			}
			sb.append((char) b);
		}
	}

}
