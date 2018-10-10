package ru.nlp_project.story_line2.crawler.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IKafkaProducer;
import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;

public class KafkaProducerImpl implements IKafkaProducer {

  private static final String EVENT_TYPE_PAGE_CRAWLED = "page-crawled-event";
  private static final String FIELD_EVENT_TYPE = "event-type";
  private static final String FIELD_EVENT_DATA = "event-data";
  private ObjectMapper objectMapper;
  private KafkaProducer<String, String> producer;

  public static IKafkaProducer newInstance(CrawlerConfiguration configuration) {
    KafkaProducerImpl result = new KafkaProducerImpl();
    result.initialize(configuration);
    return result;
  }

  private void initialize(CrawlerConfiguration configuration) {
    objectMapper = new ObjectMapper();

    Properties props = new Properties();
    props.put("bootstrap.servers", configuration.getKafkaConnectionUrl());
    props.put("acks", "all");
    props.put("linger.ms", "5");
    props.put("retries", 5);
    props.put("key.serializer", DEFAULT_KAFKA_SERIALIZER);
    props.put("value.serializer", DEFAULT_KAFKA_SERIALIZER);
    producer = new KafkaProducer<>(props);
  }

  @Override
  public void shutdown() {
    producer.close();
  }

  @Override
  public void writePageCrawledEvent(String sourceName, CrawlerNewsArticle object) throws Exception {
    Map<String, Object> map = formatMapForPageCrawledEvent(object);
    producer.send(createProducerRecord(sourceName, serializeMap(map)));
  }

  private ProducerRecord<String, String> createProducerRecord(String key, String value) {
    return new ProducerRecord<>(PRODUCER_TOPIC, key, value);
  }


  private String serializeMap(Map<String, Object> map) throws JsonProcessingException {
    return objectMapper.writeValueAsString(map);
  }

  private Map<String, Object> formatMapForPageCrawledEvent(CrawlerNewsArticle object) {
    Map<String, Object> result = new HashMap<>();
    result.put(FIELD_EVENT_TYPE, EVENT_TYPE_PAGE_CRAWLED);
    result.put(FIELD_EVENT_DATA, object);
    return result;
  }
}
