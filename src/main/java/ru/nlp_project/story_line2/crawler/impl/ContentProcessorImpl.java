package ru.nlp_project.story_line2.crawler.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.mongodb.DBObject;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.IOException;
import java.util.Date;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nlp_project.story_line2.crawler.Crawler;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;
import ru.nlp_project.story_line2.crawler.utils.BSONUtils;

public class ContentProcessorImpl implements IContentProcessor {

	@Inject
	protected MetricRegistry metricRegistry;
	@Inject
	protected CrawlerConfiguration crawlerConfiguration;
	@Inject
	protected IGroovyInterpreter groovyInterpreter;
	@Inject
	protected IMongoDBClient dbClientManager;

	private String source;
	private Counter linkProcessed;
	private Counter pagesProcessed;
	private Counter pagesEmpty;
	private Counter pagesFull;

	private Counter extrEmptyTitle;
	private Counter extrEmptyContent;
	private Counter extrEmptyPubDate;
	private Counter extrEmptyImageUrl;
	private Logger log;


	@Inject
	public ContentProcessorImpl() {
	}


	@Override
	public void initialize(String source) {
		this.source = source;
		String loggerClass = String.format("%s[%s]", this.getClass().getCanonicalName(), source);
		log = LoggerFactory.getLogger(loggerClass);

		// initialize metrics
		String escapedSource = source.replace(".", "_");

		linkProcessed = metricRegistry
				.counter("processed_link" + "." + escapedSource + Crawler.METRICS_SUFFIX);
		pagesProcessed = metricRegistry
				.counter("processed_pages" + "." + escapedSource + Crawler.METRICS_SUFFIX);
		pagesEmpty = metricRegistry
				.counter("processed_empty_pages" + "." + escapedSource + Crawler.METRICS_SUFFIX);
		pagesFull = metricRegistry
				.counter("processed_full_pages" + "." + escapedSource + Crawler.METRICS_SUFFIX);

		// extraction quality
		extrEmptyTitle = metricRegistry
				.counter("extracted_empty_title" + "." + escapedSource + Crawler.METRICS_SUFFIX);
		extrEmptyContent = metricRegistry
				.counter("extracted_empty_content" + "." + escapedSource + Crawler.METRICS_SUFFIX);
		extrEmptyPubDate = metricRegistry
				.counter("extracted_empty_pub_date" + "." + escapedSource + Crawler.METRICS_SUFFIX);
		extrEmptyImageUrl = metricRegistry.counter(
				"extracted_empty_image_url" + "." + escapedSource + Crawler.METRICS_SUFFIX);
	}


	@Override
	public void processHtml(WebURL webURL, String htmlContent) {
		processHtml(webURL, htmlContent, null, null, null);
	}


	@Override
	public void processHtml(WebURL webURL, String content, String title,
			Date publicationDate, String imageUrl) {
		// если ранне набрали ссылок в базу, то теперь можно дополнительно проверить с
		// актуальной версией скриптов - нужно ли посещать страницу
		if (!shouldProcess(webURL)) {
			return;
		}

		// skip if exists
		if (dbClientManager.isCrawlerEntryExists(source, webURL.getPath())) {
			log.debug("Record already exists - skip {}:{} ({})", source, webURL.getPath(),
					webURL.getURL());
			return;
		}

		pagesProcessed.inc();
		String rawContent = groovyInterpreter.extractRawData(source, webURL, content);
		if (null == rawContent) {
			pagesEmpty.inc();
			log.debug("No content {}:{} ({})", source, webURL.getPath(), webURL.getURL());
			return;
		}

		pagesFull.inc();
		// metrics
		if (null == publicationDate) {
			extrEmptyPubDate.inc();
		}
		if (null == title || title.isEmpty()) {
			extrEmptyTitle.inc();
		}
		if (null == content || content.isEmpty()) {
			extrEmptyContent.inc();
		}
		if (null == imageUrl || imageUrl.isEmpty()) {
			extrEmptyImageUrl.inc();
		}

		// тут не надо ничего корректировать
		Date processingDate = new Date();

		try {
			DBObject dbObject = serialize(webURL.getDomain().toLowerCase(), // domain
					webURL.getPath(), // path
					webURL.getURL(), // url
					publicationDate, // "publication_date"
					processingDate, // processing date
					title, // title
					imageUrl, // image_url
					rawContent // raw content

			);
			dbClientManager.writeCrawlerEntry(dbObject, source, webURL.getPath());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	private DBObject serialize(String source, String path, String url, Date publicationDate,
			Date processingDate, String title, String imageUrl, String rawContent)
			throws IOException {
		CrawlerNewsArticle article = new CrawlerNewsArticle(source, path, url, publicationDate,
				processingDate, title, imageUrl, rawContent);
		return BSONUtils.serialize(article);
	}

	@Override
	public boolean shouldVisit(WebURL url) {
		linkProcessed.inc();
		// необходимо учитывать, что тут может возникнуть ситуация, когда в анализируемом сайте
		// имеем ссылку на другой сайт в анализе и в таком случае надо ответить "нет" - нужные
		// данные лишь для основного сайта, другие данные получим в другом парсере
		return source.equalsIgnoreCase(url.getDomain()) && groovyInterpreter.shouldVisit(source, url);

	}

	@Override
	public boolean shouldProcess(WebURL url) {
		return groovyInterpreter.shouldProcess(source, url);
	}

}
