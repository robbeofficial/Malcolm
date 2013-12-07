package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ConsoleFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private JTextArea textArea;
	private PrintStream printStream;

	public ConsoleFrame() {
		textArea = new JTextArea(20, 80);
		textArea.setBackground(Color.BLACK);
		textArea.setForeground(Color.LIGHT_GRAY);		

		printStream = new AreaPrintStream(textArea);

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(
				new JScrollPane(textArea,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
				BorderLayout.CENTER);
		pack();
	}

	class AreaPrintStream extends PrintStream {
		AreaPrintStream(JTextArea textArea) {
			super(new AreaOutputStream(textArea));
		}
	}

	class AreaOutputStream extends OutputStream {
		JTextArea textArea;

		public AreaOutputStream(JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void write(final int b) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// TODO works only with 1 byte char codes
					textArea.append( new String(new byte[]{(byte)b}) );
					textArea.setCaretPosition(textArea.getText().length());
				}
			});
		}
	}

	public PrintStream getPrintStream() {
		return printStream;
	}
}