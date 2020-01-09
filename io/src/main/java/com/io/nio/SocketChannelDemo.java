package com.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * SocketChannel 用于创建基于 TCP 协议的客户端对象，通过 connect() 方法，SocketChannel 对象可以连接到其他 TCP 服务器程序。
 */
public class SocketChannelDemo {

    public static void main(String[] args) {
        try {
            SocketChannel channel = SocketChannel.open();
            channel.connect(new InetSocketAddress("localhost", 8080));
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put("It's comes from client.".getBytes());
            buffer.flip();
            channel.write(buffer);
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
