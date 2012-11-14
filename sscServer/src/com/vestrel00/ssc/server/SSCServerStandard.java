package com.vestrel00.ssc.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * An implementation of an SSCServer.
 * 
 * @author Estrellado, Vandolf
 * @see SSCServer
 * 
 */
public class SSCServerStandard implements SSCServer {

	private ServerSocket server;
	private List<SSCServerService> clientServices;
	private SSCServerBuffer buffer;
	private Random rand;
	private boolean isListening;

	public SSCServerStandard(int port, int maxClientCount,
			int maxClientBufferSize) throws IOException {
		server = new ServerSocket(port);
		clientServices = new ArrayList<SSCServerService>();
		buffer = new SSCServerBuffer(maxClientCount, maxClientBufferSize);
		rand = new Random();
		isListening = true;
	}

	public void start() {
		while (isListening) {
			try {
				Socket newClient = server.accept();
				clientServices.add(new SSCSServiceStandard(this, newClient));
				new Thread(clientServices.get(clientServices.size() - 1))
						.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		finish();
	}

	public void finish() {
		isListening = false;
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < clientServices.size(); i++) {
			clientServices.get(i).stopService();
		}
	}

	public SSCServerBuffer getBuffer() {
		return buffer;
	}

	@Override
	public SSCServerService getServiceByName(String clientName) {
		for (SSCServerService service : clientServices) {
			if (service.getClientName().contentEquals(clientName))
				return service;
		}
		return null;
	}

	@Override
	public List<SSCServerService> getClientServices() {
		return clientServices;
	}

	@Override
	public boolean clientIsOnline(String clientName, boolean checkBusy) {
		for (SSCServerService serv : clientServices)
			if (serv.getClientName() != null
					&& serv.getClientName().contentEquals(clientName))
				if (checkBusy)
					return !serv.isInChat();
				else
					return true;
		return false;
	}

	public int getSessionId() {
		boolean retry = true;
		int id = -1;
		while (retry) {
			retry = false;
			id = 555555 + rand.nextInt(444444);
			if (clientServices.size() == 0)
				return id;
			for (SSCServerService service : clientServices) {
				if (service.getServiceId() == id)
					retry = true;
			}
		}
		return id;
	}

	@Override
	public void removeService(String clientName) {
		int index = -1;
		for (int i = 0; i < clientServices.size(); i++)
			if (clientServices.get(i).getClientName().contentEquals(clientName)) {
				index = i;
				break;
			}
		if (index != -1)
			clientServices.remove(index);
	}

}
