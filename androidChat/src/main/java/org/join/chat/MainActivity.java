package org.join.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {

	private Intent toChat;

	public static final String KEY_METHOD = "method";
	public static final String KEY_NAME = "name";
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_PORT = "port";

	private EditText nameEdit, addressEdit, portEdit;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		nameEdit = (EditText) findViewById(R.id.nameEdit);
		addressEdit = (EditText) findViewById(R.id.addressEdit);
		portEdit = (EditText) findViewById(R.id.portEdit);

		toChat = new Intent(this, ChatActivity.class);
	}

	public void login(View v) {
		toChat.putExtra(KEY_METHOD, ChatActivity.METHOD_CLIENT);
		toChat.putExtra(KEY_NAME, nameEdit.getText().toString().trim());
		toChat.putExtra(KEY_ADDRESS, addressEdit.getText().toString().trim());
		startActivity(toChat);
	}

	public void server(View v) {
		toChat.putExtra(KEY_METHOD, ChatActivity.METHOD_SERVER);
		toChat.putExtra(KEY_PORT, portEdit.getText().toString().trim());
		startActivity(toChat);
	}

}