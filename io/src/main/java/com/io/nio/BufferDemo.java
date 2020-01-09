package com.io.nio;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class BufferDemo {

    public static void main(String[] args) {
        // 创建一个 20 个 byte 的数组缓冲区，position 的位置为 0，capacity 和 limit 默认都是数组长度
        ByteBuffer buffer = ByteBuffer.allocate(20);
        status(buffer, "allocate()");
        byte[] bytes = new byte[5];
        IntStream.range(0, 5).forEach(i -> {
            bytes[i] = 0;
        });
        // 写入 5 个字节，position 的位置为 5
        buffer.put(bytes);
        status(buffer, "put()");
        // 反转缓冲区，position 的位置为 0，limit 的位置为 5
        buffer.flip();
        status(buffer, "flip()");
        // 读取 1 个字节
        buffer.get();
        status(buffer, "get()");
        // 记录当前 position 的前一个位置
        buffer.mark();
        // position 将恢复 mark 记录下来的值
        buffer.reset();
        status(buffer, "reset()");
        // 将 position 置为 0
        buffer.rewind();
        status(buffer, "rewind()");
        // 缓冲区的索引状态又回到初始位置
        buffer.clear();
        status(buffer, "clear()");
    }

    private static void status(Buffer buffer, String prefix) {
        int capacity = buffer.capacity();
        int position = buffer.position();
        int limit = buffer.limit();
        System.out.println(prefix + " -> capacity: " + capacity + ", position: " + position + ", limit: " + limit);
    }

}
