package com.creative.libdemo;

import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
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
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.ble.service.BLEManager;
import com.ble.service.BluetoothLeService;
import com.ble.service.ReaderBLE;
import com.ble.service.SenderBLE;
import com.creative.FingerOximeter.AnalyseData.PC68BRecordDetail;
import com.creative.FingerOximeter.AnalyseData.PC68BRecordUnit;
import com.creative.FingerOximeter.FOSendCMDThread;
import com.creative.FingerOximeter.FingerOximeter;
import com.creative.FingerOximeter.IFingerOximeterCallBack;
import com.creative.FingerOximeter.IPC68BCallBack;
import com.creative.base.BaseDate.Wave;
import com.creative.draw.DrawPC300SPO2Rect;
import com.creative.draw.DrawThreadNW;

/**
 * POD,PC-60NW-1,PC-68B.
 * 
 * use BluetoothLeService.  <br>
 * you can modefy bluetooth by your need <br>
 * draw wave by view 
 */
public class MainActivity extends Activity{
	private static final String TAG ="MainActivity";
	
	private FingerOximeter mFingerOximeter;
	private TextView tv_SPO2, tv_PR, tv_PI,tv_BlueState;
	private ImageView iv_Pulse, iv_Battery;			
		
	/** 绘制波形*/
	private DrawThreadNW mDrawRunble;
	/** 绘制波形的线程 */
	private Thread mDrawThread = null;
	/** 血氧柱状图 */
	private DrawPC300SPO2Rect mDrawSPO2Rect;
	/** 血氧柱状图线程 */
	private Thread mDrawSPO2RectThread;
	
	/** 保存血氧波形数据, 用于绘制血氧柱状图 . list for drawing spo2 rect */
	public static List<Wave> SPO_RECT = new ArrayList<Wave>();
	/** 保存血氧波形数据,用于绘制血氧波形图 . list for drawing spo2 wave */
	public static List<Wave> SPO_WAVE = new ArrayList<Wave>();
	
	private BLEManager mManager;
		
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
	/** 导联脱落 */
	public static final byte MSG_PROBE_OFF = 0x06;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pod_view);
										
		initView();					
        initBLE();	             
        android6_RequestLocation(this);
	}

//----- pc68b, version4.7 send command -----------------------------	
	private boolean isPC80B;// set true if it was pc80b device
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        getMenuInflater().inflate(R.menu.pc68b, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    } 
	
	@Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action1:{	
            	FOSendCMDThread.query68b_version();
            }
            break;
            case R.id.action2:{
            	//read time
            	FOSendCMDThread.query68b_devTime_47();
            	//set time
            	//FOSendCMDThread.setting68b_devTime_47(2018, 2, 5, 12, 23, 40);
            }
            break;
            case R.id.action3:{
            	FOSendCMDThread.query68b_devSN_47();			
            }
            break;
            case R.id.action4:{
            	//read the alert param of device
            	FOSendCMDThread.setting68b_alertParam_47(0, 0, 0, 0, 0, 0, 0);
				//write alert param to device
            	//FOSendCMDThread.setting68b_alertParam_47(1, 1, 86, 55, 121, 1, 1);
            }
            break;
            case R.id.action5:{//get all record
            	FOSendCMDThread.get68b_record_47(0xffff);        	
            }
            break;
            case R.id.action6:{ 
            	//get one detail record by param. 
            	//num 0 mean the first record, num 2 mean the third record
            	FOSendCMDThread.get68b_record_47(0);
            }
            break;
            default:break;
        }
        return super.onContextItemSelected(item);
    }	
