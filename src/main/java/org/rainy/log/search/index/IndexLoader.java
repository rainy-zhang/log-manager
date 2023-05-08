package org.rainy.log.search.index;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.rainy.log.config.LogConfig;
import org.rainy.log.config.LuceneConfig;
import org.rainy.log.listener.FileContent;
import org.rainy.log.listener.FileListener;
import org.rainy.log.search.Constant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Slf4j
@Component
public class IndexLoader implements CommandLineRunner {

    private static final String[] DATETIME_FORMATTERS = {"yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss,SSS"};
    private static final Pattern DATETIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d{3}");
    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final AtomicLong DIFF_SIZE = new AtomicLong(0);
    private static final long LOAD_CAPACITY = 1000;
    private static final CopyOnWriteArrayList<FileContent> FILE_CONTENT_STORAGE = new CopyOnWriteArrayList<>();

    private final LuceneConfig luceneConfig;
    private final LogConfig logConfig;

    public IndexLoader(LuceneConfig luceneConfig, LogConfig logConfig) {
        this.luceneConfig = luceneConfig;
        this.logConfig = logConfig;
    }

    @Override
    public void run(String... args) {
        long lastPos = Paths.get(logConfig.getFilename()).toFile().length();
        load(LoadType.FULL);
        new Thread(new FileListener(Long.MAX_VALUE, lastPos, fileContent -> {
            FILE_CONTENT_STORAGE.add(fileContent);
            long diff = DIFF_SIZE.addAndGet(fileContent.getContent().length());
            if (diff >= LOAD_CAPACITY) {
                try (IndexWriter writer = getIndexWriter(LoadType.INCR)) {
                    for (FileContent content : FILE_CONTENT_STORAGE) {
                        indexDoc(writer, content);
                    }
                    writer.commit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("INCR index load completed, write docs: {}", FILE_CONTENT_STORAGE.size());
                FILE_CONTENT_STORAGE.clear();
                DIFF_SIZE.set(0);
            } else {
                log.info("detect log file changed, current diff: {}", DIFF_SIZE);
            }
        })).start();
    }

    public void load(LoadType loadType) {
        final long start = System.currentTimeMillis();

        final Path docDir = Paths.get(luceneConfig.getDocsPath());
        try (IndexWriter writer = getIndexWriter(loadType)) {

            if (Files.isDirectory(docDir)) {
                Files.walkFileTree(docDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // TODO: 如果是压缩包需要解压
                        String mimeType = Files.probeContentType(file);
                        if (mimeType != null && (mimeType.startsWith("application/zip") || mimeType.startsWith("application/gz"))) {

                            return FileVisitResult.CONTINUE;
                        }
                        indexDoc(writer, file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                indexDoc(writer, docDir);
            }

            writer.forceMerge(1);
            writer.commit();

            try (IndexReader reader = DirectoryReader.open(writer.getDirectory())) {
                log.info("{} index load completed, write docs: {}, cost: {}ms", loadType.name(), reader.numDocs(), System.currentTimeMillis() - start);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private IndexWriter getIndexWriter(LoadType loadType) throws IOException {
        final String indexPath = luceneConfig.getIndexPath();

        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);

        if (loadType == LoadType.FULL) {
            writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }
        writerConfig.setRAMBufferSizeMB(256.0);
        return new IndexWriter(dir, writerConfig);
    }

    private void indexDoc(IndexWriter writer, Path file) throws IOException {
        StringBuilder content = new StringBuilder();
        String filepath = file.toString();
        Files.lines(file).forEach(line -> {
            Matcher matcher = DATETIME_PATTERN.matcher(line);
            content.append(line);
            if (matcher.find()) {
                String timestampStr = matcher.group();
                try {
                    Document doc = new Document();
                    doc.add(new StringField(Constant.FieldName.PATH, filepath, Field.Store.YES));
                    doc.add(new TextField(Constant.FieldName.CONTENT, content.toString(), Field.Store.YES));
                    doc.add(new StringField(Constant.FieldName.TIMESTAMP, String.valueOf(DateUtils.parseDate(timestampStr, DATETIME_FORMATTERS).getTime()), Field.Store.YES));
                    writer.addDocument(doc);
                } catch (IOException | ParseException e) {
                    log.error("parse date error: {}", line);
                } finally {
                    content.setLength(0);
                }
            }
        });

    }

    private void indexDoc(IndexWriter writer, FileContent fileContent) throws IOException {
        String content = fileContent.getContent();
        for (String line : content.split("\n")) {
            Matcher matcher = DATETIME_PATTERN.matcher(line);
            String dateTime = null;
            if (matcher.find()) {
                try {
                    String timestampStr = matcher.group();
                    dateTime = String.valueOf(DateUtils.parseDate(timestampStr, DATETIME_FORMATTERS).getTime());
                } catch (ParseException e) {
                    log.error("parse date error: {}", line);
                }
            }
            if (StringUtils.isEmpty(dateTime)) {
                dateTime = fileContent.getDateTime().format(LOCAL_DATE_FORMATTER);
            }

            Document doc = new Document();
            doc.add(new StringField(Constant.FieldName.PATH, fileContent.getFile().toString(), Field.Store.YES));
            doc.add(new TextField(Constant.FieldName.CONTENT, line, Field.Store.YES));
            doc.add(new StringField(Constant.FieldName.TIMESTAMP, dateTime, Field.Store.YES));
            writer.addDocument(doc);
        }

    }


}
