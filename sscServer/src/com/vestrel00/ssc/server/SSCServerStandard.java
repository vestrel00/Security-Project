package com.vestrel00.ssc.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * An implementation of an SSCServer.
 * @author Estrellado, Vandolf
 * @see SSCServer
 *
 */
public class SSCServerStandard implements SSCServer{

	private ServerSocket server;
	private List<SSCServerService> clientServices;
	private boolean isListening;

	public SSCServerStandard(int port) throws IOException {
		server = new ServerSocket(port);
		clientServices = new ArrayList<SSCServerService>();
		isListening = true;
	}

	public void start() {
		while (isListening) {
			try {
				Socket newClient = server.accept();
				System.out.println("Recieved a new client");
				clientServices.add(new SSCSServiceStandard(newClient));
				new Thread(clientServices.get(clientServices.size() - 1)).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			stopListening();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopListening() throws IOException {
		isListening = false;
		server.close();
		for(int i=0; i<clientServices.size();i++){
			clientServices.get(i).stopService();
		}
	}



}