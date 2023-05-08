package org.rainy.log.listener;

import lombok.extern.slf4j.Slf4j;
import org.rainy.log.config.LogConfig;
import org.rainy.log.utils.ApplicationContextHelper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Slf4j
public class FileListener implements Runnable {

    private long _lastPos;
    private final long _preLines;
    private final Path _root;
    private final String logFilename;
    private final Callback<FileContent> callback;
    private WatchService _watcher;
    private boolean closed = false;


    public FileListener(long lines, long lastPos, Callback<FileContent> callback) {
        final LogConfig logConfig = ApplicationContextHelper.getBean(LogConfig.class);
        Path logPath = Paths.get(logConfig.getFilename());
        this._preLines = lines;
        this._lastPos = lastPos;
        this._root = logPath.getParent();
        this.logFilename = logPath.toFile().getName();
        this.callback = callback;
        register();
    }


    @Override
    public void run() {
        while (!closed) {
            WatchKey key;
            try {
                key = _watcher.take();
            } catch (InterruptedException e) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path filename = (Path) event.context();
                Path filepath = ((Path) key.watchable()).resolve(filename);

                if (!this.logFilename.equals(filename.toString())) {
                    log.debug("not a specified file: {}", filename);
                    continue;
                }

                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    log.debug("detected file changes: {}", filepath);
                    String content = read(filepath);
                    FileContent fileContent = FileContent.builder()
                            .file(filepath)
                            .content(content)
                            .dateTime(LocalDateTime.now())
                            .build();
                    callback.execute(fileContent);
                } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    log.debug("detected file creates: {}", filepath);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    log.debug("detected file deletes: {}", filepath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void register() {
        try {
            _watcher = FileSystems.getDefault().newWatchService();
            _root.register(_watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            Files.walkFileTree(_root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    dir.register(_watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                    );
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void close() {
        try {
            _watcher.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closed = true;  // 终止当前线程
        }
    }

    private String read(Path path) {
        StringBuilder content = new StringBuilder();
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long readLine = 0;
            long pos = raf.length() - 1;

            while (pos >= 0 && pos > _lastPos) {
                raf.seek(pos);
                char c = (char) raf.readByte();
                if (_lastPos == -1 && c == '\n' && content.length() > 0) {  // \n是换行符
                    readLine++;
                    if (readLine == this._preLines) {
                        break;
                    }
                }
                content.insert(0, c);
                pos--;

                if (_lastPos != -1 && pos == _lastPos) {
                    break;
                }
            }

            _lastPos = raf.length() - 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }


}
