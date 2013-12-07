package http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class HttpOutputStream extends BufferedOutputStream {
	
	private static final byte[] LINEBREAK = {'\r','\n'};
	private static final Charset CHARSET = Charset.forName("US-ASCII");

	public HttpOutputStream(OutputStream arg0, int arg1) {
		super(arg0, arg1);
	}

	public HttpOutputStream(OutputStream arg0) {
		super(arg0);
	}

	public void write(HashMap<String, String> header) throws IOException {
		for (Map.Entry<String, String> entry : header.entrySet()) {
			write(entry.getKey());
			write(": "); // TODO valid HTTP?
			write(entry.getValue());
			newLine();
		}
		newLine();
	}
	
	public void writeLine(String line) throws IOException {
		write(line);
		newLine();
	}
	
	public void write(String str) throws IOException {
		write(str.getBytes(CHARSET));		
	}
	
	private void newLine() throws IOException {
		write(LINEBREAK);
	}
	
}
