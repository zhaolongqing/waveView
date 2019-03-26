package org.join.chat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.join.chat.core.ChatClient;
import org.join.chat.core.ChatServer;
import org.join.chat.core.Message;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends TabActivity implements OnTabChangeListener,
		Observer {

	public static final int METHOD_CLIENT = 1;
	public static final int METHOD_SERVER = 2;

	private static final String BLANK = " ";

	/** 启动方式：客户端、服务器 */
	private int startMethod;
	/** 在线客户端信息 */
	private HashMap<InetSocketAddress, String> onLineMap;

	private ChatClient client;
	private ChatServer server;

	private LinearLayout msgLayout;
	private ListView listView;
	private EditText msgEdit;

	private boolean isConnected = false;

	private static final int UPDATE_SHOW_MESSAGE = 0;
	private static final int UPDATE_TOAST_MESSAGE = 1;
	private static final int UPDATE_LIST_DATA = 2;

	private static final int MSG_COUNT = 20;
	private Handler mHandler = new Handler();

	private class UpdateRunnable implements Runnable {

		private String msg;
		private int what;

		public UpdateRunnable(String msg, int what) {
			this.msg = msg;
			this.what = what;
		}

		@Override
		public void run() {
			switch (what) {
			case UPDATE_SHOW_MESSAGE:
				TextView textView = new TextView(getBaseContext());
				textView.setText(msg);
				if (msgLayout.getChildCount() >= MSG_COUNT) {
					msgLayout.removeViewAt(0);
				}
				msgLayout.addView(textView);
				break;
			case UPDATE_TOAST_MESSAGE:
				Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT)
						.show();
				break;
			case UPDATE_LIST_DATA:
				if (null != onLineMap) {
					ArrayList<String> dataList = new ArrayList<String>();
					Iterator<Entry<InetSocketAddress, String>> it = onLineMap
							.entrySet().iterator();
					while (it.hasNext()) {
						Entry<InetSocketAddress, String> entry = it.next();
						dataList.add(entry.getValue()
								+ BLANK
								+ Message.getInstance().toIpString(
										entry.getKey()));
					}
					listView.setAdapter(new ArrayAdapter<String>(
							getBaseContext(),
							android.R.layout.simple_expandable_list_item_1,
							dataList));
				}
				break;
			}
		}

	};

	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		createTabs(); // 创建标签页
		startMethod = getIntent().getIntExtra(MainActivity.KEY_METHOD, 0);

		msgLayout = (LinearLayout) findViewById(R.id.msgLayout);
		listView = (ListView) findViewById(R.id.listView);
		msgEdit = (EditText) findViewById(R.id.msgEdit);

		final String address = getLocalIpAddress();
		if (null == address) {
			toastMessage("请先开启网络");
		} else {
			msgLayout.post(new Runnable() {
				@Override
				public void run() {
					start(startMethod, address); // 启动服务
				}
			});
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	/** 创建标签页 */
	private void createTabs() {
		TabHost tabHost = getTabHost();
		LayoutInflater.from(this).inflate(R.layout.chat,
				tabHost.getTabContentView(), true);
		tabHost.addTab(tabHost.newTabSpec("tab_chat").setIndicator("聊天室")
				.setContent(R.id.chatTab));
		tabHost.addTab(tabHost.newTabSpec("tab_info").setIndicator("当前在线")
				.setContent(R.id.infoTab));
		tabHost.setOnTabChangedListener(this);
	}

	/** 启动服务 */
	private void start(int startMethod, String ipAddress) {
		switch (startMethod) {
		case METHOD_CLIENT:
			/* 显示圈形进度 */
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage("连接服务器中...");
			progressDialog.setCancelable(false);
			progressDialog.show();
			/* 连接服务器 */
			String[] address = getIntent().getStringExtra(
					MainActivity.KEY_ADDRESS).split(":");
			client = new ChatClient(address[0], Integer.parseInt(address[1]));
			client.addObserver(this);
			client.start();
			setTitle(ipAddress);
			break;
		case METHOD_SERVER:
			String port = getIntent().getStringExtra(MainActivity.KEY_PORT);
			server = new ChatServer(Integer.parseInt(port));
			server.addObserver(this);
			server.start();
			setTitle(ipAddress + ":" + port);
			break;
		}
	}

	@Override
	public void onTabChanged(String tabId) {
	}

	/** 发送消息 */
	public void send(View v) {
		String msg = msgEdit.getText().toString().trim();
		if (msg.equals("")) {
			msgEdit.setError("请输入信息");
		} else {
			String newMsg = Message.MSG_4 + Message.SEPARATOR + msg;
			if (null != client) {
				client.send(newMsg);
			}
			if (null != server) {
				server.send(newMsg);
				showMessage("通知：" + msg);
			}
			msgEdit.setText("");
			hideSoftInput(); // 关闭软键盘
		}
	}

	@Override
	public void update(Observable observable, Object data) {
		switch (startMethod) {
		case METHOD_CLIENT:
			updateClient(observable, data);
			break;
		case METHOD_SERVER:
			updateServer(observable, data);
			break;
		}
	}

	/** 客户端状态更新 */
	private void updateClient(Observable observable, Object data) {
		ChatClient client = (ChatClient) observable;
		switch (client.getStatus()) {
		case ChatClient.CLT_CONNECT:
			// 发送登录消息
			Message msg1 = Message.getInstance();
			msg1.setType(Message.MSG_1);
			msg1.setMsg(getIntent().getStringExtra(MainActivity.KEY_NAME));
			client.send(msg1.toString());
			// 连接上后等服务器返回MSG_3再取消进度框等
			isConnected = true;
			break;
		case ChatClient.CLT_DISCONNECT:
			isConnected = false;
			if (null != progressDialog) {
				progressDialog.dismiss();
			}
			toastMessage("断开服务器连接");
			finish();
			break;
		case ChatClient.MSG_SEND:
			break;
		case ChatClient.MSG_RECEIVE:
			Message msg = Message.getInstance();
			msg.create((String) data);
			handleClientMsg(msg); // 处理消息
			break;
		case ChatClient.ERROR:
			showMessage("error : " + ((Exception) data).getMessage());
			break;
		}
	}

	/** 客户端信息处理 */
	private void handleClientMsg(Message msg) {
		int type = msg.getType();
		switch (type) {
		case Message.MSG_2:
			showMessage(msg.getMsg() + BLANK + msg.toIpString(msg.getFromIp())
					+ BLANK + "登录了");
			onLineMap.put(msg.getFromIp(), msg.getMsg());
			updateListData(); // 更新在线人员
			break;
		case Message.MSG_3:
			showMessage("连接上了服务器");
			onLineMap = msg.getOnLineMap();
			updateListData(); // 更新在线人员
			if (null != progressDialog) {
				progressDialog.dismiss();
			}
			break;
		case Message.MSG_4:
			showMessage("通知：" + msg.getMsg());
			break;
		case Message.MSG_6:
			showMessage(onLineMap.get(msg.getFromIp()) + BLANK + "：" + BLANK
					+ msg.getMsg());
			break;
		case Message.MSG_8:
			InetSocketAddress address = msg.getFromIp();
			showMessage(onLineMap.get(address) + BLANK
					+ Message.getInstance().toIpString(address) + BLANK + "退出了");
			onLineMap.remove(address);
			updateListData(); // 更新在线人员
			break;
		}
	}

	/** 服务器状态更新 */
	private void updateServer(Observable observable, Object data) {
		ChatServer server = (ChatServer) observable;
		switch (server.getStatus()) {
		case ChatServer.SEV_ON:
			showMessage("服务器开启了");
			onLineMap = new HashMap<InetSocketAddress, String>();
			break;
		case ChatServer.SEV_OFF:
			toastMessage("服务器关闭了");
			onLineMap = null;
			finish();
			break;
		case ChatServer.CLT_CONNECT:
			// InetSocketAddress address = (InetSocketAddress) arg;
			break;
		case ChatServer.CLT_DISCONNECT:
			quit((InetSocketAddress) data);
			break;
		case ChatServer.MSG_SEND:
			break;
		case ChatServer.MSG_RECEIVE:
			Message msg = Message.getInstance();
			msg.create(server.getReceiveMessage());
			msg.setFromIp((InetSocketAddress) data);
			handleServerMsg(msg); // 处理消息
			break;
		case ChatServer.ERROR:
			showMessage("error : " + ((Exception) data).getMessage());
			break;
		}
	}

	/** 客户端退出 */
	private void quit(InetSocketAddress address) {
		if (onLineMap.get(address) != null) {
			showMessage(onLineMap.get(address) + BLANK
					+ Message.getInstance().toIpString(address) + BLANK + "退出了");
			onLineMap.remove(address);
			Message msg = Message.getInstance();
			msg.setType(Message.MSG_8);
			msg.setFromIp(address);
			server.send(msg.toString());
			updateListData(); // 更新在线人员
		}
	}

	/** 服务器信息处理 */
	private void handleServerMsg(Message msg) {
		int type = msg.getType();
		InetSocketAddress formIp = msg.getFromIp();
		switch (type) {
		case Message.MSG_1:
			showMessage(msg.getMsg() + BLANK + msg.toIpString(formIp) + BLANK
					+ "登录了");
			onLineMap.put(formIp, msg.getMsg());
			updateListData(); // 更新在线人员
			/* 通知所有客户端新登录者信息 */
			msg.setType(Message.MSG_2);
			// msg.setFromIp(address);
			server.send(msg.toString());
			/* 返回登录客户端所有在线客户端信息（会覆盖前一个发送给address的信息） */
			msg.setType(Message.MSG_3);
			msg.setOnLineMap(onLineMap);
			server.send(msg.toString(), formIp);
			break;
		case Message.MSG_4:
			showMessage(onLineMap.get(msg.getFromIp()) + BLANK + "：" + BLANK
					+ msg.getMsg());
			msg.setType(Message.MSG_6);
			server.send(msg.toString());
			break;
		case Message.MSG_7:
			quit(formIp);
			break;
		}
	}

	/** 展现信息 */
	private void showMessage(String text) {
		mHandler.post(new UpdateRunnable(text, UPDATE_SHOW_MESSAGE));
	}

	/** 提示信息 */
	private void toastMessage(String text) {
		mHandler.post(new UpdateRunnable(text, UPDATE_TOAST_MESSAGE));
	}

	/** 获取当前IP地址 */
	private String getLocalIpAddress() {
		try {
			// 遍历网络接口
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				// 遍历IP地址
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					// 非回传地址时返回
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 更新在线人员 */
	private void updateListData() {
		mHandler.post(new UpdateRunnable(null, UPDATE_LIST_DATA));
	}

	/** 关闭软键盘 */
	private void hideSoftInput() {
		InputMethodManager imm = (InputMethodManager) this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
	}

	@Override
	protected void onDestroy() {
		if (null != client) {
			if (isConnected) {
				client.send(Message.MSG_7 + "");
			}
			client.close();
		}
		if (null != server) {
			server.close();
		}
		super.onDestroy();
	}

}
