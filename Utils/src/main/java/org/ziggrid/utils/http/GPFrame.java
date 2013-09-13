package org.ziggrid.utils.http;

public class GPFrame {
	final int opcode;
	final byte[] data;

	public GPFrame(int opcode, byte[] data) {
		this.opcode = opcode;
		this.data = data;
	}

	@Override
	public String toString() {
		return "Frame[" + opcode + ":" + (data!=null?data.length:0) + "]";
	}
}
