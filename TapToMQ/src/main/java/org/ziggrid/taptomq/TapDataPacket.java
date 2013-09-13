package org.ziggrid.taptomq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapDataPacket implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger("TapDataPacket");
	
	private static final long serialVersionUID = -6933704262194288773L;
	private final String key;
	private final String value;

	public TapDataPacket(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
	
	public static byte[] serialize(TapDataPacket packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(packet);
        return baos.toByteArray();
    }

	public static TapDataPacket deserialize(byte[] byteArray) {
		ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bais);
			return (TapDataPacket) ois.readObject();
		} catch (IOException e) {
			logger.error("Error reading byte array input stream", e);
		} catch (ClassNotFoundException e) {
			logger.error("Unable to deserialize byteArray as TapDataPacket", e);
		}
		return null;
	}
}
