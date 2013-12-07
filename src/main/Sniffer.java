package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import proxy.ProxyListener;
import proxy.ProxyServer;
import proxy.ProxyServerThread;

/**
 * dumps anything that is sent over the proxy into log files
 * 
 * @author Robbe
 *
 */

public class Sniffer implements ProxyListener {
	
	HashMap<Thread, OutputStream> streams;
	String dir;
	
	public void start() throws IOException {
		ProxyServer proxy;
		
		streams = new HashMap<Thread, OutputStream>();
		dir = "session " + System.currentTimeMillis();
		
		try {
			new File(dir).mkdir();
			proxy = new ProxyServer(80);
			proxy.setListener(this);
			proxy.start();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void closed(ProxyServerThread sender) {
		try {
			streams.get(sender).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void request(ProxyServerThread sender, String request, HashMap<String, String> header) {
		try {
			OutputStream out = new FileOutputStream(dir + "/" + System.currentTimeMillis()+".dump");
						
			streams.put(sender, out);			
			out.write(request.getBytes());
			out.write("\n".getBytes());
			out.write(header.toString().getBytes());
			out.write("\n\n".getBytes());
			out.flush();
			
			sender.setRequestFork(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void response(ProxyServerThread sender, String response, HashMap<String, String> header) {		
		try {
			OutputStream out = streams.get(sender);
			
			out.write("\n\n--------------------------------------------------------------------------------\n\n".getBytes());
			out.write(response.getBytes());
			out.write("\n".getBytes());
			out.write(header.toString().getBytes());
			out.write("\n\n".getBytes());
			out.flush();
			
			sender.setResponseFork(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new Sniffer().start();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

}
