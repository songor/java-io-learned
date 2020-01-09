package com.imooc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class NioClient {

    public static void start(String name) throws IOException {
        /**
         * 连接服务器端
         */
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8888));

        /**
         * 接收响应
         */
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        new Thread(new NioClientHandler(selector)).start();

        /**
         * 向服务器端发送数据
         */
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String str = scanner.nextLine();
            socketChannel.write(Charset.forName("UTF-8").encode(name + " : " + str));
        }
    }

}
