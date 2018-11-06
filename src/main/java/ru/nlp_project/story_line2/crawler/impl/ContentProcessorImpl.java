package ru.nlp_project.story_line2.crawler.impl;

import edu.uci.ics.crawler4j.url.WebURL;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IKafkaProducer;
import ru.nlp_project.story_line2.crawler.IMetricsManager;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;

public class ContentProcessorImpl implements IContentProcessor {

  @Autowired
  private IMetricsManager metricsManager;

  @Autowired
  protected CrawlerConfiguration crawlerConfiguration;

  @Autowired
  private IGroovyInterpreter groovyInterpreter;

  @Autowired
  private IKafkaProducer kafkaProducer;

  private String sourceName;

  private Logger log;


  @Override
  public void initialize(String source) {
    this.sourceName = source;
    String loggerClass = String.format("%s[%s]", this.getClass().getCanonicalName(), sourceName);
    log = LoggerFactory.getLogger(loggerClass);

  }

  @Override
  public void processHtml(DataSourcesEnum dataSource, WebURL webURL, String htmlContent) {
    processHtml(dataSource, webURL, htmlContent, null, null, null);
  }


  @Override
  public void processHtml(DataSourcesEnum dataSource, WebURL webURL, String content, String title,
                          Date publicationDate, String imageUrl) {
    // если ранне набрали ссылок в базу, то теперь можно дополнительно проверить с
    // актуальной версией скриптов - нужно ли посещать страницу
    if (!shouldProcess(dataSource, webURL)) {
      return;
    }

    metricsManager.incrementPagesProcessed(dataSource, sourceName);

    String rawContent = groovyInterpreter.extractRawData(sourceName, webURL, content);
    if (null == rawContent) {
      metricsManager.incrementPagesEmpty(dataSource, sourceName);
      log.info("No content {}:{} ({})", sourceName, webURL.getPath(), webURL.getURL());
      return;
    }

    metricsManager.incrementPagesFull(dataSource, sourceName);

    // collect statistics only if data from RSS analysis
    if (dataSource != DataSourcesEnum.PARSE) {
      // metrics
      if (null == publicationDate) {
        metricsManager.incrementExtractionEmptyPubDate(dataSource, sourceName);
      }
      if (null == title || title.isEmpty()) {
        metricsManager.incrementExtractionEmptyTitle(dataSource, sourceName);
      }
      if (null == content || content.isEmpty()) {
        metricsManager.incrementExtractionEmptyContent(dataSource, sourceName);
      }
      if (null == imageUrl || imageUrl.isEmpty()) {
        metricsManager.incrementExtractionEmptyImageUrl(dataSource, sourceName);
      }
    }

    // тут не надо ничего корректировать
    Date processingDate = new Date();

    try {
      CrawlerNewsArticle objectToQueue = new CrawlerNewsArticle(webURL.getDomain().toLowerCase(), // domain
                                                                webURL.getPath(), // path
                                                                webURL.getURL(), // url
                                                                publicationDate, // "publication_date"
                                                                processingDate, // processing date
                                                                title, // title
                                                                imageUrl, // image_url
                                                                rawContent // raw content
      );
      kafkaProducer.writePageCrawledEvent(sourceName, objectToQueue);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }


  @Override
  public boolean shouldVisit(DataSourcesEnum dataSource, WebURL url) {
    metricsManager.incrementLinkProcessed(dataSource, sourceName);
    // необходимо учитывать, что тут может возникнуть ситуация, когда в анализируемом сайте
    // имеем ссылку на другой сайт в анализе и в таком случае надо ответить "нет" - нужные
    // данные лишь для основного сайта, другие данные получим в другом парсере
    return sourceName.equalsIgnoreCase(url.getDomain()) && groovyInterpreter
        .shouldVisit(sourceName, url);

  }

  @Override
  public boolean shouldProcess(DataSourcesEnum dataSource, WebURL url) {
    return groovyInterpreter.shouldProcess(sourceName, url);
  }

}
