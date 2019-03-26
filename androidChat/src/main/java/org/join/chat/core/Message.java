package org.join.chat.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 字符消息
 * @author Join
 */
public class Message {

	static class MessageHolder {
		static Message instance = new Message();
	}

	public static Message getInstance() {
		return MessageHolder.instance;
	}

	public static final String SEPARATOR = "`";

	/** 客户端登录服务器的消息类型，用以通知登录名等 */
	public static final int MSG_1 = 1; // 1`msg
	/** 服务器通知各客户端新登录者信息的消息类型 */
	public static final int MSG_2 = 2; // 2`msg`fromIp
	/** 服务器回复新登录者所有在线客户端信息的消息类型 */
	public static final int MSG_3 = 3; // 3`ip`msg`ip`msg`...
	/** 客户端发送所有人信息的消息类型（或服务器发送消息） */
	public static final int MSG_4 = 4; // 4`msg
	/** 客户端发送指定人信息的消息类型 */
	public static final int MSG_5 = 5; // 5`msg`toIp`toIp`...
	/** 服务器转发发送者信息的消息类型 */
	public static final int MSG_6 = 6; // 6`msg`fromIp
	/** 客户端通知服务器退出的消息类型 */
	public static final int MSG_7 = 7; // 7
	/** 服务器通知各客户端退出者信息的消息类型 */
	public static final int MSG_8 = 8; // 8`fromIp

	private int type; // 类型
	private String msg; // 消息
	private InetSocketAddress fromIp; // 发送者ip信息
	private ArrayList<InetSocketAddress> toIpList; // 接收者ip信息
	private HashMap<InetSocketAddress, String> onLineMap; // 在线者的信息

	/**
	 * 构造字符消息
	 */
	public Message() {
	}

	/**
	 * 构造字符消息
	 * @param type 信息类型
	 */
	public Message(int type) {
		this.type = type;
	}

	/**
	 * 构造字符消息
	 * @param message 字符信息
	 */
	public Message(String message) {
		create(message);
	}

	/** 由字符信息构建对象值 */
	public void create(String message) {
		int type = parseType(message);
		if (type <= 0 || type >= 9) {
			throw new IllegalArgumentException(
					"The message's type was out of range?");
		}
		this.type = type;
		initValues(message);
	}

	/** 初始化消息的值 */
	private void initValues(String message) {
		switch (this.type) {
		case MSG_1:
		case MSG_4:
			this.msg = message.split(SEPARATOR)[1];
			break;
		case MSG_2:
		case MSG_6:
			String[] msg2 = message.split(SEPARATOR);
			this.msg = msg2[1];
			this.fromIp = toIpAddress(msg2[2]);
			break;
		case MSG_3:
			String[] msg3 = message.split(SEPARATOR);
			onLineMap = new HashMap<InetSocketAddress, String>();
			for (int i = 1; i < msg3.length; i += 2) {
				onLineMap.put(toIpAddress(msg3[i]), msg3[i + 1]);
			}
			break;
		case MSG_5:
			String[] msg5 = message.split(SEPARATOR);
			this.msg = msg5[1];
			toIpList = new ArrayList<InetSocketAddress>();
			for (int i = 2; i < msg5.length; i++) {
				toIpList.add(toIpAddress(msg5[i]));
			}
			break;
		case MSG_7:
			break;
		case MSG_8:
			this.fromIp = toIpAddress(message.split(SEPARATOR)[1]);
			break;
		}
	}

	/** 提取消息的类型 */
	private int parseType(String message) {
		int type = -1;
		try {
			type = Integer.parseInt(message.substring(0, 1));
		} catch (NumberFormatException e) {
		}
		return type;
	}

	/** 返回字符串 */
	@Override
	public String toString() {
		switch (this.type) {
		case MSG_1:
		case MSG_4:
			return type + SEPARATOR + msg;
		case MSG_2:
		case MSG_6:
			return type + SEPARATOR + msg + SEPARATOR + toIpString(fromIp);
		case MSG_3:
			StringBuffer msg3 = new StringBuffer(MSG_3 + "");
			Iterator<Entry<InetSocketAddress, String>> it = onLineMap
					.entrySet().iterator();
			while (it.hasNext()) {
				Entry<InetSocketAddress, String> entry = it.next();
				msg3.append(SEPARATOR);
				msg3.append(toIpString(entry.getKey()));
				msg3.append(SEPARATOR);
				msg3.append(entry.getValue());
			}
			return msg3.toString();
		case MSG_5:
			StringBuffer msg5 = new StringBuffer(MSG_5 + SEPARATOR + msg);
			if (null != toIpList) {
				for (InetSocketAddress address : toIpList) {
					msg5.append(SEPARATOR);
					msg5.append(toIpString(address));
				}
			}
			return msg5.toString();
		case MSG_7:
			return MSG_7 + "";
		case MSG_8:
			return MSG_8 + SEPARATOR + toIpString(fromIp);
		}
		return super.toString();
	}

	public String toIpString(InetSocketAddress address) {
		return address.getAddress().getHostAddress() + ":" + address.getPort();
	}

	public InetSocketAddress toIpAddress(String ipStr) {
		String ipAddress[] = ipStr.split(":");
		return new InetSocketAddress(ipAddress[0],
				Integer.parseInt(ipAddress[1]));
	}

	/* getters & setters */

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public InetSocketAddress getFromIp() {
		return fromIp;
	}

	public void setFromIp(InetSocketAddress fromIp) {
		this.fromIp = fromIp;
	}

	public ArrayList<InetSocketAddress> getToIpList() {
		return toIpList;
	}

	public void setToIpList(ArrayList<InetSocketAddress> toIpList) {
		this.toIpList = toIpList;
	}

	public HashMap<InetSocketAddress, String> getOnLineMap() {
		return onLineMap;
	}

	public void setOnLineMap(HashMap<InetSocketAddress, String> onLineMap) {
		this.onLineMap = onLineMap;
	}

}
