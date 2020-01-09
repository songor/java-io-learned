package com.io.nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 通过 InputStream，OutputStream，RandomAccessFile 获取 FileChannel，FileChannel 只能是阻塞模式，
 * 更准确的来说是因为 FileChannel 没有继承 SelectableChannel。
 */
public class FileChannelDemo {

    public static void main(String[] args) {
        write();
        read();
    }

    private static void read() {
        try {
            String file = "demo.txt";
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(48);
            int count = channel.read(buffer);
            while (count != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    System.out.print((char) buffer.get());
                }
                buffer.clear();
                count = channel.read(buffer);
            }
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write() {
        try {
            String file = "demo.txt";
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(48);
            buffer.put("It's ok".getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.clear();
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
