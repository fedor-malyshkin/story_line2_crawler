package ru.nlp_project.story_line2.crawler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.ParseSiteConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfigurationTest.TestClass;

// WARN: for unknown reasons '@TestPropertySource' does not work with YAML
@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = TestClass.class)
public class CrawlerConfigurationTest {

  @Autowired
  CrawlerConfiguration testable;

  @BeforeClass
  public static void setUpClass() {
    // WARN: for unknown reasons '@TestPropertySource' does not work with YAML
    System.setProperty("spring.config.location",
                       "src/test/resources/ru/nlp_project/story_line2/crawler/crawler_config.yml");
  }

  @Test
  public void testMainConfig() {
    assertThat(testable.isAsync()).isTrue();
    assertThat(testable.getCrawlerPerSite()).isEqualTo(1);
    assertThat(testable.getParseSites()).hasSize(1);
    assertThat(testable.getFeedSites()).hasSize(1);
    assertThat(testable.getInfluxdbMetrics()).isNotNull();
    assertThat(testable.getCrawlerStorageDir()).isEqualTo("/tmp/crawler");
    assertThat(testable.getKafkaConnectionUrl()).isEqualTo("localhost:9092");
    assertThat(testable.getSkipImagesOlderDays()).isEqualTo(30);
  }

  @Test
  public void testParseSitesConfig() {
    ParseSiteConfiguration siteConfiguration = testable.getParseSites().get(0);
    assertThat(siteConfiguration.getCronSchedule()).isEqualTo("0 0/5 * * * ?");
    assertThat(siteConfiguration.getSeed()).isEqualTo("http://bnkomi.ru");
    assertThat(siteConfiguration.getSource()).isEqualTo("bnkomi.ru");
  }


  @EnableConfigurationProperties(CrawlerConfiguration.class)
  public static class TestClass {

    @Bean
    protected IMetricsManager metricsManager() {
      return mock(IMetricsManager.class);
    }
  }


}
