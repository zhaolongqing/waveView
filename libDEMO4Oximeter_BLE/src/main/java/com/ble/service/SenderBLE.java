package com.ble.service;

import java.io.IOException;
import com.creative.base.Isender;

public class SenderBLE implements Isender{
	
    private BLEHelper mHelper;

	public SenderBLE(BLEHelper helper) {
		mHelper = helper;
	}
	
	@Override
	public void send(byte[] d) throws IOException {
		mHelper.write(d);
	}

	@Override
	public void close() {
		mHelper = null;
	}

}
