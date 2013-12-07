package proxy;

import http.HttpInputStream;
import http.HttpOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ProxyServerThread extends Thread {
	
	private Socket client;
	private Socket server;
	
	private HttpInputStream clientIn;
	private HttpOutputStream clientOut;
	
	private HttpInputStream serverIn;
	private HttpOutputStream serverOut;
	
	// optional fork streams for request and response body 
	private OutputStream responseFork; 
	private OutputStream requestFork;
	
	private ProxyListener listener;
	
	private PrintStream debug;
	
	private static int DEFAULT_BUFFER_SIZE = 4096;
	
	public ProxyServerThread(Socket client, ProxyListener listener, PrintStream debug) throws IOException {
		this.client = client;
		this.debug = debug;
		this.listener = listener;

		clientIn = new HttpInputStream(client.getInputStream());
		clientOut = new HttpOutputStream(client.getOutputStream());
		
		dump("connetced to client: " + client.getRemoteSocketAddress());
	}

	public void setResponseFork(OutputStream responseFork) {
		this.responseFork = responseFork;
	}
	
	public void setRequestFork(OutputStream requestFork) {
		this.requestFork = requestFork;
	}

	public OutputStream getResponseFork() {
		return responseFork;
	}

	public OutputStream getRequestFork() {
		return requestFork;
	}

	private int forward(HttpInputStream in, HttpOutputStream out, OutputStream fork, int length) throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		int bytesRead = 0;
		int bytesTotal = 0;
		
		boolean noLength = length < 0;
		
		while (noLength || bytesTotal < length) {
			// read from input
			if (noLength) {				
				//dump("read start");
				bytesRead = in.read(buffer);
				//dump("read stop");
			} else {
				bytesRead = in.read(buffer, 0, Math.min(DEFAULT_BUFFER_SIZE, length - bytesTotal) );
			}
			
			if (bytesRead < 0) { // the end of input stream has been reached
				break;
			}			
			bytesTotal += bytesRead; // count bytes that were actually forwarded
			
			// send to output
			try {
				// forward to HTTP stream
				out.write(buffer, 0, bytesRead);
				out.flush();
				
				// forward to fork stream
				if (fork != null) {
					fork.write(buffer, 0, bytesRead);
					fork.flush();
				}
			} catch (SocketException se) { // output peer disconnected
				dump("peer disconnected");
				break;
			}
		}
		
		return bytesTotal;
	}

	@Override
	public void run() {
		int bytes = 0;
		try {
			// read request and header from client
			String request = clientIn.readLine();			
			HashMap<String, String> requestHeader = clientIn.readHeader();
			
			dump("request : " + request);
			dump("request header : " + requestHeader);
			
			// extract header information from client
			String host = requestHeader.get("host");
			String requestLengthStr = requestHeader.get("content-length");
			int requestLength = requestLengthStr != null ? Integer.parseInt(requestLengthStr) : -1; 
			
			// establish server connection
			server = new Socket(host, 80); // FIXME throws "ConnectException: Connection timed out" once in a while
			serverIn = new HttpInputStream(server.getInputStream());
			serverOut = new HttpOutputStream(server.getOutputStream());
			
			dump("connected to server : " + server.getRemoteSocketAddress());
			
			// forward request from client to server
			serverOut.writeLine(request); // forward request
			serverOut.write(requestHeader); // forward header
			serverOut.flush();
						
			// tell anybody what happened
			if (listener != null) {
				listener.request(this, request, requestHeader);
			}
			
			if (requestLength > 0) { // note: request body is only forwarded if content length was given
				forward(clientIn, serverOut, requestFork, requestLength);
			}
			dump("forwarded request of length " + requestLength + "("+requestLength+")" );
			
			// read response and header from server
			String response = serverIn.readLine();
			HashMap<String, String> responseHeader = serverIn.readHeader();			
			dump("response : " + response);
			dump("response header : " + responseHeader);
			
			String responseLengthStr = responseHeader.get("content-length");
			int responseLength = responseLengthStr != null ? Integer.parseInt(responseLengthStr) : -1;			
			
			// notify response to listener
			if (listener != null) {
				listener.response(this, response, responseHeader);
			}			
			
			// forward response from server to client
			clientOut.writeLine(response);
			clientOut.write(responseHeader);
			clientOut.flush();			
			
			// forward response body 
			bytes = forward(serverIn, clientOut, responseFork, responseLength);
			dump("forwarded response of length " + bytes + "("+responseLength+")");
			
			// close all we have
			serverIn.close();
			serverOut.close();
			server.close();
			
			clientIn.close();
			clientOut.close();
			client.close();
			
			// notify close to listener
			if (listener != null) {
				listener.closed(this);
			}
			
			dump("closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// dumps message to debug out
	private void dump(String message) {
		if (debug != null) {
			debug.println("ProxyServerThread("+getId()+"): " + message);
		}
	}	
	
	

}