//------------------ pc68b command end ---------------------------------------	
	
	private void initView(){	
		tv_BlueState = (TextView) findViewById(R.id.blueState);
		tv_SPO2 = (TextView) findViewById(R.id.realplay_spo2_spo2);
		tv_PR = (TextView) findViewById(R.id.realplay_spo2_pr);
		tv_PI = (TextView) findViewById(R.id.realplay_spo2_pi);
		iv_Pulse = (ImageView) findViewById(R.id.realplay_spo2_pulse);
		iv_Battery = (ImageView) findViewById(R.id.realplay_spo2_battery);		
		mDrawSPO2Rect = (DrawPC300SPO2Rect)findViewById(R.id.realplay_spo2_draw_rect);			
		mDrawRunble = (DrawThreadNW) findViewById(R.id.realplay_spo2_draw_wave);	
		
		//set handler for updating UI
		mDrawRunble.setmHandler(myHandler);
		
		findViewById(R.id.btnConn).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {	
				if(mManager!=null){
					mManager.scanLeDevice(true);
				}
			}
		});
		findViewById(R.id.btnDiscon).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mManager!=null){
					mManager.disconnect();
				}		
			}
		});	
		Button btn_cmd = (Button) findViewById(R.id.btnCmd);
		btn_cmd.setOnCreateContextMenuListener(this);//给组件注册Context菜单
		btn_cmd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.showContextMenu();//单击直接显示Context菜单
			}
		});
		if(isPC80B){//PC-68B:67295
			btn_cmd.setVisibility(View.VISIBLE);
		}
	}
	
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_BLUETOOTH_STATE: {//蓝牙状态信息
				tv_BlueState.setText((String) msg.obj);
				//Log.d(TAG, (String) msg.obj);
			}
				break;
			case MSG_DATA_SPO2_PARA:{ //波形参数
				//nStatus探头状态 ->true为正常 false为脱落
				//probe status ->true noraml, false off
				Bundle bundle = msg.getData();
				if (!bundle.getBoolean("nStatus")) {
					SPO_RECT.clear();
					SPO_WAVE.clear();
					mDrawSPO2Rect.clean();
					mDrawRunble.cleanWaveData();			
					
					myHandler.removeMessages(BATTERY_ZERO);
					myHandler.sendEmptyMessage(MSG_PROBE_OFF);
					pauseDraw();
					//探头脱落，过一段时间，系统会自动断开蓝牙
					// auto disconnect bluetooth after probe off a period
					//mBluetoothLeService.disconnect();
					break;
				}
				int nSpo2 = bundle.getInt("nSpO2");
				int nPR = bundle.getInt("nPR");
				float fPI = bundle.getFloat("fPI");
				float b = bundle.getFloat("nPower");
				int powerLevel = bundle.getInt("powerLevel");
				//Log.d(TAG, "b："+b+",powerLevel:"+powerLevel);
				
				int battery = 0;							
				if(b!=0){
					if (b < 2.5f) {
						battery = 0;
					} else if (b < 2.8f) {
						battery = 1;
					} else if (b < 3.0f)
						battery = 2;
					else
						battery = 3;
				}else {
					battery = powerLevel;
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
			case MSG_PROBE_OFF:{	
				Toast.makeText(MainActivity.this, "probe off", Toast.LENGTH_SHORT).show();
			}
				break;
//			case 290:{
//				scrollLog((String)msg.obj);
//			}
//			break;
			default:break;
			}
		}
	};

	
	/**
	 * 收到的血氧仪数据
	 * received FingerOximeter of data
	 * pc68b软件版本不是4.7定制版的，和POD,PC-60NW-1设备，不用实现IPC68BCallBack 这个接口.
	 * 
	 * You will implements the interface "IPC68BCallBack" and "IFingerOximeterCallBack" if it is PC-68B device,
	 * only implements the interface "IFingerOximeterCallBack" if it is POD or PC-60NW-1
	 */
	class FingerOximeterCallBack implements IFingerOximeterCallBack ,IPC68BCallBack{

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

		//血氧波形数据采样频率：50Hz，每包发送 5 个波形数据，即每 1 秒发送 10 包波形数据
		//参数 waves 对应一包数据
		//spo2 sampling rate is 50hz, 5 wave data in a packet, 
		//send 10 packet 1/s. param "waves" is 1 data packet
		@Override
		public void OnGetSpO2Wave(List<Wave> waves) {
			//Log.d(TAG, "wave.size:"+waves.size()); // size = 5
			SPO_RECT.addAll(waves);	
			SPO_WAVE.addAll(waves);
			
			//myHandler.obtainMessage(MSG_DATA_SPO2_WAVE, waves).sendToTarget();						
		}

		@Override
		public void OnGetDeviceVer(String hardVer,String softVer,String deviceName) {
			Log.d(TAG, "hardVer:"+hardVer+",softVer:"+softVer+",deviceName:"+deviceName);
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "device info,获取到设备信息:" + "hardVer:"+hardVer+",softVer:"+softVer).sendToTarget();
		}

		@Override
		public void OnConnectLose() {
			myHandler.obtainMessage(MSG_BLUETOOTH_STATE, "connect lost,连接丟失").sendToTarget();
		}
