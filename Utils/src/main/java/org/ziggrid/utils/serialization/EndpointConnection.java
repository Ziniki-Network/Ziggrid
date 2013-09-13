package org.ziggrid.utils.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.ziggrid.utils.exceptions.UtilException;

public class EndpointConnection {

	private final Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;

	public EndpointConnection(Socket socket) {
		this.socket = socket;
		this.ois = null;
		this.oos = null;
	}

	public InputStream getInput() {
		try {
			return socket.getInputStream();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public OutputStream getOutput() {
		try {
			return socket.getOutputStream();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void send(ControlRequest msg) {
		try {
			oos.writeObject(msg);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public ObjectInputStream getOIS() {
		if (ois != null)
			return ois;
		try
		{
			ois = new ObjectInputStream(socket.getInputStream());
			return ois;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public ObjectOutputStream getOOS() {
		if (oos != null)
			return oos;
		try
		{
			oos = new ObjectOutputStream(socket.getOutputStream());
			return oos;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public void close() {
		try
		{
			socket.close();
		}
		catch (IOException ex)
		{
			;
		}
	}
}
