package ru.nlp_project.story_line2.crawler;

import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;

public interface IKafkaProducer {

  public static final String PRODUCER_TOPIC = "crawler-events";
  public static final String DEFAULT_KAFKA_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

  void shutdown();

  void writePageCrawledEvent(String sourceName, CrawlerNewsArticle object) throws Exception;
}
