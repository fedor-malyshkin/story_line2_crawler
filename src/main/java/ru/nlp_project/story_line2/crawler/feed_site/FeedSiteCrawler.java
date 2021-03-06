package ru.nlp_project.story_line2.crawler.feed_site;

import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IContentProcessor.DataSourcesEnum;
import ru.nlp_project.story_line2.crawler.IDatabaseProcessor;

class FeedSiteCrawler {

  private final CrawlerConfiguration crawlerConfiguration;

  private final IDatabaseProcessor databaseProcessor;

  private final IContentProcessor contentProcessor;

  private Logger log;

  private FeedSiteConfiguration siteConfig;

  FeedSiteCrawler(CrawlerConfiguration crawlerConfiguration, FeedSiteConfiguration siteConfig, IDatabaseProcessor databaseProcessor, IContentProcessor contentProcessor) {
    this.crawlerConfiguration = crawlerConfiguration;
    this.siteConfig = siteConfig;
    this.databaseProcessor = databaseProcessor;
    this.contentProcessor = contentProcessor;

    String loggerClass = String
        .format("%s[%s]", this.getClass().getCanonicalName(), siteConfig.source);
    this.log = LoggerFactory.getLogger(loggerClass);
  }


  /**
   * Метод фактически выполняющий анализ и вызываемый на периодической основе.
   */
  void crawlFeed() {
    try {
      String feed = getFeed();
      parseFeed(feed);
    } catch (Exception e) {
      log.error("Error while crawling {}:{}", siteConfig.source, siteConfig.feed, e);
    }
  }


  private String getFeed() throws IOException {
    InputStream inputStream = new URL(siteConfig.feed).openStream();
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }


  private String getImageUrlFromEnclosures(List<SyndEnclosure> enclosures) {
    // gif|jpg|png|jpeg
    for (SyndEnclosure enc : enclosures) {
      if (enc.getType().matches("image/(gif|jpg|png|jpeg)")) {
        return enc.getUrl();
      }
    }
    return null;
  }


  void initialize() {
    databaseProcessor.initialize(crawlerConfiguration.crawlerStorageDir + File.separator + siteConfig.source);
    contentProcessor.initialize(siteConfig.source);
  }


  void parseFeed(String feed) throws Exception {
    SyndFeedInput input = new SyndFeedInput();
    SyndFeed syndFeed = input.build(new StringReader(feed));
    for (SyndEntry entry : syndFeed.getEntries()) {
      processSyndEntry(entry);
    }
  }


  private void processSyndEntry(SyndEntry entry) {
    Date publicationDate = entry.getPublishedDate();
    String title = entry.getTitle().trim();
    WebURL webURL = new WebURL();
    String uri = entry.getUri().trim();
    if (databaseProcessor.contains(uri)) {
      return;
    }
    webURL.setURL(uri);
    if (!siteConfig.parseForContent) {
      String content = entry.getDescription().getValue();
      String imageUrl = null;
      if (!siteConfig.parseForImage) {
        imageUrl = getImageUrlFromEnclosures(entry.getEnclosures());
      }
      contentProcessor.processHtml(DataSourcesEnum.RSS, webURL, content, title, publicationDate, imageUrl);
      databaseProcessor.add(uri);
    } else {
      throw new IllegalStateException("NIE!");
    }
  }

}
