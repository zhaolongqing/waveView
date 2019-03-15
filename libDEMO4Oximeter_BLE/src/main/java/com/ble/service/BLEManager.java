package com.ble.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class BLEManager {	
	private static final String TAG = "BLEManager";

    private BluetoothAdapter   mBluetoothAdapter;
    private BluetoothDevice    mTargetDevice;
    private BluetoothLeService mBluetoothLeService;
    public static BLEHelper mBleHelper;  
    private Context mContext;
    private boolean bScanning;
    private Handler mHandler;
    private boolean bBindServ;
    
    private static final long SCAN_PERIOD = 15000;
    public static final String ACTION_FIND_DEVICE = "find_device";
    public static final String ACTION_SEARCH_TIME_OUT = "search_timeout";
    public static final String ACTION_START_SCAN = "start_scan";
    
	public BLEManager(Context context,BluetoothAdapter adapter) {
		mContext = context;
		mHandler = new Handler();
		mBluetoothAdapter = adapter;
	}
	
	public void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					if(bScanning){
					   //tv_BlueState.setText("search time out!");
						broadcastUpdate(ACTION_SEARCH_TIME_OUT);
						Log.d(TAG, "search time out!");
					}							
				}
			}, SCAN_PERIOD);
			
			bScanning = true;
			broadcastUpdate(ACTION_START_SCAN);
			mBluetoothAdapter.startLeScan(mLeScanCallback);			
		} else {
			bScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {					
			String devName = device.getName();
			if (devName!=null &&
				("PC-60NW-1".equalsIgnoreCase(devName)||
				"POD".equalsIgnoreCase(devName)||
				devName.contains("PC-68B")||
				"Wrist-100".equalsIgnoreCase(devName))) {
				mTargetDevice = device;
				scanLeDevice(false);				
				Log.d(TAG, "find-->"+mTargetDevice.getName());
				broadcastUpdate(ACTION_FIND_DEVICE);
				
				// start BluetoothLeService
				synchronized (this) {
					bBindServ = true;
					Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
					mContext.bindService(gattServiceIntent, mServiceConnection, mContext.BIND_AUTO_CREATE);
				}
			}
		}
	};
	
	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");		
				//Toast.makeText(mContext, "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
				return;
			}
			
			mBleHelper = new BLEHelper(mBluetoothLeService);
			
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mTargetDevice.getAddress());
			
//			new Thread(new Runnable() {
//				public void run() {
//					try {
//						Thread.sleep(3000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					mBluetoothLeService.connect(mTargetDevice.getAddress());
//				}
//			}).start();
			
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
			mBleHelper = null;
		}
	};
	
	
	public void closeService(){
		synchronized (this) {
			if(bBindServ){
				mContext.unbindService(mServiceConnection);
				bBindServ = false;
			}
			if(mBluetoothLeService!=null){
				mBluetoothLeService.close();
				mBluetoothLeService = null;
			}	
			Log.d(TAG, "-- closeService --");	
		}
	}
	
	/**
	 * 断开连接
	 */
	public void disconnect() {
		if (mBluetoothLeService != null) {
			mBluetoothLeService.disconnect();
		}
	}
		
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }
	
	/**
	 * 自定义过滤器
	 * custom intentFilter
	 */
	public static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		//---
		intentFilter.addAction(BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_CHARACTER_NOTIFICATION);
		intentFilter.addAction(ACTION_FIND_DEVICE);
		intentFilter.addAction(ACTION_SEARCH_TIME_OUT);
		intentFilter.addAction(ACTION_START_SCAN);
		return intentFilter;
	}

}