//---------- PC68B version4.7 --------------
		@Override
		public void onGetAlertParam_68b(int alertIsOn,int spo2Lo,int PRLo,int PRHi,int pulseBeepIsOn,int sensorAlertIsOn) {
			Log.i(TAG, "68b alertIsOn:"+alertIsOn+",spo2Lo:"+spo2Lo+",PRLo:"+PRLo+",PRHi："+PRHi+",pulseBeepIsOn:"+pulseBeepIsOn+",sensorAlertIsOn:"+sensorAlertIsOn);
		}

		@Override
		public void onGetAllRecordSize_68b(int size) {
			Log.i(TAG, "68b size:"+size);
		}

		@Override
		public void onGetAllRecord_68b(List<PC68BRecordDetail> datas) {
			for(PC68BRecordDetail data:datas){
				Log.i(TAG, "num:"+data.num+",time:"+data.time);	
			}
		}

		@Override
		public void onGetDeviceSN_68b(String sn) {
			Log.i(TAG, "68b sn:"+sn);
		}

		@Override
		public void onGetOneRecordDetail_68b(List<PC68BRecordUnit> datas) {
			for(PC68BRecordUnit data:datas){
				Log.i(TAG, "68b one frame:"+data.toString());	
			}
		}

		@Override
		public void onGetTime_68b(String time) {
			Log.i(TAG, "68b time:"+time);
		}

		@Override
		public void onSettingTime_68b(int isSuccess) {
			Log.i(TAG, "68b isSuccess:"+isSuccess);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        registerReceiver(mGattUpdateReceiver, BLEManager.makeGattUpdateIntentFilter());	
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
		unregisterReceiver(mGattUpdateReceiver);
	}
	
	
	/** 开始绘图 */
	private void startDraw() {
		if (mDrawThread == null) {
			mDrawThread = new Thread(mDrawRunble, "DrawPOD_Thread");
			mDrawThread.start();
			mDrawSPO2RectThread = new Thread(mDrawSPO2Rect, "DrawPODRect_Thread");
			mDrawSPO2RectThread.start();
		} else if (mDrawRunble.isPause()) {
			mDrawRunble.Continue();
			mDrawSPO2Rect.Continue();
		}
		
	}	
	
	/** 暂停绘图 */
	private void pauseDraw() {
		if (mDrawThread != null && !mDrawRunble.isPause()) {
			mDrawRunble.Pause();
			mDrawSPO2Rect.Pause();
		}
	}
	
	/** 停止绘图  */
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
	 * battery level
	 */
	private int batteryRes[] = { R.drawable.battery_0, R.drawable.battery_1, R.drawable.battery_2,
			R.drawable.battery_3 };

	/** 消息: 电池电量为0 , message: battery level is 0 */
	private static final int BATTERY_ZERO = 0x302;

	/**
	 * 设置电量图标
	 * set battery icon
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
	 * set pulse flag
	 */
	private void showPulse(boolean isShow) {
		if (isShow) {
			iv_Pulse.setVisibility(View.VISIBLE);
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					myHandler.sendEmptyMessage(RECEIVEMSG_PULSE_OFF);
				}
			},50);
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
	
	
//-----------------  ble operation ---------------------------
    private BluetoothAdapter   mBluetoothAdapter;
    
	private void initBLE(){
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();           
        }
		
        final BluetoothManager bluetoothManager =
        		(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "error bluetooth not supported", Toast.LENGTH_SHORT).show();           
        }else{
        	mBluetoothAdapter.enable();       	
        	mManager = new BLEManager(this, mBluetoothAdapter);       	
        }                     
	}

	public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			// Log.d(TAG, "action->"+action);
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//				Toast.makeText(MainActivity.this, "connected success", Toast.LENGTH_SHORT).show();
				tv_BlueState.setText("connected success");

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {				
				if(mManager!=null){
					mManager.closeService();
				}
//				Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();		
				tv_BlueState.setText("disconnected");
				
				if (mFingerOximeter != null)
					mFingerOximeter.Stop();
				mFingerOximeter = null;

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on
				// theuser interface.
				// showAllCharacteristic();

			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				 //Toast.makeText(MainActivity.this,intent.getStringExtra(BluetoothLeService.EXTRA_DATA),Toast.LENGTH_SHORT).show();

			} else if (BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE.equals(action)) {
				 //byte[] data =intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				 //Log.d(TAG, "MainActivity received:"+Arrays.toString(data));

			} else if (BluetoothLeService.ACTION_CHARACTER_NOTIFICATION.equals(action)) {
				Toast.makeText(MainActivity.this, "send wave request", Toast.LENGTH_SHORT).show();
				startFingerOximeter();
				
			}else if(BLEManager.ACTION_FIND_DEVICE.equals(action)){
				tv_BlueState.setText("find device, start service");
				
			}else if(BLEManager.ACTION_SEARCH_TIME_OUT.equals(action)){
				tv_BlueState.setText("search time out!");
				
			}else if(BLEManager.ACTION_START_SCAN.equals(action)){
				tv_BlueState.setText("discoverying");

			}
		}
	};

	private void startFingerOximeter(){	
		if(BLEManager.mBleHelper!=null){
			mFingerOximeter = new FingerOximeter(new ReaderBLE(BLEManager.mBleHelper), new SenderBLE(BLEManager.mBleHelper), new FingerOximeterCallBack());
			mFingerOximeter.Start();
			mFingerOximeter.SetWaveAction(true);
			//mFingerOximeter.QueryDeviceVer();
			startDraw();
		}
	}

	
//	public void showAllCharacteristic() {
//		List<BluetoothGattService> services = mBluetoothLeService.getSupportedGattServices();
//	}


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
	
	
	
//    private void scrollLog(String message) {
//        Spannable colorMessage = new SpannableString(message + "\n");
//        colorMessage.setSpan(new ForegroundColorSpan(0xff0000ff), 0, message.length(),
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        tv_spo2_hex.append(colorMessage);
//        Layout layout = tv_spo2_hex.getLayout();
//        if (layout != null) {
//            int scrollAmount = layout.getLineTop(tv_spo2_hex.getLineCount()) - tv_spo2_hex.getHeight();
//            if (scrollAmount > 0) {
//            	tv_spo2_hex.scrollTo(0, scrollAmount + tv_spo2_hex.getCompoundPaddingBottom());
//            } else {
//            	tv_spo2_hex.scrollTo(0, 0);
//            }
//        }
//    }
	
}
