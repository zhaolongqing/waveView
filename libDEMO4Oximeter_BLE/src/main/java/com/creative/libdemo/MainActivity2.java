package com.creative.libdemo;

import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bde.parentcyTransport.ACSUtility.blePort;
import com.creative.FingerOximeter.FingerOximeter;
import com.creative.FingerOximeter.IFingerOximeterCallBack;
import com.creative.base.BLEReader;
import com.creative.base.BLESender;
import com.creative.base.BaseDate.Wave;
import com.creative.bluetooth.ble.BLEOpertion;
import com.creative.bluetooth.ble.IBLECallBack;
import com.creative.draw.DrawPC300SPO2Rect;
import com.creative.draw.DrawThreadNW2;

/**
 * use ACSUtilityService. <br>
 * Open in AndroidManifest.xml <br>
 * draw wave by view
 */
public class MainActivity2 extends Activity {

	private static final String TAG="pod";
	private BLEOpertion mBleOpertion;
	private FingerOximeter mFingerOximeter;
	private TextView tv_SPO2, tv_PR, tv_PI,tv_BlueState;
	private ImageView iv_Pulse, iv_Battery;			
	
	/** 绘制波形*/
	private DrawThreadNW2 mDrawRunble;
	/** 绘制波形的线程 */
	private Thread mDrawThread = null;
	/** 血氧柱状图 */
	private DrawPC300SPO2Rect mDrawSPO2Rect;
	/** 血氧柱状图线程 */
	private Thread mDrawSPO2RectThread;
	
	/** 保存血氧波形数据, 用于绘制血氧柱状图 */
	public static List<Wave> SPO_RECT = new ArrayList<Wave>();
	/** 保存血氧波形数据,用于绘制血氧波形图*/
	public static List<Wave> SPO_WAVE = new ArrayList<Wave>();
	
