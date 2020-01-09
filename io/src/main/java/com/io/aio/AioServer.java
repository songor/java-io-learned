package com.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class AioServer {

    private static int clientCounter = 0;

    private static AtomicInteger threadCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        try {
            // 初始化一个 AsynchronousServerSocketChannel
            AsynchronousServerSocketChannel assc = AsynchronousServerSocketChannel.open();
            // 开始监听
            assc.bind(new InetSocketAddress("localhost", 8080));
            // 通过 accept 方法注册一个“完成处理器”的回调，即 CompletionHandler，用于在接收到连接后的相关操作
            assc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

                // 当客户端连接过来后，由系统来接收，并创建好 AsynchronousSocketChannel 对象，然后触发该回调，
                // 并把 AsynchronousSocketChannel 传送进该回调，该回调会在 Worker 线程中执行
                @Override
                public void completed(AsynchronousSocketChannel asc, Object attachment) {
                    // 再次使用 accept 方法注册一次相同的“完成处理器”回调，用于让系统接收下一个连接
                    assc.accept(null, this);
                    try {
                        InetSocketAddress socketAddress = (InetSocketAddress) asc.getRemoteAddress();
                        System.out.println(currentTime() + " -> " + socketAddress.getHostName() + " : " + socketAddress.getPort()
                                + " -> " + Thread.currentThread().getId() + " : " + (++clientCounter));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    readFromChannelAsync(asc);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                }

            });
            synchronized (AioServer.class) {
                AioServer.class.wait();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void readFromChannelAsync(AsynchronousSocketChannel asc) {
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 + 1);
        long begin = Instant.now().toEpochMilli();
        // 使用 AsynchronousSocketChannel 对象的 read 方法注册另一个接收数据回调，用于在接收到数据后的相关操作
        asc.read(buffer, null, new CompletionHandler<Integer, Object>() {

            int total = 0;

            // 当客户端数据过来后，由系统接收，并放入指定好的 ByteBuffer 中，然后触发该回调，并把本次接收到的数据字节数传入该回调，
            // 该回调会在 Worker 线程中执行
            @Override
            public void completed(Integer count, Object attachment) {
                threadCounter.incrementAndGet();
                if (count > -1) {
                    total += count;
                }
                int size = buffer.position();
                System.out.println(currentTime() + " -> count=" + count + ", total=" + total + "bs, buffer=" + size + "bs -> "
                        + Thread.currentThread().getId() + " : " + threadCounter.get());
                // 如果没有接收完，需要再次使用 read 方法把同一对象注册一次，用于让系统接收下一次数据
                // 客户端的数据可能是分多次传到服务器端的，所以接收数据回调会被执行多次，直到数据接收完成为止。
                // 多次接收到的数据合起来才是完整的数据
                if (count > -1) {
                    // 关于 ByteBuffer，要么足够大，能够装得下完整的客户端数据，这样多次接收的数据直接往里追加即可。
                    // 要么每次把 ByteBuffer 中的数据移到别的地方存储起来，然后清空 ByteBuffer，用于让系统往里装入下一次接收的数据
                    asc.read(buffer, null, this);
                } else {
                    try {
                        asc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                threadCounter.decrementAndGet();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
            }

        });
        long end = Instant.now().toEpochMilli();
        System.out.println(currentTime() + " -> execute read request, use " + (end - begin) + "ms  -> " + Thread.currentThread().getId());
    }

    private static String currentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;
        return formatter.format(LocalTime.now());
    }

}