package net.alexyoung91.accesslogserver;

import java.io.*;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

class Server extends WebSocketServer {
	
	private static Apache2AccessLog log;
	private static LogWatcher watcher;
	private static Sender sender;
	
	public Server(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}
	
	public Server(InetSocketAddress address) {
		super(address);
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		Server.log( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected." );
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn + " disconnected");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println(conn + ": " + message);
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		System.out.println("received fragment: " + fragment);
	}
	
	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if(conn != null) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}
	
	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToAll( String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( text );
			}
		}
	}
	
	public static void log(String msg) {
		System.out.println("SERVER: " + msg);
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {		 
		WebSocketImpl.DEBUG = false;
		int port = 8887; // 843 flash policy port
		try {
				port = Integer.parseInt(args[0]);
		} catch ( Exception ex ) {
			//
		}
		
		log = new Apache2AccessLog();
		log.read();
		
		watcher = new LogWatcher(log);
		watcher.start();
		
		Server s = new Server(port);
		s.start();
		Server.log("Started on port: " + s.getPort());
		
		sender = new Sender(s, log);
		sender.start();

		BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String in = sysin.readLine();
			s.sendToAll(in);
			if (in.equals("/exit")) {
				s.stop();
				break;
			} else if (in.equals("/restart")) {
				s.stop();
				s.start();
				break;
			}
		}
	}
}
