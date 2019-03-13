package zlq.com.myapplication;

import android.util.Log;

/**
 * author: ZlqPC
 * created on: 2019/3/11 22:07
 * description:  一个定长的队列，该队列默认从0位置开始增长，默认游标为-1。当队列增长至n，则游标为n。
 * 队列增加至满为(length)，start = length-1;再增加一个数字时，则把最开始增加的数字替换成最新增加的数字。
 * 遵循先进先出规则
 */
public final class ArrayQueue {

    private static final String TAG = "ArrayQueue";
    //    初始化长度
    private final int length;
    //    队列
    private final int[] queue;
    //    自增长长度
    private int addLength = 0;
    //    需要增长的位置（注意：这里的start是要增长的位置，也就是开始位置应该是减1）
    private int start = 0;
    //    初始游标
    private int cursor = -1;
    // 最先数字位置
    private int end = 0;

    public ArrayQueue(int length) {
        if (length == 0) {
            try {
                throw new Exception("length!=0");
            } catch (Exception e) {
                Log.e(TAG, "ArrayQueue initialize error", e);
            }
        }
        this.length = length;
        queue = new int[length];
    }

    public boolean isFull() {
        return addLength == length;
    }

    /**
     * 增加数字
     */
    public void pop(int d) {
        queue[start++] = d;
        start = start % length;
        if (!isFull()) {
            addLength++;
        } else {
            selectEnd();
        }
    }

    /**
     * 删除数字
     */
    public void remove() {
        int s = start - 1 + length * ((start - 1) < 0 ? 1 : 0);
        if (addLength == 0) {
            try {
                throw new Exception("remove error");
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        if (end == s) {
            queue[end] = 0;
        }
        queue[end++] = 0;
        if (end == length) {
            end = 0;
        }
        addLength--;
        cursor = end;
    }

    private void selectEnd() {
        end = start - addLength;
        if (end < 0) {
            end = end + length;
        }
    }


    /**
     * 查询
     * 先进先查
     * end
     */
    public Integer select() {
        if (addLength == 0) {
            return null;
        }
        int s = start - 1 + length * ((start - 1) < 0 ? 1 : 0);
        if (cursor == -1) {
            cursor = end;
        }
        if (cursor == s) {
            int c = cursor;
            cursor = end;
            if (cursor - end < addLength) {
                return queue[c];
            }
        }
        if (cursor == length) {
            cursor = 0;
        }
        return queue[cursor++];
    }

    /**
     * 返回初始化长度
     */
    public int initLength() {
        return length;
    }

    /**
     * 返回增加的长度
     */
    public int getAddLength() {
        return addLength;
    }

}
