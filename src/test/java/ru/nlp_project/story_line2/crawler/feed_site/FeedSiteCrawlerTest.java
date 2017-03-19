package ru.nlp_project.story_line2.crawler.feed_site;

import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_CONTENT;

import java.io.FileInputStream;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import static org.mockito.Mockito.*;

import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IImageLoader;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;

public class FeedSiteCrawlerTest {

	private FeedSiteCrawler testable;
	private FeedSiteConfiguration siteConfiguration;
	private IMongoDBClient mongoDBClient;
	private IGroovyInterpreter groovyInterpreter;
	private IImageLoader imageLoader;

	@Before
	public void setUp() {
		imageLoader = mock(IImageLoader.class);
		mongoDBClient = mock(IMongoDBClient.class);
		groovyInterpreter = mock(IGroovyInterpreter.class);
		siteConfiguration = new FeedSiteConfiguration();
		siteConfiguration.source = "test.source";
		testable = new FeedSiteCrawler(siteConfiguration);
		testable.dbClientManager = mongoDBClient;
		testable.groovyInterpreter = groovyInterpreter;
		testable.imageLoader = imageLoader;
		testable.metricRegistry = new MetricRegistry();
		testable.initializeMetrics();
	}

	@Test
	public void test_parseFeed_NoParseForContent_NoParseForImage() throws Exception {
		FileInputStream inputStream = new FileInputStream(
				"src/test/resources/ru/nlp_project/story_line2/crawler/feed_site/komiinform.rss");
		String feed = IOUtils.toString(inputStream);
		siteConfiguration.parseForContent = false;
		siteConfiguration.parseForImage = false;

		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_CONTENT, "test_contnt");

		when(imageLoader.loadImage(anyString())).thenReturn(new byte[] {});
		when(mongoDBClient.isNewsExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
		when(groovyInterpreter.shouldVisit(anyString(), any())).thenReturn(true);

		testable.parseFeed(feed);

		verify(mongoDBClient, times(20)).writeNews(any(), any(), any());
	}

	/**
	 * |Проверка случая, когда в базе парсера набрано уже много ссылок не нужных и откорректированы
	 * скрипты, которые говорят, что их посещать не надо.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_parseFeed_WithShouldVisitReturnsFalse() throws Exception {
		FileInputStream inputStream = new FileInputStream(
				"src/test/resources/ru/nlp_project/story_line2/crawler/feed_site/komiinform.rss");
		String feed = IOUtils.toString(inputStream);
		siteConfiguration.parseForContent = false;
		siteConfiguration.parseForImage = false;

		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_CONTENT, "test_contnt");

		when(imageLoader.loadImage(anyString())).thenReturn(new byte[] {});
		when(mongoDBClient.isNewsExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
		when(groovyInterpreter.shouldVisit(anyString(), any())).thenReturn(false);

		testable.parseFeed(feed);

		verify(mongoDBClient, never()).writeNews(any(), any(), any());
	}

}
