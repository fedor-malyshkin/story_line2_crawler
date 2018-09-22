package ru.nlp_project.story_line2.crawler;

import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;

public interface IKafkaProducer {

  public static final String PRODUCER_TOPIC = "crawler-events";

  void shutdown();

  void writePageCrawledEvent(String sourceName, CrawlerNewsArticle object);
}
