package org.ziggrid.utils.http.ws;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.websocket.WebSocket;

import org.ziggrid.utils.http.GPResponse;

public class WSWriter extends WebSocket {
	private final GPResponse response;

	public WSWriter(GPResponse response, AtmosphereConfig atmosphereConfig) {
		super(atmosphereConfig);
		this.response = response;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public WebSocket write(AtmosphereResponse r, String data)
			throws IOException {
		return write(data);
	}

	@Override
	public WebSocket write(String data) throws IOException {
		response.writeTextMessage(data);
		return this;
	}

	@Override
	public WebSocket write(AtmosphereResponse r, byte[] data) throws IOException {
		response.writeBinaryMessage(data, 0, data.length);
		return this;
	}

	@Override
	public WebSocket write(AtmosphereResponse r, byte[] data, int offset, int length) throws IOException {
		return write(data, offset, length);
	}

	@Override
	public WebSocket write(byte[] data, int offset, int length) throws IOException {
		response.writeBinaryMessage(data, offset, length);
		return this;
	}

	@Override
	public WebSocket writeError(AtmosphereResponse r, int errorCode, String message) throws IOException {
		response.writeClose(errorCode, message);
		return this;
	}

	@Override
	public WebSocket flush(AtmosphereResponse r) throws IOException {
		response.flush();
		return this;
	}
	
	@Override
	public void close(AtmosphereResponse r) throws IOException {
		close();
	}

	@Override
	public void close() {
		response.close();
	}
}
