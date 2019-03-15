/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * This file is provide by Google. I do modify some code to adopt This demo Project.
 *
 */
package com.ble.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {
//    private final static String TAG = BluetoothLeService.class.getSimpleName();
	private static final String TAG = "MainActivity";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    
    public static int mConnectionState = STATE_DISCONNECTED;
    
    // BLE action
    public final static String ACTION_GATT_CONNECTED ="com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED ="com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED ="com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE ="com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA ="com.example.bluetooth.le.EXTRA_DATA";    
    
    // business action
    public final static String ACTION_CHARACTER_NOTIFICATION="com.example.bluetooth.le.notification.success";  
    public final static String ACTION_SPO2_DATA_AVAILABLE ="com.example.bluetooth.le.ACTION_SPO2_DATA_AVAILABLE";
    
    //-----UUID ---
	/** 血氧 SpO2 service-> uuid */
    public static final UUID  UUID_SERVICE_DATA            = UUID.fromString("0000FFB0-0000-1000-8000-00805F9B34FB");
    /** 血氧 SpO2 character->write uuid */  
    public static UUID  UUID_CHARACTER_WRITE         = UUID.fromString("0000FFB2-0000-1000-8000-00805F9B34FB");
    /** 血氧 SpO2 character->read uuid */
    public static final UUID  UUID_CHARACTER_READ		   = UUID.fromString("0000FFB1-0000-1000-8000-00805F9B34FB");
   
    public static final UUID UUID_CLIENT_CHARACTER_CONFIG  = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
         
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
            
            //设置监听,获取onCharacteristic()的通知回调
            Log.i(TAG, "set linstening of notificaiton callback(Function onCharacteristicChanged() callback) ,in writting characteristic ");
            BluetoothGattCharacteristic characteristic = getGattCharacteristic(UUID_CHARACTER_READ);
            setCharacteristicNotification(characteristic, true);
         
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,int status) {
        	Log.d(TAG, "onCharacteristicRead " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	//broadcastUpdate(ACTION_SPO2_DATA_AVAILABLE, characteristic);        	
        	
        	// Log.d(TAG, "service receive data："+Arrays.toString(characteristic.getValue()));//10进制
//			synchronized (this) { // 16进制
//				byte[] datas = characteristic.getValue();
//				String str = "";
//				for (int i = 0; i < datas.length; i++) {
//					str += String.format("%02x", datas[i]) + " ";
//				}
//				Log.d(TAG, "收到数据:" + str);
//			}
        	   
        	mBuffer.add(characteristic.getValue());

        }
        
        /** 
         * 当写characteristics得到结果时回调 ,命令是否发送成功
         * whether or not Characteristic Write success
         */  
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	//Log.d(TAG, "onCharacteristicWrite " + status);  
            if (status == BluetoothGatt.GATT_SUCCESS){ 
//            	try {  
//                     byte[] mByte = characteristic.getValue();  
//                     StringBuilder strBuilder = new StringBuilder(mByte.length);                                    
//                     for (byte mByteChar : mByte) {  
//                         strBuilder.append(String.format("%02x", mByteChar));                     
//                     }  
//                     Log.d(TAG, strBuilder.toString());
//                 } catch (Exception e) {                     
//                     e.printStackTrace();  
//                 } 
            	
            	byte[] mByte = characteristic.getValue();
            	if(mByte!=null && mByte.length>0){
            		if((mByte[4]&0xff) == 0x84){
            			Log.i(TAG, "enable param of request-> send success");
            		}else if((mByte[4]&0xff) == 0x85){
            			Log.d(TAG, "enable [ wave ] of request-> send success");
            		}
            	}
            }
        };      
        
        //listen for whether or not notification success
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//        	if (status == BluetoothGatt.GATT_SUCCESS){
//				Log.d(TAG, descriptor.getCharacteristic().getUuid()+ " Notification Enabled");
//        	}
        	
        	if (UUID_CHARACTER_READ.equals(descriptor.getCharacteristic().getUuid())){
        		Log.d(TAG, "CHARACTER_READ-> Notification Enabled");
                try {
     				Thread.sleep(3500); //等待终端设备初始化,wait for Oximeter terminal init
     			} catch (InterruptedException e) {
     				// TODO Auto-generated catch block
     				e.printStackTrace();
     			}
                Log.d(TAG, "-- wake up --"); 
        		BluetoothGattCharacteristic chara = getGattCharacteristic(UUID_CHARACTER_WRITE);
                setCharacteristicNotification(chara, true);
                
        	}else if(UUID_CHARACTER_WRITE.equals(descriptor.getCharacteristic().getUuid())){
        		Log.d(TAG, "CHARACTER_WRITE-> Notification Enabled"); 	       		  		      		      		
        		broadcastUpdate(ACTION_CHARACTER_NOTIFICATION);
        	}              	       	
        };       
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private final static int TRANSFER_PACKAGE_SIZE = 10;
    private byte[] buf = new byte[20];
    
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);   
        if (UUID_CHARACTER_WRITE.equals(characteristic.getUuid())) {
        	int bufIndex = 0;
        	final byte[] data = characteristic.getValue();           
                    
            for(byte b : data)
            {
                buf[bufIndex] = b;  
                bufIndex++;
                if(bufIndex == buf.length)
                {
                    intent.putExtra(EXTRA_DATA,buf);
                    sendBroadcast(intent);
                }
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                //final StringBuilder stringBuilder = new StringBuilder(data.length);
                intent.putExtra(EXTRA_DATA, new String(data));
                sendBroadcast(intent);
            }
        }

    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized->1");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized->2");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }       

    /** 
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
//    	final int charaProp = characteristic.getProperties();
//    	if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0){}
    	
    	if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized->3");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        
//        for(BluetoothGattDescriptor des:characteristic.getDescriptors()){      	
//        	Log.d(TAG, "descriptor->"+des.getUuid());
//        	des.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//        	mBluetoothGatt.writeDescriptor(des);
//        }
               
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTER_CONFIG);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);
    }

    
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

	public BluetoothGattCharacteristic getGattCharacteristic(UUID characterUUID){	
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "BluetoothAdapter not initialized->4");
			return null;
		} else if (mBluetoothGatt == null) {
			Log.e(TAG, "BluetoothGatt not initialized->5");
			return null;
		}

		BluetoothGattService service = mBluetoothGatt.getService(UUID_SERVICE_DATA);
		if (service == null) {
			Log.e(TAG, "Service is not found!");
			return null;
		}
		BluetoothGattCharacteristic chara = service.getCharacteristic(characterUUID);
		return chara;
		
	}
	
    /**
     * Split the package into small pieces to transfer.
     * @param ch
     * @param bytes
     */
    public void write(BluetoothGattCharacteristic ch, byte[] bytes)
    {
        int byteOffset = 0;
        while(bytes.length - byteOffset > TRANSFER_PACKAGE_SIZE)
        {
            byte[] b = new byte[TRANSFER_PACKAGE_SIZE];
            System.arraycopy(bytes,byteOffset,b,0, TRANSFER_PACKAGE_SIZE);

            ch.setValue(b);
            mBluetoothGatt.writeCharacteristic(ch);

            byteOffset += TRANSFER_PACKAGE_SIZE;
        }

        if(bytes.length - byteOffset != 0)
        {
            byte[] b = new byte[bytes.length - byteOffset];
            System.arraycopy(bytes,byteOffset,b,0,bytes.length - byteOffset);

            ch.setValue(b);
            mBluetoothGatt.writeCharacteristic(ch);
        }
    }  
    

  /**
  * push data to analyse protocal of buffer 
  * @param dataBuffer  :analyse buffer
  * @return
  */
    private LinkedBlockingQueue<byte[]> mBuffer = new LinkedBlockingQueue<byte[]>();
    public int read(byte[] dataBuffer){
    	if(mBuffer.size()>0){
			byte[] temp = mBuffer.poll(); 
			if(temp!=null && temp.length>0){
				int len = (temp.length < dataBuffer.length)? temp.length:dataBuffer.length;    	
				for(int j=0;j<len;j++){
					dataBuffer[j]=temp[j];
				}
				return len;
			}
    	}
    	return 0;  	
    }   
    
	public void clean() {
		if(mBuffer!=null){
			mBuffer.clear();
		}
	}

	public int available() throws IOException {
		if(mBuffer!=null){
			return mBuffer.size();
		}
		return 0;
	}
	   
	
}
