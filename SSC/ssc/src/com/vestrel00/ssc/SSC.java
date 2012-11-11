package com.vestrel00.ssc;

import java.io.IOException;
import java.net.UnknownHostException;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Main class for Simple Secure Chat (SSC)
 * This is the client implementation of SSC.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSC implements ApplicationListener {

	// Control Frames per sec (FPS)
	public static final long FPS = 40L;
	public static final long FRAME_INTERVAL = 1000000000L / FPS;
	public static long lastFrame = 0L;
	
	// Client
	public static SSCClient client;

	@Override
	public void create() {
		try {
			client = new SSCClient("127.0.0.1", 8080);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void dispose() {

	}

	@Override
	public void render() {
		if (TimeUtils.nanoTime() - lastFrame > FRAME_INTERVAL) {
			lastFrame = TimeUtils.nanoTime();
			Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
			Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			
			

		}
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
