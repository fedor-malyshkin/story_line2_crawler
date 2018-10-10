package ru.nlp_project.story_line2.crawler.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_CONTENT;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_IMAGE_URL;

import edu.uci.ics.crawler4j.url.WebURL;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor.DataSourcesEnum;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IKafkaProducer;
import ru.nlp_project.story_line2.crawler.IMetricsManager;
import ru.nlp_project.story_line2.crawler.KafkaLocal;
import ru.nlp_project.story_line2.crawler.ZooKeeperLocal;
import ru.nlp_project.story_line2.crawler.impl.ContentProcessorImplTest.TestClass;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestClass.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ContentProcessorImplTest {

  private static final String CLASSPATH_PREFIX = "/ru/nlp_project/story_line2/crawler/impl/ContentProcessorImplTest";
  private static KafkaLocal kafkaLocal;
  private static ZooKeeperLocal zookeeperLocal;

  @Autowired
  private ContentProcessorImpl testable;

  @Autowired
  private IKafkaProducer kafkaProducer;

  @Autowired
  private IGroovyInterpreter groovyInterpreter;

  private WebURL webUrl;

  @Rule
  public TestName testName = new TestName();

  @BeforeClass
  public static void setUpClass() throws Exception {
    zookeeperLocal = new ZooKeeperLocal(0, null);
    zookeeperLocal.start();
    kafkaLocal = new KafkaLocal(0, null, zookeeperLocal);
    kafkaLocal.start();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    kafkaLocal.stop();
    zookeeperLocal.stop();
  }

  @After
  public void tearDown() {
    kafkaLocal.releaseConsumer();
    kafkaLocal.deleteAllTopics();
  }


  @Before
  public void setUp() {
    webUrl = new WebURL();
    webUrl.setURL("https://www.bnkomi.ru/data/news/60691/");
    testable.initialize("test.source");
    kafkaLocal.createTopic(IKafkaProducer.DEFAULT_KAFKA_SERIALIZER);
    kafkaLocal.initConsumer();
  }


  @Test
  public void testProcessHtmlFromFeed() throws InterruptedException {
    HashMap<String, Object> extrData = new HashMap<>();
    extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
    extrData.put(EXTR_KEY_CONTENT, "test_content");
    when(groovyInterpreter.extractRawData(eq("test.source"), any(), anyString()))
        .thenReturn("some text");
    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);
    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "", "some", null, "Some");

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
    ConsumerRecord<String, String> record = kafkaLocal.getMessageFromTopic(IKafkaProducer.PRODUCER_TOPIC);
    Assertions.assertThat(record).isNotNull();
    Assertions.assertThat(jsonMatchExpected(record.value())).isTrue();
  }

  @Test
  public void testProcessHtmlFromParser() throws InterruptedException {
    when(groovyInterpreter.extractRawData(anyString(), any(), anyString())).thenReturn("some");
    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "");

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
    verify(groovyInterpreter).extractRawData(eq("test.source"), any(), anyString());
    ConsumerRecord<String, String> record = kafkaLocal.getMessageFromTopic(IKafkaProducer.PRODUCER_TOPIC);
    Assertions.assertThat(record).isNotNull();
    Assertions.assertThat(jsonMatchExpected(record.value())).isTrue();
  }


  @Test
  public void testProcessHtml_OldPublicationDate_NoImageLoading() throws InterruptedException {

    when(groovyInterpreter.extractRawData(eq("test.source"), any(), anyString()))
        .thenReturn("some text");

    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "", null, null, null);

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
    ConsumerRecord<String, String> record = kafkaLocal.getMessageFromTopic(IKafkaProducer.PRODUCER_TOPIC);
    Assertions.assertThat(record).isNotNull();
    Assertions.assertThat(jsonMatchExpected(record.value())).isTrue();

  }

  @Test
  public void testProcessHtml_ShouldNotProcess() throws InterruptedException {
    HashMap<String, Object> extrData = new HashMap<>();
    extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
    extrData.put(EXTR_KEY_CONTENT, "test_contnt");

    when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(false);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "", null, null, null);

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
    Assertions.assertThat(kafkaLocal.isNoMessages(IKafkaProducer.PRODUCER_TOPIC)).isTrue();
  }

  @Test
  public void testShouldVisitWrongDomain() throws InterruptedException {
    when(groovyInterpreter.shouldVisit(anyString(), any())).thenReturn(true);
    testable.initialize("rambler.ru");

    webUrl = new WebURL();
    webUrl.setURL("https://www.bnkomi.ru/data/news/60691/");

    assertThat(testable.shouldVisit(DataSourcesEnum.PARSE, webUrl), is(false));

    webUrl = new WebURL();
    webUrl.setURL("https://www.rambler.ru/data/news/60691/");

    assertThat(testable.shouldVisit(DataSourcesEnum.PARSE, webUrl), is(true));
  }


  public static class TestClass {

    @Bean
    public CrawlerConfiguration crawlerConfiguration() {
      return new CrawlerConfiguration();
    }

    @Bean
    public ContentProcessorImpl contentProcessor() {
      return new ContentProcessorImpl();
    }

    @Bean
    protected IMetricsManager metricsManager() {
      return mock(IMetricsManager.class);
    }

    @Bean
    public IGroovyInterpreter groovyInterpreter() {
      return mock(IGroovyInterpreter.class);
    }

    @Bean
    public IKafkaProducer kafkaProducer() {
      CrawlerConfiguration configuration = new CrawlerConfiguration();
      configuration.setKafkaConnectionUrl(kafkaLocal.getConnectionUrl());
      return KafkaProducerImpl.newInstance(configuration);
    }


  }


  private boolean jsonMatchExpected(String actualJson) {
    String methodName = testName.getMethodName();
    try {
      String expectedJson = IOUtils.resourceToString(CLASSPATH_PREFIX + File.separator + methodName + ".json", Charset.defaultCharset());
      JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT);
    } catch (Exception e) {
      Assertions.fail(e.getMessage(), e);
    }
    return true;
  }

}
