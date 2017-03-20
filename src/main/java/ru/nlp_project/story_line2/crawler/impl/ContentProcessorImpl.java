package ru.nlp_project.story_line2.crawler.impl;

import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_CONTENT;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_IMAGE_URL;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_PUB_DATE;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_TITLE;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.mongodb.DBObject;

import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.Crawler;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IImageLoader;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;
import ru.nlp_project.story_line2.crawler.utils.BSONUtils;
import ru.nlp_project.story_line2.crawler.utils.DateTimeUtils;

public class ContentProcessorImpl implements IContentProcessor {

	@Inject
	protected MetricRegistry metricRegistry;
	@Inject
	protected CrawlerConfiguration crawlerConfiguration;
	@Inject
	protected IGroovyInterpreter groovyInterpreter;
	@Inject
	protected IMongoDBClient dbClientManager;
	@Inject
	protected IImageLoader imageLoader;

	private String source;
	private Counter linkProcessed;
	private Counter pagesProcessed;
	private Counter pagesEmpty;
	private Counter pagesFull;

	private Counter extrEmptyTitle;
	private Counter extrEmptyContent;
	private Counter extrEmptyPubDate;
	private Counter extrEmptyImageUrl;
	private Counter extrEmptyImage;
	private Logger log;
	private ZoneId crawlerZoneId;


	@Inject
	public ContentProcessorImpl() {}

	private Date getDateSafe(Map<String, Object> data, String key) {
		Object object = data.get(key);
		if (object != null)
			return (Date) object;
		else
			return null;
	}


	private String getTextSafe(Map<String, Object> data, String key) {
		Object object = data.get(key);
		if (object != null)
			return object.toString();
		else
			return null;
	}


	@Override
	public void initialize(String source) {
		initialize(source, ZoneId.systemDefault());

	}

	public void initialize(String source, ZoneId zoneId) {
		this.crawlerZoneId = zoneId;
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
		extrEmptyImage = metricRegistry
				.counter("extracted_empty_image" + "." + escapedSource + Crawler.METRICS_SUFFIX);
	}

	private byte[] loadImage(WebURL webURL, String imageUrl) {
		try {
			return imageLoader.loadImage(imageUrl);
		} catch (IOException e) {
			log.error("Exception while loading image  {}:{}", source, webURL.getPath(), e);
			return null;
		}
	}

	@Override
	public void processHtml(WebURL webURL, String htmlContent, String titleIn,
			Date publicationDateIn, String imageUrlIn) {
		// если ранне набрали ссылок в базу, то теперь можно дополнительно проверить с
		// актуальной версией скриптов - нужно ли посещать страницу
		if (!shouldProcess(webURL))
			return;

		// skip if exists
		if (dbClientManager.isNewsExists(source, webURL.getPath())) {
			log.debug("Record already exists - skip {}:{} ({})", source, webURL.getPath(),
					webURL.getURL());
			return;
		}

		pagesProcessed.inc();
		Map<String, Object> data = groovyInterpreter.extractData(source, webURL, htmlContent);
		if (null == data) {
			pagesEmpty.inc();
			log.debug("No content {}:{} ({})", source, webURL.getPath(), webURL.getURL());
			return;
		}

		pagesFull.inc();

		Date publicationDate = publicationDateIn == null ? getDateSafe(data, EXTR_KEY_PUB_DATE)
				: publicationDateIn;
		String title = titleIn == null ? getTextSafe(data, EXTR_KEY_TITLE) : titleIn;
		String imageUrl = imageUrlIn == null ? getTextSafe(data, EXTR_KEY_IMAGE_URL) : imageUrlIn;

		String content = getTextSafe(data, EXTR_KEY_CONTENT);
		byte[] imageData = null;
		if (null != imageUrl && !imageUrl.isEmpty()) {
			if (publicationDate == null)
				imageData = loadImage(webURL, imageUrl);
			else {
				Instant tmpInst = DateTimeUtils.now()
						.minusDays(crawlerConfiguration.skipImagesOlderDays).toInstant();
				ZonedDateTime imageDT = DateTimeUtils.toUTC(publicationDate, crawlerZoneId);
				if (imageDT.toInstant().isAfter(tmpInst))
					imageData = loadImage(webURL, imageUrl);
			}
		}

		// metrics
		if (null == publicationDate)
			extrEmptyPubDate.inc();
		if (null == title || title.isEmpty())
			extrEmptyTitle.inc();
		if (null == content || content.isEmpty())
			extrEmptyContent.inc();
		if (null == imageUrl || imageUrl.isEmpty())
			extrEmptyImageUrl.inc();
		if (null == imageData)
			extrEmptyImage.inc();


		try {
			DBObject dbObject = serialize(webURL.getDomain().toLowerCase(), // domain
					webURL.getPath(), // path
					webURL.getURL(), // url
					publicationDate, // "publication_date"
					new Date(), // processDate
					title, // title
					content, // content
					imageUrl, // image_url
					imageData // image
			);
			dbClientManager.writeNews(dbObject, source, webURL.getPath());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	private DBObject serialize(String source, String path, String url, Date publicationDate,
			Date processingDate, String title, String content, String imageUrl, byte[] imageData)
			throws IOException {
		CrawlerNewsArticle article = new CrawlerNewsArticle(source, path, url, publicationDate,
				processingDate, title, content, imageUrl, imageData);
		return BSONUtils.serialize(article);
	}

	@Override
	public boolean shouldVisit(WebURL url) {
		linkProcessed.inc();
		// необходимо учитывать, что тут может возникнуть ситуация, когда в анализируемом сайте
		// имеем ссылку на другой сайт в анализе и в таком случае надо ответить "нет" - нужные
		// данные лишь для основного сайта, другие данные получим в другом парсере
		if (!source.equalsIgnoreCase(url.getDomain()))
			return false;

		return groovyInterpreter.shouldVisit(source, url);
	}

	@Override
	public boolean shouldProcess(WebURL url) {
		return groovyInterpreter.shouldProcess(source, url);
	}

}
