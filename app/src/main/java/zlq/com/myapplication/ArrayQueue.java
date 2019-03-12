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
    //    需要增长的位置
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
        start = start == length ? 0 : start++;
        queue[start] = d;
        if (!isFull()) {
            addLength++;
        } else
            selectEnd();
    }

    /**
     * 删除数字
     */
    public void remove() {
        if (addLength - 1 != -1) {
            selectEnd();
            addLength--;
            queue[end] = 0;
        } else {
            try {
                throw new Exception("remove error");
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
    }

    private void selectEnd() {
        if (isFull()) {
//            s 0,e 1  s 1,e 2  s 2,e 3 s 3, e 0
            end = start + 1 == length ? 0 : start + 1;
        } else {
            end = start - addLength + 1 >= 0 ? start - addLength + 1 : length - (addLength - start - 1);
        }
        cursor = end;
    }

    /**
     * 查询
     * 先进后查
     */
    public Integer select() {
        start;
        end;
        addLength;
        cursor;

        if (start >= end) {
            if (cursor + 1 <= start) {
                cursor += 1;
                return queue[cursor];
            } else return null;
        } else {
            int c1 = cursor + 1;

            if (c1 == length) {

            }else{
                if (addLength)
            }
        }
    }

}
