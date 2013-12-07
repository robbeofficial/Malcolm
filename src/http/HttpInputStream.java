package http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class HttpInputStream extends BufferedInputStream {

	public HttpInputStream(InputStream in, int size) {
		super(in, size);
	}

	public HttpInputStream(InputStream in) {
		super(in); 
	}
	
	public HashMap<String, String> readHeader() throws IOException {
		HashMap<String, String> header = new HashMap<String, String>();
		String line;
		String[] kvpair;
		
		while( !(line = readLine()).isEmpty() ) {
			kvpair = line.split(": "); // TODO valid HTTP?
			header.put(kvpair[0].toLowerCase(), kvpair[1]);			
		}
		
		return header;			
	}
	
	public String readLine() throws IOException {
		char chr;
		StringBuilder line = new StringBuilder();
		
		while ( (chr = (char)read()) != '\n' ) {
			line.append(chr);
		}
		
		return line.toString().trim();
	}
}
