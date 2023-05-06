package io.protobj.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FileUtil {


    public static List<Path> getPaths(Predicate<Path> predicate, String... dirs) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String dir : dirs) {
            Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (predicate.test(file)) {
                        files.add(file);
                    }
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return files;
    }

    public static void deleteAll(File file) {
        if (file == null) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        for (File listFile : file.listFiles()) {
            if (listFile.isDirectory()) {
                deleteAll(listFile);
            }else{
                listFile.delete();
            }
        }
        file.delete();
    }
}
