package de.mas.jnus.lib.utils;

import lombok.Getter;
import lombok.Setter;

public class ByteArrayBuffer{
	@Getter public byte[] buffer;
	@Getter @Setter int lengthOfDataInBuffer;
		
	public ByteArrayBuffer(int length){
		buffer = new byte[(int) length];
	}

	public int getSpaceLeft() {
		return buffer.length - getLengthOfDataInBuffer();
	}

	public void addLengthOfDataInBuffer(int bytesRead) {
		lengthOfDataInBuffer += bytesRead;		
	}

	public void resetLengthOfDataInBuffer() {
		setLengthOfDataInBuffer(0);
	}
}
