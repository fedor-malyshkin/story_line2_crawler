package ru.nlp_project.story_line2.crawler;

import static org.mockito.Mockito.*;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.nlp_project.story_line2.crawler.CrawlerConfigurationTest.TestClass;

// WARN: for unknown reasons '@TestPropertySource' does not work with YAML
@RunWith(SpringRunner.class)
//@TestPropertySource("classpath:ru/nlp_project/story_line2/server_web/test_server_web_config.yml")
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
	public void testToString() {
		Assertions.assertThat(testable.toString()).isEqualToIgnoringCase(
				"CrawlerConfiguration{async=true, crawlerPerSite=1, crawlerScriptDir='/tmp/crawler/scripts', "
						+ "parseSites=["
						+ "ParseSiteConfiguration{source='bnkomi.ru', seed='http://bnkomi.ru', cronSchedule='0 0/5 * * * ?'}"
						+ "], feedSites=["
						+ "FeedSiteConfiguration{source='komiinform.ru', feed='http://komiinform.ru/rss/news/', cronSchedule='0 0/1 * * * ?', parseForContent=false, parseForImage=false}"
						+ "], "
						+ "influxdbMetrics=MetricsConfiguration{enabled=false, influxdbHost='ci.nlp-project.ru', influxdbPort=8086, influxdbDb='storyline', influxdbUser='', influxdbPassword='', reportingPeriod=30, logReportingPeriod=30}, "
						+ "crawlerStorageDir='/tmp/crawler', mongodbConnectionUrl='mongodb://localhost:27017/', skipImagesOlderDays=30}");
	}


	@EnableConfigurationProperties(CrawlerConfiguration.class)
	public static class TestClass {

		@Bean
		protected IMetricsManager metricsManager() {
			return mock(IMetricsManager.class);
		}
	}


}
