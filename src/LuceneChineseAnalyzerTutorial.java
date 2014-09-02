import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;

public class LuceneChineseAnalyzerTutorial {
	public static void main(String[] args) throws IOException, ParseException {
		String[] targetArray = { "关于此次黑客攻击的方式，苹果尚未给出任何确定的结论。",
				"不过，信息安全公司FireEye的专家指出，这可能是一次直接的暴力攻击。",
				"换句话说，如果采取一些额外的信息安全保护措施，那么事故完全可以避免。" };
		String[] searchArray = { "黑客", "信息安全", "保护措施" };

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);

		// 1. create the index
		Directory index = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,
				analyzer);
		IndexWriter w = new IndexWriter(index, config);
		w.deleteAll();
		w.commit();
		for (int i = 0; i < targetArray.length; i++) {
			addDoc(w, targetArray[i]);
		}
		w.close();

		for (int j = 0; j < searchArray.length; j++) {
			System.out.println("\tTarget string:" + targetArray[j]);
			System.out.println("\t\tTokenized:"
					+ getTokenizedString(analyzer, targetArray[j]));
			System.out.println("\tSearch string:" + searchArray[j]);
			System.out.println("\t\tTokenized:"
					+ getTokenizedString(analyzer, searchArray[j]));
			searchDoc(index, analyzer, searchArray[j]);
		}

	}

	private static void addDoc(IndexWriter w, String post) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("post", post, Field.Store.YES));
		w.addDocument(doc);
	}

	private static void searchDoc(Directory index, Analyzer analyzer,
			String searchString) {
		try {
			// 2. query .
			QueryParser parser = new QueryParser(Version.LUCENE_4_9, "post",
					analyzer);
			parser.setAllowLeadingWildcard(true);
			parser.setDefaultOperator(QueryParser.AND_OPERATOR);
			Query q = parser.parse(searchString);
			System.out.println("\tQuery q:" + q);

			// 3. search
			int hitsPerPage = 10;
			DirectoryReader reader = DirectoryReader.open(index);

			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(
					hitsPerPage, true);
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// 4. display results
			System.out.println("\tFound " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println("\t\t" + (i + 1) + ". " + d.get("post"));
			}

			// reader can only be closed when there
			// is no need to access the documents any more.
			reader.close();
		} catch (Exception ex) {
			System.err.println("Exception:" + ex);
		}
	}

	private static String getTokenizedString(Analyzer analyzer,
			String inputString) {
		String returnString = "";
		try {
			TokenStream tokenStream = analyzer.tokenStream("post",
					new StringReader(inputString));
			CharTermAttribute charTermAttribute = tokenStream
					.addAttribute(CharTermAttribute.class);

			tokenStream.reset();
			while (tokenStream.incrementToken()) {
				returnString = returnString + "term["
						+ charTermAttribute.toString() + "]" + " ";
			}
			tokenStream.close();
		} catch (Exception ex) {
			System.err.println("Exception:" + ex);
		}
		return (returnString);
	}
}