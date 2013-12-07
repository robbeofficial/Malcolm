package proxy;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;

/**
 * very basic forwarding http proxy server
 * @author Robbe
 *
 */
public class ProxyServer extends Thread {

	private ServerSocket serverSocket;
	private PrintStream debug;
	private boolean running;
	private ProxyListener listener; // TODO allow multiple listeners
	
	public ProxyServer(int port, PrintStream debug) throws IOException {		
		this.debug = debug;
		listener = null;
		dump("proxy server running on port : " + port);
		serverSocket = new ServerSocket(port);
	}
	
	public ProxyServer(int port) throws IOException {
		this(port, null);
	}
	
	public void close() {		
		// ensure socket to be closed
		try {
			serverSocket.close();
		} catch (Exception e) {}
		dump("proxy server closed");
	}	
	
	private void dump(String message) {
		if (debug != null) {
			debug.println("ProxyServer(" + Thread.currentThread().getId() + "): " + message);
		}
	}
	
	public void setListener(ProxyListener listener) {
		this.listener = listener;
	}	
	
	@Override
	public void run() {
		running = true;
		try {
			while(running) {
				new ProxyServerThread(serverSocket.accept(), listener, debug).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}	
}
