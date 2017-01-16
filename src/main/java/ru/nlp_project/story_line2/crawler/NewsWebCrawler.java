package ru.nlp_project.story_line2.crawler;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Краулер для новостей.
 * 
 * @author fedor
 *
 */
public class NewsWebCrawler extends WebCrawler {
	@Inject
	public IGroovyInterpreter groovyInterpreter;
	@Inject
	public CrawlerConfiguration configuration;
	@Inject
	public IMongoDBClient dbClientManager;
	private ObjectMapper mapper;
	private Logger myLogger;

	public NewsWebCrawler() {
		myLogger = LoggerFactory.getLogger(this.getClass());
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
		// в случае если страница будет отвергнута -- она не будет проанализирована и самой
		// библиотекой
		return groovyInterpreter.shouldVisit(url.getDomain(), url);
	}

	/**
	 * This function is called when a page is fetched and ready to be processed by your program.
	 */
	@Override
	public void visit(Page page) {
		WebURL webURL = page.getWebURL();
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();

			Map<String, Object> data = groovyInterpreter.extractData(webURL.getDomain(), html);
			if (null == data) {
				String msg = String.format("[%s] '%s' has no content.", webURL.getDomain(),
						webURL.getURL());
				myLogger.debug(msg);
				return;
			}

			try {
				String json =
						serialize(webURL.getDomain().toLowerCase(), webURL.getPath().toLowerCase(),
								webURL.getURL().toLowerCase(), (Date) data.get("date"), new Date(),
								data.get("title").toString(), data.get("content").toString());
				dbClientManager.writeNews(json, webURL.getDomain(), webURL.getPath());
			} catch (JsonProcessingException e) {
				myLogger.error(e.getMessage(), e);
			}
		}
	}

	public ObjectMapper getObjectMapper() {
		if (mapper == null) {
			mapper = new ObjectMapper(new JsonFactory());
			mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		}
		return mapper;
	}

	private String serialize(String domain, String path, String url, Date date, Date creationDate,
			String title, String content) throws JsonProcessingException {
		ObjectMapper mapper = getObjectMapper();
		NewsArticle article =
				new NewsArticle(creationDate, date, content, path, domain, title, url);
		return mapper.writeValueAsString(article);
	}

}
