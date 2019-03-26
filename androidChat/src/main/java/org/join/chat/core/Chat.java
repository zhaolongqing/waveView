package org.join.chat.core;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Observable;

public abstract class Chat extends Observable {

	public static final int SEV_ON = 0;
	public static final int SEV_OFF = 1;
	public static final int CLT_CONNECT = 2;
	public static final int CLT_DISCONNECT = 3;
	public static final int MSG_SEND = 4;
	public static final int MSG_RECEIVE = 5;
	public static final int ERROR = 6;

	/** 缓存区大小 */
	protected static final int BUFFERSIZE = 1024 * 10;
	/** 字符编码 */
	protected static final String CHARSET = "UTF-8";

	/** 字符编码器 */
	protected static CharsetEncoder encoder;
	/** 字符解码器 */
	protected static CharsetDecoder decoder;

	static {
		encoder = Charset.forName(CHARSET).newEncoder();
		decoder = Charset.forName(CHARSET).newDecoder();
	}

	/** 当前状态 */
	protected int status;

	/** 获得当前状态 */
	public int getStatus() {
		return status;
	}

	/**
	 * 通知状态改变
	 * @param status 状态
	 * @param arg 参数
	 */
	protected void notifyStateChanged(int status, Object arg) {
		this.status = status;
		notifyStateChanged(arg);
	}

	/**
	 * 通知状态改变
	 * @param arg 参数
	 */
	protected void notifyStateChanged(Object arg) {
		setChanged();
		notifyObservers(arg);
	}

}
