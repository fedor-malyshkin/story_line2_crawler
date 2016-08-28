package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class NewsWebCrawler extends WebCrawler {

	private GroovyInterpreter groovyInterpreter;
	private CrawlerConfiguration configuration;
	private MongoDBClientManager dbClientManager;
	private ObjectMapper mapper;
	private Logger myLogger;
	private SimpleDateFormat dateFormatter;

	public NewsWebCrawler(CrawlerConfiguration configuration, GroovyInterpreter groovyInterpreter,
			MongoDBClientManager dbClientManager) {
		myLogger = LoggerFactory.getLogger(this.getClass());
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		this.dbClientManager = dbClientManager;
		this.groovyInterpreter = groovyInterpreter;
		this.configuration = configuration;
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

			if (configuration.storeFiles) {
				File file = new File("/var/tmp/" + System.currentTimeMillis() + ".json");
				try {
					String json = serialize(webURL.getDomain().toLowerCase(),
							webURL.getPath().toLowerCase(), webURL.getURL().toLowerCase(),
							data.get("date").toString(), dateFormatter.format(new Date()),
							data.get("title").toString(), data.get("content").toString());
					FileUtils.write(file, json);
					myLogger.trace("Create '" + file + "' file");
				} catch (IOException e) {
					myLogger.error(e.getMessage(), e);
				}
			} else {
				try {
					String json = serialize(webURL.getDomain().toLowerCase(),
							webURL.getPath().toLowerCase(), webURL.getURL().toLowerCase(),
							data.get("date").toString(), dateFormatter.format(new Date()),
							data.get("title").toString(), data.get("content").toString());
					dbClientManager.writeNews(json, webURL.getDomain(), webURL.getPath());
				} catch (JsonProcessingException e) {
					myLogger.error(e.getMessage(), e);
				}
			}
		}
	}

	public ObjectMapper getObjectMapper() {
		if (mapper == null)
			mapper = new ObjectMapper();
		return mapper;
	}

	private String serialize(String domain, String path, String url, String date,
			String creationDate, String title, String content) throws JsonProcessingException {
		ObjectMapper mapper = getObjectMapper();
		Map<String, String> map = new HashMap<>();

		map.put("domain", domain);
		map.put("path", path);
		map.put("url", url);
		map.put("date", date);
		map.put("creationDate", creationDate);
		map.put("content", content);
		map.put("title", title);

		return mapper.writeValueAsString(map);

	}


}
