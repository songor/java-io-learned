package com.io.aio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AsyncDemo {

    public static void main(String[] args) {
        // 将来式
        try {
            Path file = Paths.get("async.txt");
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(file);
            ByteBuffer buffer = ByteBuffer.allocate(100_000);
            Future<Integer> result = channel.read(buffer, 0);

            // Other things

            Integer count = result.get();
            System.out.println("Bytes read [" + count + "]");
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // 回调式
        try {
            Path file = Paths.get("async.txt");
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(file);
            ByteBuffer buffer = ByteBuffer.allocate(100_000);
            channel.read(buffer, 0, null, new CompletionHandler<Integer, ByteBuffer>() {

                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    System.out.println("Bytes read [" + result + "]");
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    System.out.println(exc.getMessage());
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
