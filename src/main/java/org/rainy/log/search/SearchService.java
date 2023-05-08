package org.rainy.log.search;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import org.rainy.log.config.LuceneConfig;
import org.rainy.log.search.param.SearchParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Slf4j
@Service
public class SearchService {

    private final IndexSearcher searcher;
    private final Analyzer analyzer;

    public SearchService(LuceneConfig luceneConfig) throws IOException {
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(luceneConfig.getIndexPath())));
        this.searcher = new IndexSearcher(reader);
        this.analyzer = new StandardAnalyzer();
    }


    public String search(SearchParam param) {
        final long start = System.currentTimeMillis();
        final String keyword = param.getKeyword();
        final int lines = param.getLines();
        final LocalDateTime startTime = param.getStartTime();
        final LocalDateTime endTime = param.getEndTime();

        StringBuilder result = new StringBuilder();
        try {
            QueryParser queryParser = new QueryParser(Constant.FieldName.CONTENT, analyzer);
            Query query = queryParser.parse(keyword);

            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(query));

            TopDocs docs = searcher.search(query, Integer.MAX_VALUE);
            ScoreDoc[] hits = docs.scoreDocs;
            StoredFields storedFields = searcher.storedFields();
            for (ScoreDoc hit : hits) {
                Document doc = storedFields.document(hit.doc);
                long timestamp = Long.parseLong(doc.get(Constant.FieldName.TIMESTAMP));
                if (startTime != null) {
                    long startTimestamp = startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
                    if (timestamp < startTimestamp) {
                        continue;
                    }
                }

                if (endTime != null) {
                    long endTimestamp = endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
                    if (timestamp > endTimestamp) {
                        continue;
                    }
                }

                String contents = doc.get(Constant.FieldName.CONTENT);

//                TokenStream tokenStream = TokenSources.getTokenStream(SearchConstant.FieldName.CONTENT, TermVectors.EMPTY.get(hit.doc), contents, analyzer, lines);
                TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hit.doc, Constant.FieldName.CONTENT, analyzer);
                TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, contents, false, lines);

                for (TextFragment fragment : fragments) {
                    result.append(fragment);
                }
            }

            int totalHits = Math.toIntExact(docs.totalHits.value);
            log.info("total matching documents: {}, cost: {}ms", totalHits, System.currentTimeMillis() - start);
        } catch (IOException | ParseException | InvalidTokenOffsetsException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

}
