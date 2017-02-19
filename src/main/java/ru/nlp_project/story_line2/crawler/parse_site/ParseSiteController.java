package ru.nlp_project.story_line2.crawler.parse_site;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.CrawlController.WebCrawlerFactory;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;

public class ParseSiteController {
	@Inject
	MetricRegistry metricRegistry;

	@Inject
	CrawlerConfiguration crawlerConfiguration;

	private ParseSiteConfiguration siteConfig;

	private Logger logger;

	private CrawlController controller;

	public ParseSiteController(ParseSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
		CrawlerBuilder.getComponent().inject(this);
		this.logger = LoggerFactory.getLogger(this.getClass());
	}

	public void initialize() {
		checkScriptsDirectory();
		CrawlConfig crawlConfig = createCrawlConfig(siteConfig);
		try {
			controller = createCrawlController(crawlConfig, siteConfig);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void checkScriptsDirectory() {
		try {
			Collection<File> files = FileUtils.listFiles(new File(crawlerConfiguration.scriptDir),
					new String[] {"groovy"}, true);
			if (files.size() == 0) {
				logger.error("Empty script directory {} ", crawlerConfiguration.scriptDir);
				throw new IllegalStateException(
						"Empty script directory: " + crawlerConfiguration.scriptDir);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	private CrawlController createCrawlController(CrawlConfig crawlConfig,
			ParseSiteConfiguration site) throws Exception {
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

	private CrawlConfig createCrawlConfig(ParseSiteConfiguration site) {
		String crawlStorageFolder = crawlerConfiguration.storageDir + File.separator + site.source;

		try {
			FileUtils.forceMkdir(new File(crawlStorageFolder));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}

		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageFolder);
		config.setResumableCrawling(true);
		return config;
	}


	WebCrawlerFactory<NewsWebCrawler> factory = new WebCrawlerFactory<NewsWebCrawler>() {
		@Override
		public NewsWebCrawler newInstance() throws Exception {
			NewsWebCrawler result = new NewsWebCrawler(siteConfig);
			result.initialize();
			return result;
		}
	};

	public void start() {
		if (crawlerConfiguration.async)
			controller.startNonBlocking(factory, crawlerConfiguration.crawlerPerSite);
		else
			controller.start(factory, crawlerConfiguration.crawlerPerSite);
	}

	public void stop() {
		controller.shutdown();
		controller.waitUntilFinish();
	}

}
