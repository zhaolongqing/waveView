package org.join.chat.core;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Set;

public class NioServer extends NioCommunication implements Runnable {


    private static final String TAG = "NioServer";
    private boolean isPrepared = false;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ArrayList<SelectionKey> serverKeyList;
    private String receiveMessage;


    /**
     * 服务器构造函数
     *
     * @param port 端口
     */
    public NioServer(int port) {
        try {
            selector = Selector.open();
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            serverKeyList = new ArrayList<>();
            isPrepared = true;
        } catch (IOException e) {
            notifyStateChanged(ERROR, e);
            Log.e(TAG, "NioServer", e);
        }
    }


    public void start() {
        if (isPrepared)
            new Thread(this).start();
    }



    @Override
    public void run() {
        notifyStateChanged(SEV_ON, null);
        try {
            while (isPrepared) {
                int keysCount = selector.select();
                if (keysCount < 1) {
                    continue;
                }
                Set<SelectionKey> set = selector.selectedKeys();
                for (SelectionKey key : set) {
                    if (key.isAcceptable()) {
                        doAccept(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        doRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        doWrite(key);
                    }
                }
                set.clear();
            }
        } catch (IOException e) {
            Log.e(TAG, "run", e);
        } finally {
            // close();
            notifyStateChanged(SEV_OFF, null);
        }
    }

    private void doWrite(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        String msg = (String) key.attachment();
        if (null == msg) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        try {
            sc.write(encoder.encode(CharBuffer.wrap(msg)));
        } catch (IOException e) {
            disconnect(key, sc);
            // e.printStackTrace();
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void doRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
        StringBuilder sb = new StringBuilder();
        try {
            int count;
            while ((count = sc.read(bb)) > 0) {
                bb.flip();
                sb.append(decoder.decode(bb));
            }
            if (count == -1) {
                disconnect(key, sc);
            } else {
                receiveMessage = sb.toString().trim();
                notifyStateChanged(MSG_RECEIVE, sc.socket().getRemoteSocketAddress());
            }
        } catch (IOException e) {
            disconnect(key, sc);
            Log.e(TAG, "doRead", e);
            // e.printStackTrace();
        }
    }

    private void doAccept(SelectionKey key) {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        try {
            SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
            // newKey.attach(new ArrayList<String>());
            serverKeyList.add(newKey);
            notifyStateChanged(CLT_CONNECT, sc.socket().getRemoteSocketAddress());
        } catch (IOException e) {
            Log.e(TAG, "doAccept", e);
        }
    }

    /**
     * 关闭服务
     */
    public void close() {
        isPrepared = false;
        try {
            if (null != serverKeyList) {
                for (SelectionKey key : serverKeyList) {
                    key.channel().close();
                }
            }
            if (null != selector) {
                selector.close();
            }
            if (null != ssc) {
                ssc.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 断开连接 */
    private void disconnect(SelectionKey key, SocketChannel sc) {
        serverKeyList.remove(key);
        notifyStateChanged(CLT_DISCONNECT, sc.socket().getRemoteSocketAddress());
        try {
            key.cancel();
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
