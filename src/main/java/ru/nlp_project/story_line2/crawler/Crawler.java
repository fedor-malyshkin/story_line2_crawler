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
import ru.nlp_project.story_line2.crawler.ConfigurationReader.SiteConfiguration;

public class Crawler {
  public static Crawler newInstance(String configFile, boolean startAsync) throws Exception {
    Crawler result = new Crawler(configFile, startAsync);
    result.initialize();
    return result;
  }

  private String configFile;
  private boolean startAsync;
  private GroovyInterpreter groovyInterpreter;
  private List<CrawlController> controllers;
  private ConfigurationReader configurationReader;

  private Crawler(String configFile, boolean startAsync) {
    this.configFile = configFile;
    this.startAsync = startAsync;
  }

  private void initialize() throws Exception {
    configurationReader = ConfigurationReader.newInstance(configFile);
    groovyInterpreter = GroovyInterpreter.newInstance(configurationReader);
    controllers = new ArrayList<>();

    for (SiteConfiguration site : configurationReader.getConfigurationMain().sites) {
      CrawlConfig crawlConfig = createCrawlConfig(site);
      CrawlController controller = createCrawlController(crawlConfig, site);
      controllers.add(controller);
    }
  }

  WebCrawlerFactory<NewsWebCrawler> factory = new WebCrawlerFactory<NewsWebCrawler>() {

    @Override
    public NewsWebCrawler newInstance() throws Exception {
      return new NewsWebCrawler(groovyInterpreter, configurationReader);
    }
  };

  public void run() {
    if (startAsync)
      for (CrawlController c : controllers)
        c.startNonBlocking(factory, configurationReader.getConfigurationMain().crawlerPerSite);
    else
      for (CrawlController c : controllers)
        c.start(factory, configurationReader.getConfigurationMain().crawlerPerSite);
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
     * For each crawl, you need to add some seed urls. These are the first URLs that are fetched and
     * then the crawler starts following links which are found in these pages
     */
    controller.addSeed(site.seed);
    return controller;
  }

  private CrawlConfig createCrawlConfig(SiteConfiguration site) {
    String crawlStorageFolder =
        configurationReader.getConfigurationMain().storageDir + File.separator + site.domain;
    CrawlConfig config = new CrawlConfig();
    config.setCrawlStorageFolder(crawlStorageFolder);
    config.setResumableCrawling(true);
    return config;
  }
}
