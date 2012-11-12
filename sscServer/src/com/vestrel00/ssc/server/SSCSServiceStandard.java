package com.vestrel00.ssc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.vestrel00.ssc.server.interf.SSCServerService;
import com.vestrel00.ssc.server.protocols.SSCProtocol;

/**
 * An implementation of the SSCServerService.
 * @author Estrellado, Vandolf
 * @see SSCServerService
 *
 */
public class SSCSServiceStandard implements SSCServerService {

	private SSCProtocol protocol;
	private Socket client;
	private BufferedReader in;
	private PrintWriter out;
	private boolean inService;

	public SSCSServiceStandard(Socket client) throws IOException {
		this.client = client;
		inService = true;
		openIO();
		protocol = new SSCProtocol(in, out);
	}
	
	public void openIO() throws IOException {
		if (out == null) {
			out = new PrintWriter(client.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
		} else {
			closeIO();
			openIO();
		}
	}
	
	public void closeIO() throws IOException{
		out.close();
		in.close();
		out = null;
		in = null;
	}

	public void stopService() throws IOException {
		inService = false;
		closeIO();
		client.close();
	}

	@Override
	public void run() {
		while (inService) {
			if(!protocol.work())
				inService = false;
		}
		try {
			stopService();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}