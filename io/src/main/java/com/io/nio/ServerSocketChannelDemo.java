package com.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * ServerSocketChannel 允许我们监听 TCP 连接请求，通过 accept() 方法可以创建一个 SocketChannel 对象从客户端读、写数据。
 */
public class ServerSocketChannelDemo {

    public static void main(String[] args) {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress("localhost", 8080));
            SocketChannel sc = ssc.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuilder sb = new StringBuilder();
            int count = sc.read(buffer);
            while (count != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    sb.append((char) buffer.get());
                }
                buffer.clear();
                count = sc.read(buffer);
            }
            System.out.println(sb.toString());
            sc.close();
            ssc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