	//----------- message -------------
	/** 血氧参数 */
	public static final byte MSG_DATA_SPO2_PARA = 0x01;	
	/** 血氧波形数据 */
	public static final byte MSG_DATA_SPO2_WAVE = 0x02;	
	/** 血氧搏动标记 */
	public static final byte MSG_DATA_PULSE = 0x03;	
	/** 取消搏动标记 */
	public static final byte RECEIVEMSG_PULSE_OFF = 0x04;
	/** 蓝牙状态信息 */
	public static final byte MSG_BLUETOOTH_STATE = 0x05;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pod_view2);
				
		try {
			mBleOpertion = new BLEOpertion(this, new BleCallBack());
		} catch (Exception e) {
			e.printStackTrace();
		}

		initView();
		
		android6_RequestLocation(this);
	}

	private void initView(){	
		tv_BlueState = (TextView) findViewById(R.id.blueState);
		tv_SPO2 = (TextView) findViewById(R.id.realplay_spo2_spo2);
		tv_PR = (TextView) findViewById(R.id.realplay_spo2_pr);
		tv_PI = (TextView) findViewById(R.id.realplay_spo2_pi);
		iv_Pulse = (ImageView) findViewById(R.id.realplay_spo2_pulse);
		iv_Battery = (ImageView) findViewById(R.id.realplay_spo2_battery);		
		mDrawSPO2Rect = (DrawPC300SPO2Rect)findViewById(R.id.realplay_spo2_draw_rect);		
		mDrawRunble = (DrawThreadNW2) findViewById(R.id.realplay_spo2_draw_wave);
		
		mDrawRunble.setmHandler(myHandler);
		
		findViewById(R.id.btnConn).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mBleOpertion.startDiscover();
				// ble.connect("D0:39:72:BC:58:D1");
				myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "Begin discovery").sendToTarget();
			}
		});
		findViewById(R.id.btnDiscon).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mBleOpertion.disConnect();
				myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "disConnect").sendToTarget();
			}
		});
	}
	
	private Handler myHandler = new Handler() {

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_BLUETOOTH_STATE: {//蓝牙状态信息
				tv_BlueState.setText((String) msg.obj);
				//Log.d(TAG, (String) msg.obj);
			}
				break;
			case MSG_DATA_SPO2_WAVE: { //波形数据
//				List<Wave> wave = (List<Wave>) msg.obj;
//				String showText = "wave=";
//				for (Iterator<Wave> iterator = wave.iterator(); iterator.hasNext();) {
//					Wave w = iterator.next();
//					showText += w.data + "---";
//				}
//				Log.i(TAG, showText);
				
//				SPO_RECT.addAll(wave);
//				SPO_WAVE.addAll(wave);
			}
				break;
			case MSG_DATA_SPO2_PARA:{ //波形参数
				//nStatus探头状态 ->true为正常 false为脱落
				Bundle bundle = msg.getData();
				if (!bundle.getBoolean("nStatus")) {
					mDrawRunble.cleanWaveData();
					myHandler.removeMessages(BATTERY_ZERO);
					break;
				}
				int nSpo2 = bundle.getInt("nSpO2");
				int nPR = bundle.getInt("nPR");
				float fPI = bundle.getFloat("fPI");
				float b = bundle.getFloat("nPower");
				int powerLevel = bundle.getInt("powerLevel");				

				int battery = 0;	
				if(powerLevel!=0){
					battery = powerLevel;
				}else {
					if (b < 2.5f) {
						battery = 0;
					} else if (b < 2.8f) {
						battery = 1;
					} else if (b < 3.0f)
						battery = 2;
					else
						battery = 3;
				}		
				
				setBattery(battery);
				setTVSPO2(nSpo2 + "");
				setTVPR(nPR + "");
				setTVPI(fPI+"");
			}
			break;
			case MSG_DATA_PULSE:{
				showPulse(true);
			}
			break;
			case RECEIVEMSG_PULSE_OFF: {
				showPulse(false);
			}
				break;
			default:break;
			}
		}
	};

	/**
	 * 蓝牙回调
	 */
	class BleCallBack implements IBLECallBack {

		@Override
		public void onFindDevice(final blePort port) {
			System.out.println("搜索到设备：" + port._device.getAddress() + "  " + port._device.getName());
			
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE,"Find device:" + port._device.getAddress() + "  " + port._device.getName() + " " /*+ port.devInfo*/)
					.sendToTarget();
			if ("PC-60NW-1".equals(port._device.getName().trim())||
					"POD".equals(port._device.getName().trim())||
					"PC-68B".equals(port._device.getName().trim())) {
				mBleOpertion.stopDiscover();
				myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "Begin connect").sendToTarget();
				new Thread() {

					@Override
					public void run() {
						super.run();
						mBleOpertion.connect(port);
					}
				}.start();
			}
		}

		@Override
		public void onConnected(blePort port) {
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "Connected success:" + port._device.getName()).sendToTarget();
			mFingerOximeter = new FingerOximeter(new BLEReader(mBleOpertion), new BLESender(mBleOpertion), new FingerOximeterCallBack());
			mFingerOximeter.Start();
			//send request for wave 发送波形请求
			mFingerOximeter.SetWaveAction(true);
		}

		@Override
		public void onConnectFail() {
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "Connected fail").sendToTarget();
			if (mFingerOximeter != null)
				mFingerOximeter.Stop();
			mFingerOximeter = null;
		}

		@Override
		public void onSended(boolean isSend) {
			System.out.println("send data success:" + isSend);
			//myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "isSend data:" + isSend).sendToTarget();
		}

		public void onDiscoveryCompleted(List<blePort> device) {
			System.out.println("onDiscoveryCompleted");
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "time out").sendToTarget();
		}

		@Override
		public void onDisConnect(blePort prot) {
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "disConnect").sendToTarget();
			mFingerOximeter.Stop();
			mFingerOximeter = null;
		}

		@Override
		public void onReadyForUse() {
			System.out.println("onReadyForUse");
		}

	}

	/**
	 * 收到的血氧仪数据
	 */
	class FingerOximeterCallBack implements IFingerOximeterCallBack {

		@Override
		public void OnGetSpO2Param(int nSpO2, int nPR, float fPI, boolean nStatus, int nMode, float nPower,int powerLevel) {			
			Message msg = myHandler.obtainMessage(MSG_DATA_SPO2_PARA);
			Bundle data = new Bundle();
			data.putInt("nSpO2", nSpO2);
			data.putInt("nPR", nPR);
			data.putFloat("fPI", fPI);
			data.putFloat("nPower", nPower);
			data.putBoolean("nStatus", nStatus); 
			data.putInt("nMode", nMode);
			data.putFloat("nPower", nPower);
			data.putInt("powerLevel", powerLevel);
			msg.setData(data);
			myHandler.sendMessage(msg);			
			//myHandler.obtainMessage(2, "数据--" + nSpO2 + " " + nPR + " " + nPI).sendToTarget();
		}

		@Override
		public void OnGetSpO2Wave(List<Wave> wave) {
			//myHandler.obtainMessage(MSG_DATA_SPO2_WAVE, wave).sendToTarget();
			
			//SPO_RECT.addAll(wave);
			SPO_WAVE.addAll(wave);
		}

		@Override
		public void OnGetDeviceVer(String hardVer,String softVer,String deviceName) {
			Log.d(TAG, "hardVer:"+hardVer+",softVer:"+softVer+",deviceName:"+deviceName);
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "device info,获取到设备信息:" + "hardVer:"+hardVer+",softVer:"+softVer).sendToTarget();
		}

		@Override
		public void OnConnectLose() {
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "Connected Lose").sendToTarget();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBleOpertion != null) {
			mBleOpertion.disConnect();
			mBleOpertion.closeACSUtility();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startDraw();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		pauseDraw();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		stopDraw();
	}
	
	//--------------------------------------------------------
	/**
	 * 开始绘图
	 */
	private void startDraw() {
		if (mDrawThread == null) {
			mDrawThread = new Thread(mDrawRunble, "DrawPC60NWThread");
			mDrawThread.start();
			mDrawSPO2RectThread = new Thread(mDrawSPO2Rect, "DrawPC300RectThread");
			mDrawSPO2RectThread.start();
		} else if (mDrawRunble.isPause()) {
			mDrawRunble.Continue();
			mDrawSPO2Rect.Continue();
		}
	}	
	
	/**
	 * 暂停绘图
	 */
	private void pauseDraw() {
		if (mDrawThread != null && !mDrawRunble.isPause()) {
			mDrawRunble.Pause();
			mDrawSPO2Rect.Pause();
		}
	}
	
	/***
	 * 停止绘图
	 */
	private void stopDraw() {
		if (!mDrawRunble.isStop()) {
			mDrawRunble.Stop();
			mDrawSPO2Rect.Stop();
		}
		mDrawThread = null;
		mDrawSPO2RectThread = null;
	}
	
	/**
	 * 电量等级
	 */
	private int batteryRes[] = { R.drawable.battery_0, R.drawable.battery_1, R.drawable.battery_2,
			R.drawable.battery_3 };

	/** 消息 电池电量为0 */
	private static final int BATTERY_ZERO = 0x302;

	/**
	 * 设置电量图标
	 */
	private void setBattery(int battery) {
		iv_Battery.setImageResource(batteryRes[battery]);
		if (battery == 0) {
			if (!myHandler.hasMessages(BATTERY_ZERO)) {
				myHandler.sendEmptyMessage(BATTERY_ZERO);
			}
		} else {
			iv_Battery.setVisibility(View.VISIBLE);
			if (myHandler.hasMessages(BATTERY_ZERO))
				myHandler.removeMessages(BATTERY_ZERO);
		}
	}
	
	
	/**
	 * 设置搏动标记
	 */
	private void showPulse(boolean isShow) {
		if (isShow) {
			iv_Pulse.setVisibility(View.VISIBLE);
			new Thread() {
				@Override
				public void run() {
					super.run();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					myHandler.sendEmptyMessage(RECEIVEMSG_PULSE_OFF);
				}
			}.start();
		} else {
			iv_Pulse.setVisibility(View.INVISIBLE);
		}
	}
	
	private void setTVSPO2(String data) {
		setTVtext(tv_SPO2, data);
	}

	private void setTVPR(String data) {
		setTVtext(tv_PR, data);
	}

	private void setTVPI(String data) {
		setTVtext(tv_PI, data);
	}
	
	/**
	 * 设置TextView显示的内容
	 */
	private void setTVtext(TextView tv, String msg) {
		if (tv != null) {
			if (msg != null && !msg.equals("")) {
				if (msg.equals("0") || msg.equals("0.0")) {
					tv.setText("--");
				} else {
					tv.setText(msg);
				}
			}
		}
	}
	
	/**
	 * android6.0 Bluetooth, need to open location for bluetooth scanning
	 * android6.0 蓝牙扫描需要打开位置信息
	 */
	private void android6_RequestLocation(final Context context){
		if (Build.VERSION.SDK_INT >= 23) {			
			// BLE device need to open location
	        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
	        		&& !isGpsEnable(context)) {	                     
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setCancelable(false);
				builder.setTitle("Prompt")
						.setIcon(android.R.drawable.ic_menu_info_details)
						.setMessage("Android6.0 need to open location for bluetooth scanning")
						.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).setPositiveButton("OK", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {								
								Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								// startActivityForResult(intent,0);
								context.startActivity(intent);
							}
						});  
				builder.show();
	        }
			
	        //request permissions
			int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
			if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
				//判断是否需要 向用户解释，为什么要申请该权限
				if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
					Toast.makeText(context,"need to open location info for discovery bluetooth device in android6.0 version，otherwise find not！", Toast.LENGTH_LONG).show();
				//请求权限
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
			}
		}	
		
	}
	
	// whether or not location is open, 位置是否打开
	public final boolean isGpsEnable(final Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if (gps || network) {
			return true;
		}			
		return false;
	}

}
