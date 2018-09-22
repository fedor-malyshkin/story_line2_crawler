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
import java.io.IOException;
import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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
// @SpringBootTest()
@ContextConfiguration(classes = TestClass.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ContentProcessorImplTest {

  private static KafkaLocal kafkaLocal;
  private static ZooKeeperLocal zookeeperLocal;

  @Autowired
  private ContentProcessorImpl testable;

  @Autowired
  private IKafkaProducer kafkaProducer;

  @Autowired
  private IGroovyInterpreter groovyInterpreter;

  private WebURL webUrl;


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
    kafkaLocal.deleteAllTopics();
  }


  @Before
  public void setUp() {
    webUrl = new WebURL();
    webUrl.setURL("https://www.bnkomi.ru/data/news/60691/");
    testable.initialize("test.source");
    kafkaLocal.createTopic(IKafkaProducer.DEFAULT_KAFKA_SERIALIZER);
  }


  @Test
  public void testProcessHtmlFromFeed() throws IOException, InterruptedException {
    Thread.sleep(10*1_000);
    HashMap<String, Object> extrData = new HashMap<>();
    extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
    extrData.put(EXTR_KEY_CONTENT, "test_content");

    when(groovyInterpreter.extractRawData(eq("test.source"), any(), anyString()))
        .thenReturn("some text");
    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "", "some", null, "Some");

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
    Thread.sleep(10*1_000);
    kafkaLocal.getMessageFromTopic(IKafkaProducer.PRODUCER_TOPIC, true);


  }

  @Ignore
  @Test
  public void testProcessHtmlFromParser() throws IOException {
    when(groovyInterpreter.extractRawData(anyString(), any(), anyString())).thenReturn("some");
    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "");

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());

    verify(groovyInterpreter).extractRawData(eq("test.source"), any(), anyString());

  }


  @Ignore
  @Test
  public void testProcessHtml_OldPublicationDate_NoImageLoading() throws IOException {

    when(groovyInterpreter.extractRawData(eq("test.source"), any(), anyString()))
        .thenReturn("some text");

    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "", null, null, null);

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
  }

  @Ignore
  @Test
  public void testProcessHtml_ShouldNotProcess() throws IOException {
    HashMap<String, Object> extrData = new HashMap<>();
    extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
    extrData.put(EXTR_KEY_CONTENT, "test_contnt");

    when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
    when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(false);

    testable.processHtml(DataSourcesEnum.PARSE, webUrl, "", null, null, null);

    verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
  }
  @Ignore
  @Test
  public void testShouldVisitWrongDomain() {
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

}
