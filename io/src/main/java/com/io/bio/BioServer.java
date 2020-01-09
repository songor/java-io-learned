package com.io.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接收到这 20 个连接，创建 20 个 Socket，接着创建 20 个线程，每个线程负责一个 Socket
 */
public class BioServer {

    private static AtomicInteger threadCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        try {
            // 监听端口
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("localhost", 8080));
            while (true) {
                // 接收请求，建立连接
                Socket socket = serverSocket.accept();
                // 数据交换
                processWithNewThread(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processWithNewThread(Socket socket) {
        Runnable r = () -> {
            try {
                InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                System.out.println(currentTime() + " -> " + socketAddress.getHostName() + " : " + socketAddress.getPort() + " -> " + Thread.currentThread().getId() + " : " + threadCounter.incrementAndGet());
                String result = readBytes(socket.getInputStream());
                System.out.println(currentTime() + " -> " + result + " -> " + Thread.currentThread().getId() + " : " + threadCounter.getAndDecrement());
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        new Thread(r).start();
    }

    private static String readBytes(InputStream inputStream) throws IOException {
        byte[] bytes = new byte[1024];
        int total = 0;
        int length;
        long start = 0;
        long begin = Instant.now().toEpochMilli();
        while ((length = inputStream.read(bytes)) != -1) {
            if (start < 1) {
                start = Instant.now().toEpochMilli();
            }
            total += length;
        }
        long end = Instant.now().toEpochMilli();
        return "Wait: " + (start - begin) + "ms, Read: " + (end - start) + "ms, Total: " + total + "bs";
    }

    private static String currentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;
        return formatter.format(LocalTime.now());
    }

}
