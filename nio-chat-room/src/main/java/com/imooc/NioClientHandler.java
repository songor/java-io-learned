package com.imooc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NioClientHandler implements Runnable {

    private Selector selector;

    public NioClientHandler(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        /**
         *循环调用 Selector 的 select 方法，检测就绪情况
         */
        for (; ; ) {
            int readyChannels = 0;
            try {
                readyChannels = selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (readyChannels == 0) continue;

            /**
             * 调用 selectedKeys 方法获取就绪 Channel 集合
             */
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                /**
                 * 判断就绪事件种类，调用业务处理方法
                 */
                if (selectionKey.isReadable()) {
                    try {
                        readHandler(selector, selectionKey);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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

        StringBuilder response = new StringBuilder();
        /**
         * 循环读取服务端响应信息
         */
        while (socketChannel.read(buffer) > 0) {
            buffer.flip();
            response.append(Charset.forName("UTF-8").decode(buffer));
        }

        /**
         * 将 Channel 再次注册到 Selector 上
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 打印
         */
        System.out.println(response.toString());
    }

}
