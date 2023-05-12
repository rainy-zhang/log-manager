package org.rainy.log.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Slf4j
public class DecompressUtils {

    public static boolean decompress(File file, Path output) {
        String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
        if (extension.equals("zip")) {
            unZip(file, output);
        } else if (extension.equals("gz")) {
            unGZip(file, output);
        } else {
            log.debug("illegal file type: {}", file.getName());
            return false;
        }
        return true;
    }

    public static void unZip(File file, Path output) {
        try {
            ZipFile zipFile = new ZipFile(file, Charset.defaultCharset());

            if (Files.notExists(output)) {
                Files.createDirectories(output);
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    createDirectory(output.toString(), zipEntry.getName());
                } else {
                    File tempFile = new File(output + File.separator + zipEntry.getName());
                    try (
                            InputStream in = zipFile.getInputStream(zipEntry);
                            OutputStream out = new FileOutputStream(tempFile)
                    ) {
                        int len;
                        byte[] buffer = new byte[8192];
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }

                }
            }

        } catch (IOException e) {
            log.error("decompress zip error", e);
        }

    }

    public static void unGZip(File file, Path output) {
        try (
                GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
                OutputStream out = new FileOutputStream(output.toFile());
        ) {
            if (Files.notExists(output)) {
                Files.createDirectories(output);
            }

            int len;
            byte[] buffer = new byte[8192];
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0 , len);
            }

        } catch (IOException e) {
            e.printStackTrace();
            log.error("decompress gzip error", e);
        }

    }


    private static void createDirectory(String path, String subPath) {
        File file = new File(path);
        if (subPath != null) {
            file = new File(path + File.separator + subPath);
        }
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("decompress error, because directory create failed: {}", path);
            }
        }
    }


}
