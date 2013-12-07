package proxy;

import java.util.HashMap;

public interface ProxyListener {
	
	/**
	 * client made a request
	 * 
	 * @param sender	sender thread
	 * @param request	request string
	 * @param header	request header
	 */
	void request(ProxyServerThread sender, String request, HashMap<String, String> header);
	
	/**
	 * server responded to clients request
	 * 
	 * @param sender	sender thread
	 * @param response	response string
	 * @param header	response header
	 */
	void response(ProxyServerThread sender, String response, HashMap<String, String> header);
	
	/**
	 * connection was closed
	 * 
	 * @param sender	sender thread
	 */
	void closed(ProxyServerThread sender);	
}
