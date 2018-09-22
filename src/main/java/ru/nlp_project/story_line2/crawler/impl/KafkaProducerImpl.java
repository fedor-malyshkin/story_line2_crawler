package ru.nlp_project.story_line2.crawler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IKafkaProducer;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;

public class KafkaProducerImpl implements IKafkaProducer {
  private ObjectMapper objectMapper;

  public static IKafkaProducer newInstance(CrawlerConfiguration configuration) {
    KafkaProducerImpl result = new KafkaProducerImpl();
    result.initialize(configuration);
    return result;
  }

  private void initialize(CrawlerConfiguration configuration) {
    objectMapper = new ObjectMapper();
  }

  @Override
  public void shutdown() {

  }

  @Override
  public void writePageCrawledEvent(String sourceName, CrawlerNewsArticle object) {

  }
}
