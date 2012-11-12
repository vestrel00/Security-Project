package com.vestrel00.ssc.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;
import com.vestrel00.ssc.server.protocols.SSCProtocol;

/**
 * An implementation of the SSCServerService.
 * 
 * @author Estrellado, Vandolf
 * @see SSCServerService
 * 
 */
public class SSCSServiceStandard implements SSCServerService {

	private SSCProtocol protocol;
	private SSCServer serverClass;
	private Socket client;
	private DataInputStream in;
	private DataOutputStream out;
	private boolean inService;

	/**
	 * Create the service.
	 * 
	 * @param serverClass
	 *            The server which launched this service.
	 * @param client
	 *            The client that will be serviced.
	 * @throws IOException
	 */
	public SSCSServiceStandard(SSCServer serverClass, Socket client)
			throws IOException {
		this.serverClass = serverClass;
		this.client = client;
		inService = true;
		openIO();
	}

	public void openIO() {
		if (out == null) {
			try {
				out = new DataOutputStream(client.getOutputStream());
				in = new DataInputStream(client.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			closeIO();
			openIO();
		}
	}

	public void closeIO() {
		try {
			out.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out = null;
		in = null;
	}

	public void stopService() {
		inService = false;
		closeIO();
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		protocol = new SSCProtocol(this, in, out);
		while (inService) {
			if (!protocol.work())
				inService = false;
		}
		stopService();
	}

	public SSCServer getServerClass() {
		return serverClass;
	}

	@Override
	public void forwardMessageTo(SSCServerService service, byte[] message) {
		// TODO Auto-generated method stub
		
	}

}
