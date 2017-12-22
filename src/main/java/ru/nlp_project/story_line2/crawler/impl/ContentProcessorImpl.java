package ru.nlp_project.story_line2.crawler.impl;

import com.mongodb.DBObject;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.IOException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IMetricsManager;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;
import ru.nlp_project.story_line2.crawler.utils.BSONUtils;

public class ContentProcessorImpl implements IContentProcessor {

	@Autowired
	protected IMetricsManager metricsManager;
	@Autowired
	protected CrawlerConfiguration crawlerConfiguration;
	@Autowired
	protected IGroovyInterpreter groovyInterpreter;
	@Autowired
	protected IMongoDBClient dbClientManager;

	private String sourceName;
	private Logger log;


	@Override
	public void initialize(String source) {
		this.sourceName = source;
		String loggerClass = String.format("%s[%s]", this.getClass().getCanonicalName(), sourceName);
		log = LoggerFactory.getLogger(loggerClass);
		// initialize metrics
		String escapedSource = sourceName.replace(".", "_");
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
		if (dbClientManager.isCrawlerEntryExists(sourceName, webURL.getPath())) {
			log.debug("Record already exists - skip {}:{} ({})", sourceName, webURL.getPath(),
					webURL.getURL());
			return;
		}

		metricsManager.incrementPagesProcessed(sourceName);
		String rawContent = groovyInterpreter.extractRawData(sourceName, webURL, content);
		if (null == rawContent) {
			metricsManager.incrementPagesEmpty(sourceName);
			log.debug("No content {}:{} ({})", sourceName, webURL.getPath(), webURL.getURL());
			return;
		}

		metricsManager.incrementPagesFull(sourceName);
		// metrics
		if (null == publicationDate) {
			metricsManager.incrementExtractionEmptyPubDate(sourceName);
		}
		if (null == title || title.isEmpty()) {
			metricsManager.incrementExtractionEmptyTitle(sourceName);
		}
		if (null == content || content.isEmpty()) {
			metricsManager.incrementExtractionEmptyContent(sourceName);
		}
		if (null == imageUrl || imageUrl.isEmpty()) {
			metricsManager.incrementExtractionEmptyImageUrl(sourceName);
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
			dbClientManager.writeCrawlerEntry(dbObject, sourceName, webURL.getPath());
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
		metricsManager.incrementLinkProcessed(sourceName);
		// необходимо учитывать, что тут может возникнуть ситуация, когда в анализируемом сайте
		// имеем ссылку на другой сайт в анализе и в таком случае надо ответить "нет" - нужные
		// данные лишь для основного сайта, другие данные получим в другом парсере
		return sourceName.equalsIgnoreCase(url.getDomain()) && groovyInterpreter
				.shouldVisit(sourceName, url);

	}

	@Override
	public boolean shouldProcess(WebURL url) {
		return groovyInterpreter.shouldProcess(sourceName, url);
	}

}
