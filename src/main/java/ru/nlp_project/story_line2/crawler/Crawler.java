package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.CrawlController.WebCrawlerFactory;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import io.dropwizard.lifecycle.Managed;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.SiteConfiguration;

public class Crawler implements Managed {

	@Override
	public void start() throws Exception {
		initialize();
		run();
	}

	@Override
	public void stop() throws Exception {
		shutdown();
	}

	private void shutdown() {
		for (CrawlController c : controllers) {
			c.shutdown();
			c.waitUntilFinish();
		}
		mongoDBClientManager.shutdown();

	}

	public static Crawler newInstance(CrawlerConfiguration configuration) throws Exception {
		Crawler result = new Crawler(configuration);
		// because it is managed object - it starts in #start method)
		// result.initialize();
		return result;
	}

	private GroovyInterpreter groovyInterpreter;
	private List<CrawlController> controllers;
	private CrawlerConfiguration configuration;
	private MongoDBClientManager mongoDBClientManager;

	private Crawler(CrawlerConfiguration configuration) {
		this.configuration = configuration;
	}

	private void initialize() throws Exception {
		mongoDBClientManager = MongoDBClientManager.newInstance(configuration);
		groovyInterpreter = GroovyInterpreter.newInstance(configuration);
		controllers = new ArrayList<>();

		for (SiteConfiguration site : configuration.sites) {
			CrawlConfig crawlConfig = createCrawlConfig(site);
			CrawlController controller = createCrawlController(crawlConfig, site);
			controllers.add(controller);
		}
	}

	WebCrawlerFactory<NewsWebCrawler> factory = new WebCrawlerFactory<NewsWebCrawler>() {
		@Override
		public NewsWebCrawler newInstance() throws Exception {
			return new NewsWebCrawler(configuration, groovyInterpreter, mongoDBClientManager);
		}
	};

	private void run() {
		if (configuration.async)
			for (CrawlController c : controllers)
				c.startNonBlocking(factory, configuration.crawlerPerSite);
		else
			for (CrawlController c : controllers)
				c.start(factory, configuration.crawlerPerSite);
	}

	private CrawlController createCrawlController(CrawlConfig crawlConfig, SiteConfiguration site)
			throws Exception {
		/*
		 * Instantiate the controller for this crawl.
		 */
		PageFetcher pageFetcher = new PageFetcher(crawlConfig);
		RobotstxtConfig robotsTxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotsTxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(crawlConfig, pageFetcher, robotstxtServer);

		/*
		 * For each crawl, you need to add some seed urls. These are the first URLs that are fetched
		 * and then the crawler starts following links which are found in these pages
		 */
		controller.addSeed(site.seed);
		return controller;
	}

	private CrawlConfig createCrawlConfig(SiteConfiguration site) {
		String crawlStorageFolder = configuration.storageDir + File.separator + site.domain;
		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageFolder);
		config.setResumableCrawling(true);
		return config;
	}

	public void dumpsNewsToFiles() throws Exception {
		initialize();
		CrawlController crawlController = controllers.get(0);
			NewsWebCrawler webCrawler = factory.newInstance();
		webCrawler.dumpsAllNewsToFiles();
	}
}
