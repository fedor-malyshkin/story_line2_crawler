package ru.nlp_project.story_line2.crawler.feed_site;

import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_CONTENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.mongodb.DBObject;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;

import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.Crawler;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IImageLoader;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;
import ru.nlp_project.story_line2.crawler.utils.BSONUtils;

public class FeedSiteCrawler {


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

	private Logger logger;
	private FeedSiteConfiguration siteConfig;
	private Counter extrEmptyTitle;
	private Counter extrEmptyContent;
	private Counter extrEmptyPubDate;
	private Counter extrEmptyImageUrl;
	private Counter extrEmptyImage;
	private Counter pagesProcessed;
	private Counter pagesEmpty;
	private Counter pagesFull;
	private Meter pagesFullFreq;

	FeedSiteCrawler(FeedSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
		logger = LoggerFactory.getLogger(this.getClass());
	}


	/**
	 * Метод фактически выполняющий анализ и вызываемый на периодической основе.
	 */
	public void crawlFeed() {
		try {
			String feed = getFeed();
			parseFeed(feed);
		} catch (Exception e) {
			logger.error("Error while crawling {}:{}", siteConfig.source, siteConfig.feed, e);
		}
	}


	protected String getContentFromDescription(String source, WebURL webURL,
			SyndContent description) {
		if ("text/html".equalsIgnoreCase(description.getType())) {
			Map<String, Object> extractData =
					groovyInterpreter.extractData(source, webURL, description.getValue());
			String title = (String) extractData.get(EXTR_KEY_CONTENT);
			return title;
		} else {
			throw new IllegalStateException("NIE!");
		}
	}


	private String getFeed() throws IOException {
		InputStream inputStream = new URL(siteConfig.feed).openStream();
		return IOUtils.toString(inputStream);
	}


	protected String getImageUrlFromEnclosures(List<SyndEnclosure> enclosures) {
		// gif|jpg|png|jpeg
		for (SyndEnclosure enc : enclosures) {
			if (enc.getType().matches("image/(gif|jpg|png|jpeg)")) {
				return enc.getUrl();
			}
		}
		return null;
	}


	void initialize() {
		CrawlerBuilder.getComponent().inject(this);
		initializeMetrics();
	}


	protected void initializeMetrics() {
		// initialize metrics
		String siteMetrics = siteConfig.source.replace(".", "_");

		// extraction quality
		pagesProcessed = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.pages.count");
		pagesEmpty = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.empty_pages.count");
		pagesFull = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.full_pages.count");
		pagesFullFreq = metricRegistry
				.meter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.full_pages.freq");



		extrEmptyTitle = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_title.count");
		extrEmptyContent = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_content.count");
		extrEmptyPubDate = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_pub_date.count");
		extrEmptyImageUrl = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_image_url.count");
		extrEmptyImage = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_image.count");
	}


	protected void parseFeed(String feed) throws Exception {
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed syndFeed = input.build(new StringReader(feed));
		for (SyndEntry entry : syndFeed.getEntries())
			processSyndEntry(entry);
	}


	private void processSyndEntry(SyndEntry entry) {
		Date publicationDate = entry.getPublishedDate();
		String uri = entry.getUri().trim();
		WebURL webURL = new WebURL();
		webURL.setURL(uri);
		String source = webURL.getDomain();
		String path = webURL.getPath();
		String title = entry.getTitle().trim();
		String content = null;
		String imageUrl = null;
		byte[] imageData = null;

		// skip if exists
		if (dbClientManager.isNewsExists(siteConfig.source, webURL.getPath())) {
			logger.trace("Record already exists - skip {}:{}", siteConfig.source, webURL.getPath());
			return;
		}
		
		pagesProcessed.inc();

		if (!siteConfig.parseForContent) {
			content = getContentFromDescription(siteConfig.source, webURL, entry.getDescription());
		} else {
			throw new IllegalStateException("NIE!");
		}

		if (null == content) {
			pagesEmpty.inc();
			logger.trace("No content {}:{}", siteConfig.source, webURL.getPath());
			return;
		}
		pagesFull.inc();
		pagesFullFreq.mark();

		if (!siteConfig.parseForContent && !siteConfig.parseForImage) {
			imageUrl = getImageUrlFromEnclosures(entry.getEnclosures());
		} else
			throw new IllegalStateException("NIE!");

		if (null != imageUrl && !imageUrl.isEmpty())
			imageData = loadImage(webURL, imageUrl);

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
			DBObject dbObject = serialize(source, // domain
					path, // path
					uri, // url
					publicationDate, // "publication_date"
					new Date(), // processDate
					title, // title
					content, // content
					imageUrl, // image_url
					imageData // image
			);
			dbClientManager.writeNews(dbObject, siteConfig.source, webURL.getPath());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private byte[] loadImage(WebURL webURL, String imageUrl) {
		try {
			return imageLoader.loadImage(imageUrl);
		} catch (IOException e) {
			logger.error("Exception while loading image  {}:{}", siteConfig.source,
					webURL.getPath(), e);
			return null;
		}
	}

	private DBObject serialize(String source, String path, String url, Date publicationDate,
			Date processingDate, String title, String content, String imageUrl, byte[] imageData)
			throws IOException {
		CrawlerNewsArticle article = new CrawlerNewsArticle(source, path, url, publicationDate,
				processingDate, title, content, imageUrl, imageData);
		return BSONUtils.serialize(article);
	}
}
