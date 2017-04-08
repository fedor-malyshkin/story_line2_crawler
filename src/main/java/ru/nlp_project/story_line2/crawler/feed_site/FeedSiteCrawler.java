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

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;

import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;

public class FeedSiteCrawler {
	@Inject
	protected IContentProcessor contentProcessor;
	private Logger log;
	private FeedSiteConfiguration siteConfig;
	@Inject
	protected IGroovyInterpreter groovyInterpreter;

	FeedSiteCrawler(FeedSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
		log = LoggerFactory.getLogger(this.getClass());
	}


	/**
	 * Метод фактически выполняющий анализ и вызываемый на периодической основе.
	 */
	public void crawlFeed() {
		try {
			String feed = getFeed();
			parseFeed(feed);
		} catch (Exception e) {
			log.error("Error while crawling {}:{}", siteConfig.source, siteConfig.feed, e);
		}
	}


	protected String getContentFromDescription(String source, WebURL webURL,
			SyndContent description) {
		if ("text/html".equalsIgnoreCase(description.getType())) {
			// в данном случае просто убираем тэги из HTML
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
		contentProcessor.initialize(siteConfig.source);
	}



	protected void parseFeed(String feed) throws Exception {
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed syndFeed = input.build(new StringReader(feed));
		for (SyndEntry entry : syndFeed.getEntries())
			processSyndEntry(entry);
	}


	private void processSyndEntry(SyndEntry entry) {
		Date publicationDate = entry.getPublishedDate();
		String title = entry.getTitle().trim();

		WebURL webURL = new WebURL();
		String uri = entry.getUri().trim();
		webURL.setURL(uri);
		if (!siteConfig.parseForContent) {
			String content =
					getContentFromDescription(siteConfig.source, webURL, entry.getDescription());
			String imageUrl = null;
			if (!siteConfig.parseForImage)
				imageUrl = getImageUrlFromEnclosures(entry.getEnclosures());
			contentProcessor.processHtml(webURL, content, title, publicationDate, imageUrl);
		} else {
			throw new IllegalStateException("NIE!");
		}
	}

}
