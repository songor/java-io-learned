package com.io.aio;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryStreamDemo {

    public static void main(String[] args) {
        Path dir = Paths.get("");
        // 在目录中查找文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
            stream.forEach(path -> {
                System.out.println(path.getFileName());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // 遍历目录树
            Files.walkFileTree(dir, new FindJavaVisitor());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class FindJavaVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(".java")) {
                System.out.println(file.getFileName());
            }
            return FileVisitResult.CONTINUE;
        }

    }

}
