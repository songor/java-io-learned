package com.io.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 创建 20 个 Socket 并连接到服务器，再创建 20 个线程，每个线程负责一个 Socket
 */
public class Client {

    public static void main(String[] args) {
        IntStream.range(0, 20).forEach(i -> {
            try {
                // 建立连接
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", 8080));
                processWithNewThread(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void processWithNewThread(Socket socket) {
        Runnable r = () -> {
            try {
                Thread.sleep((new Random().nextInt(6) + 5) * 1000);
                // 获取输出流
                socket.getOutputStream().write(prepareBytes());
                Thread.sleep(1000);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(r).start();
    }

    private static byte[] prepareBytes() {
        byte[] bytes = new byte[1024 * 1024];
        IntStream.range(0, bytes.length).forEach(i -> {
            bytes[i] = 0;
        });
        return bytes;
    }

}
