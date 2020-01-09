package com.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.EnumSet;
import java.util.stream.IntStream;

public class CopyFile {

    public static void main(String[] args) throws IOException {
        Path source = createFile();
        Path target = Paths.get("big_copy.file");
        Files.deleteIfExists(target);

        fileChannelWithNonDirectBuffer(source, target);

        fileChannelWithDirectBuffer(source, target);

        fileChannelTransferTo(source, target);

        fileChannelTransferFrom(source, target);

        fileChannelMap(source, target);

        bufferedStream(source.toFile(), target.toFile());

        inputStream(source.toFile(), target.toFile());

        filesCopy(source, target);
    }

    private static void fileChannelWithNonDirectBuffer(Path source, Path target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using FileChannel and non-direct buffer");
        try (FileChannel sou = FileChannel.open(source, EnumSet.of(StandardOpenOption.READ));
             FileChannel tar = FileChannel.open(target, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            int count;
            while ((count = sou.read(buffer)) > 0) {
                buffer.flip();
                tar.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileChannelWithDirectBuffer(Path source, Path target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using FileChannel and direct buffer");
        try (FileChannel sou = FileChannel.open(source, EnumSet.of(StandardOpenOption.READ));
             FileChannel tar = FileChannel.open(target, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
            int count;
            while ((count = sou.read(buffer)) > 0) {
                buffer.flip();
                tar.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileChannelTransferTo(Path source, Path target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using FileChannel.transferTo()");
        try (FileChannel sou = FileChannel.open(source, EnumSet.of(StandardOpenOption.READ));
             FileChannel tar = FileChannel.open(target, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            sou.transferTo(0L, sou.size(), tar);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileChannelTransferFrom(Path source, Path target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using FileChannel.transferFrom()");
        try (FileChannel sou = FileChannel.open(source, EnumSet.of(StandardOpenOption.READ));
             FileChannel tar = FileChannel.open(target, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            tar.transferFrom(sou, 0L, sou.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileChannelMap(Path source, Path target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using FileChannel.map()");
        try (FileChannel sou = FileChannel.open(source, EnumSet.of(StandardOpenOption.READ));
             FileChannel tar = FileChannel.open(target, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            MappedByteBuffer buffer = sou.map(FileChannel.MapMode.READ_ONLY, 0, sou.size());
            tar.write(buffer);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void bufferedStream(File source, File target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using buffered stream");
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void inputStream(File source, File target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using input stream");
        try (FileInputStream is = new FileInputStream(source);
             FileOutputStream os = new FileOutputStream(target)) {
            byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void filesCopy(Path source, Path target) {
        long begin = Instant.now().toEpochMilli();
        System.out.println("Using Files.copy()");
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("Cost " + (end - begin) + " ms");

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path createFile() throws IOException {
        Path file = Paths.get("big.file");
        Files.deleteIfExists(file);

        byte[] bytes = new byte[1024 * 1024 * 50];
        IntStream.range(0, bytes.length).forEach(i -> {
            bytes[i] = 0;
        });
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW);

        return file;
    }

}
