package com.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调用 Selector 的静态工厂创建一个选择器，创建一个服务器的 Channel，绑定到一个 socket 对象，并把这个通信信道注册到选择器上，
 * 把这个通信信道设置为非阻塞模式。然后就可以调用 Selector 的 selectedKeys 方法来检查已经注册在这个选择器上的所有通信信道是否有需要的事件发生，
 * 如果有某个事件发生，将会返回所有的 SelectionKey，通过这个对象的 Channel 方法就可以取得这个通信信道对象，从而读取通信的数据，
 * 而这里读取的数据是 Buffer，这个 Buffer 是我们可以控制的缓冲器。
 */

/**
 * 多路指的是多个 Socket 通道，复用指的是只用一个线程来管理它们
 */
public class NioServer {

    private static int clientCounter = 0;

    private static AtomicInteger threadCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        try {
            // “跑腿”服务员
            Selector selector = Selector.open();
            // 大堂经理
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            // ssc.bind(new InetSocketAddress("localhost", 8080));
            ssc.socket().bind(new InetSocketAddress("localhost", 8080));
            // 大堂经理委托给“跑腿”服务员，你帮我盯着，有人来了告诉我
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            // 轮询
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {// 终于来客人了，“跑腿”服务员赶紧告诉“大堂经理”
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        // “大堂经理”把客人带到座位上
                        SocketChannel sc = channel.accept();
                        sc.configureBlocking(false);
                        // 客人接下来肯定是要点餐的，但是现在在看菜单，不知道什么时候能看好，所以你不时地过来问问，看需不需要点餐
                        sc.register(selector, SelectionKey.OP_READ);
                        InetSocketAddress socketAddress = (InetSocketAddress) sc.socket().getRemoteSocketAddress();
                        System.out.println(currentTime() + " -> " + socketAddress.getHostName() + " : " + socketAddress.getPort() + " -> " + Thread.currentThread().getId() + " : " + (++clientCounter));
                    } else if (key.isReadable()) {// 客人终于决定点餐了，“跑腿”服务员赶紧找来一个“点餐”服务员为客人写菜单
                        key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                        processWithNewThread((SocketChannel) key.channel(), key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processWithNewThread(SocketChannel sc, SelectionKey key) {
        Runnable r = () -> {
            threadCounter.incrementAndGet();
            try {
                String result = readBytes(sc);
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                System.out.println(currentTime() + " -> " + result + " -> " + Thread.currentThread().getId() + " : " + threadCounter.get());
                sc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            threadCounter.decrementAndGet();
        };
        new Thread(r).start();
    }

    private static String readBytes(SocketChannel sc) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int total = 0;
        int length;
        long start = 0;
        long begin = Instant.now().toEpochMilli();
        while ((length = sc.read(buffer)) != -1) {
            if (start < 1) {
                start = Instant.now().toEpochMilli();
            }
            total += length;
            buffer.clear();
        }
        long end = Instant.now().toEpochMilli();
        return "Wait: " + (start - begin) + "ms, Read: " + (end - start) + "ms, Total: " + total + "bs";
    }

    private static String currentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;
        return formatter.format(LocalTime.now());
    }

}
