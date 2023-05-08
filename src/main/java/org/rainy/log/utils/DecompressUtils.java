package org.rainy.log.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
public class DecompressUtils {

    public static void unZip(File file, String output) {
        try {
            ZipFile zipFile = new ZipFile(file, Charset.defaultCharset());

            if (Files.notExists(Paths.get(output))) {
                Files.createDirectories(Paths.get(output));
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    createDirectory(output, zipEntry.getName());
                } else {
                    File tempFile = new File(output + File.separator + zipEntry.getName());
                    try (
                            InputStream in = zipFile.getInputStream(zipEntry);
                            OutputStream out = new FileOutputStream(tempFile)
                    ) {
                        int len;
                        byte[] buffer = new byte[8192];
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0 , len);
                        }
                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    private static void createDirectory(String path, String subPath) {
        File file = new File(path);
        if (subPath != null) {
            file = new File(path + File.separator + subPath);
        }
        if (!file.exists()) {
            if (file.mkdirs()) {

            }
        }
    }

}
