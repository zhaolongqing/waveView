package org.join.chat.core;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class NioClient extends NioCommunication implements Runnable {

    private static final String TAG = "NioClient";
    private boolean isPrepared = false;
    private Selector selector;
    private SelectionKey clientKey;
    private InetSocketAddress address;

    /**
     * 客户端构造函数
     *
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public NioClient(String host, int port) {
        address = new InetSocketAddress(host, port);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            notifyStateChanged(ERROR, e);
            Log.e(TAG, "NioClient", e);
        }
    }


    public void start() {
        new Thread(this).start();
    }

    public void send(String msg) {
        notifyStateChanged(MSG_SEND, msg);
        if (null == clientKey) {
            return;
        }
        clientKey.attach(msg);
        clientKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        clientKey.selector().wakeup();
    }

    @Override
    public void run() {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(address);
            clientKey = sc.register(selector, SelectionKey.OP_CONNECT);
            isPrepared = true;
            while (isPrepared) {
                int keysCount = selector.select();
                if (keysCount < 1) {
                    continue;
                }
                Set<SelectionKey> set = selector.selectedKeys();
                for (SelectionKey key : set) {
                    if (key.isConnectable()) {
                        doConnect(key);
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
            notifyStateChanged(ERROR, e);
            e.printStackTrace();
        } finally {
            // close();
            notifyStateChanged(CLT_DISCONNECT, null);
        }
    }


    private void doConnect(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            // http://www.velocityreviews.com/forums/t145075-whats-the-proper-way-to-use-socketchannel-finishconnect.html
            sc.finishConnect();
            key.interestOps(SelectionKey.OP_READ);
            notifyStateChanged(CLT_CONNECT, null);
        } catch (IOException e) {
            disconnect(key);
            e.printStackTrace();
        }
    }

    private void doRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
        StringBuilder sb = new StringBuilder();
        try {
            int count = 0;
            while ((count = sc.read(bb)) > 0) {
                bb.flip();
                sb.append(decoder.decode(bb));
            }
            if (count == -1) {
                disconnect(key);
            } else {
                notifyStateChanged(MSG_RECEIVE, sb.toString().trim());
            }
        } catch (IOException e) {
            disconnect(key);
            e.printStackTrace();
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
            disconnect(key);
            e.printStackTrace();
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    public void close() {
        isPrepared = false;
        try {
            if (null != clientKey) {
                clientKey.channel().close();
            }
            if (null != selector) {
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开连接
     */
    private void disconnect(SelectionKey key) {
        notifyStateChanged(CLT_DISCONNECT, null);
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
