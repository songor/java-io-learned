package com.imooc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    private static void start() throws IOException {
        /**
         * 创建 Selector
         */
        Selector selector = Selector.open();

        /**
         * 创建 ServerSocketChannel，并绑定监听端口
         */
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8888));

        /**
         * 将 Channel 设置为非阻塞模式
         */
        serverSocketChannel.configureBlocking(false);

        /**
         * 将 Channel 注册到 Selector 上，监听连接事件
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server Listening...");

        /**
         * 循环调用 Selector 的 select 方法，检测就绪情况
         */
        for (; ; ) {
            int readyChannels = selector.select();
            if (readyChannels == 0) continue;

            /**
             * 调用 selectedKeys 方法获取 就绪 Channel 集合
             */
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                /**
                 * 判断就绪事件种类，调用业务处理方法
                 */
                if (selectionKey.isAcceptable()) {
                    acceptHandler(selector, serverSocketChannel);
                }
                if (selectionKey.isReadable()) {
                    readHandler(selector, selectionKey);
                }
            }
        }
    }

    private static void acceptHandler(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        /**
         * 创建 SocketChannel
         */
        SocketChannel socketChannel = serverSocketChannel.accept();

        /**
         * 将 SocketChannel 设置为非阻塞工作模式
         */
        socketChannel.configureBlocking(false);

        /**
         * 将 Channel 注册到 Selector 上，监听 可读 事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 回复客户端
         */
        socketChannel.write(Charset.forName("UTF-8").encode("Connect"));
    }

    private static void readHandler(Selector selector, SelectionKey selectionKey) throws IOException {
        /**
         * 获取到已经就绪的 Channel
         */
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        /**
         * 创建 Buffer
         */
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        StringBuilder request = new StringBuilder();
        /**
         * 循环读取客户端请求信息
         */
        while (socketChannel.read(buffer) > 0) {
            buffer.flip();
            request.append(Charset.forName("UTF-8").decode(buffer));
        }

        /**
         * 将 Channel 再次注册到 Selector 上
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 广播
         */
        String str = request.toString();
        boardCast(selector, socketChannel, str);
    }

    private static void boardCast(Selector selector, SocketChannel sourceChannel, String str) throws IOException {
        /**
         * 获取所有的 注册 客户端 Channel
         */
        Set<SelectionKey> selectionKeys = selector.keys();
        selectionKeys.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();

            /**
             * 排除发消息 Channel
             */
            if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel) {

                /**
                 * 广播
                 */
                try {
                    ((SocketChannel) targetChannel).write(Charset.forName("UTF-8").encode(str));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws IOException {
        new NioServer().start();
    }

}
