package ru.nlp_project.story_line2.crawler.parse_site;

import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.mongodb.DBObject;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.Crawler;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;
import ru.nlp_project.story_line2.crawler.utils.BSONUtils;

/**
 * Краулер для новостей.
 * 
 * @author fedor
 *
 */
public class NewsWebCrawler extends WebCrawler {

	@Inject
	MetricRegistry metricRegistry;
	@Inject
	CrawlerConfiguration crawlerConfiguration;
	@Inject
	public IGroovyInterpreter groovyInterpreter;
	@Inject
	public IMongoDBClient dbClientManager;

	private Logger logger;
	private Counter linkProcessed;
	private Counter pagesProcessed;
	private Counter pagesEmpty;
	private Counter pagesFull;
	private Meter pagesFullFreq;

	private ParseSiteConfiguration siteConfig;
	private Counter extrEmptyTitle;
	private Counter extrEmptyContent;
	private Counter extrEmptyPubDate;
	private Counter extrEmptyImageUrl;

	public NewsWebCrawler(ParseSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
		logger = LoggerFactory.getLogger(this.getClass());
	}

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

	public void initialize() {
		CrawlerBuilder.getComponent().inject(this);

		// initialize metrics
		String siteMetrics = siteConfig.source.replace(".", "_");
		linkProcessed = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.link.count");
		pagesProcessed = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.pages.count");
		pagesEmpty = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.empty_pages.count");
		pagesFull = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.full_pages.count");
		pagesFullFreq = metricRegistry
				.meter(Crawler.METRICS_PREFIX + siteMetrics + ".processed.full_pages.freq");

		// extraction quality
		extrEmptyTitle = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_title.count");
		extrEmptyContent = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_content.count");
		extrEmptyPubDate = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_pub_date.count");
		extrEmptyImageUrl = metricRegistry
				.counter(Crawler.METRICS_PREFIX + siteMetrics + ".extracted.empty_image_url.count");


	}

	private DBObject serialize(String domain, String path, String url, Date publicationDate,
			Date processingDate, String title, String content, String imageUrl, byte[] imageData)
			throws IOException {
		CrawlerNewsArticle article = new CrawlerNewsArticle(domain, path, url, publicationDate,
				processingDate, title, content, imageUrl, imageData);
		return BSONUtils.serialize(article);
	}

	/**
	 * This method receives two parameters. The first parameter is the page in which we have
	 * discovered this new url and the second parameter is the new url. You should implement this
	 * function to specify whether the given url should be crawled or not (based on your crawling
	 * logic). In this example, we are instructing the crawler to ignore urls that have css, js,
	 * git, ... extensions and to only accept urls that start with "http://www.ics.uci.edu/". In
	 * this case, we didn't need the referringPage parameter to make the decision.
	 */
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		linkProcessed.inc();
		// в случае если страница будет отвергнута -- она не будет проанализирована и самой
		// библиотекой
		return groovyInterpreter.shouldVisit(url.getDomain(), url);
	}

	/**
	 * This function is called when a page is fetched and ready to be processed by your program.
	 */
	@Override
	public void visit(Page page) {
		pagesProcessed.inc();
		WebURL webURL = page.getWebURL();
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();

			Map<String, Object> data =
					groovyInterpreter.extractData(webURL.getDomain(), webURL, html);
			if (null == data) {
				pagesEmpty.inc();
				logger.trace("No content {}:{}", webURL.getDomain(), webURL.getPath());
				return;
			}

			pagesFull.inc();
			pagesFullFreq.mark();

			Date publicationDate = getDateSafe(data, EXTR_KEY_PUB_DATE);
			String title = getTextSafe(data, EXTR_KEY_TITLE);
			String content = getTextSafe(data, EXTR_KEY_CONTENT);
			String imageUrl = getTextSafe(data, EXTR_KEY_IMAGE_URL);

			// metrics
			if (null == publicationDate)
				extrEmptyPubDate.inc();
			if (null == title || title.isEmpty())
				extrEmptyTitle.inc();
			if (null == content || content.isEmpty())
				extrEmptyContent.inc();
			if (null == imageUrl || imageUrl.isEmpty())
				extrEmptyImageUrl.inc();

			byte[] imageData = null;
			if (null != imageUrl && !imageUrl.isEmpty())
				imageData = loadImage(webURL, imageUrl);

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
				dbClientManager.writeNews(dbObject, webURL.getDomain(), webURL.getPath());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private byte[] loadImage(WebURL webURL, String imageUrl) {
		try {
			URL url = new URL(imageUrl);
			InputStream openStream = url.openStream();
			return IOUtils.toByteArray(openStream);
		} catch (IOException e) {
			logger.error("Exception while loading image  {}:{}", webURL.getDomain(), imageUrl, e);
			return null;
		}
	}

}
