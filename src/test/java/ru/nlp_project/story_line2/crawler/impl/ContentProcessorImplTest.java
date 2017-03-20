package ru.nlp_project.story_line2.crawler.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_CONTENT;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_IMAGE_URL;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_PUB_DATE;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;

import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IImageLoader;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;

public class ContentProcessorImplTest {

	private ContentProcessorImpl testable;
	private IMongoDBClient mongoDBClient;
	private IGroovyInterpreter groovyInterpreter;
	private IImageLoader imageLoader;
	private WebURL webUrl;
	private CrawlerConfiguration configuration;

	@Before
	public void setUp() {
		webUrl = new WebURL();
		webUrl.setURL("https://www.bnkomi.ru/data/news/60691/");
		imageLoader = mock(IImageLoader.class);
		mongoDBClient = mock(IMongoDBClient.class);
		groovyInterpreter = mock(IGroovyInterpreter.class);
		configuration  = new CrawlerConfiguration();
		

		
		testable = new ContentProcessorImpl();
		testable.crawlerConfiguration = configuration;
		testable.dbClientManager = mongoDBClient;
		testable.groovyInterpreter = groovyInterpreter;
		testable.imageLoader = imageLoader;
		testable.metricRegistry = new MetricRegistry();
		testable.initialize("test.source");
	}


	@Test
	public void testProcessHtml() throws IOException {
		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
		extrData.put(EXTR_KEY_CONTENT, "test_contnt");

		when(imageLoader.loadImage(anyString())).thenReturn(new byte[] {});
		when(mongoDBClient.isNewsExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
		when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

		testable.processHtml(webUrl, "", null, null, null);

		verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
		verify(mongoDBClient).isNewsExists(eq("test.source"), anyString());
		verify(groovyInterpreter).extractData(eq("test.source"), any(), anyString());
		verify(imageLoader).loadImage(anyString());
		verify(mongoDBClient).writeNews(any(), eq("test.source"), anyString());
	}


	@Test
	public void testProcessHtml_OldPublicationDate_NoImageLoading() throws IOException {
		Instant now = Instant.now();
		Instant pubDate = now.minus(configuration.skipImagesOlderDays+1, ChronoUnit.DAYS);
		java.util.Date obsPubDate = Date.from(pubDate);
		
		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_PUB_DATE, obsPubDate);
		extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
		extrData.put(EXTR_KEY_CONTENT, "test_contnt");

		when(mongoDBClient.isNewsExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
		when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

		testable.processHtml(webUrl, "", null, null, null);

		verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
		verify(mongoDBClient).isNewsExists(eq("test.source"), anyString());
		verify(groovyInterpreter).extractData(eq("test.source"), any(), anyString());
		verify(mongoDBClient).writeNews(any(), eq("test.source"), anyString());
		verifyNoMoreInteractions(imageLoader);
	}

	@Test
	public void testProcessHtml_ShouldNotProcess() throws IOException {
		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
		extrData.put(EXTR_KEY_CONTENT, "test_contnt");

		when(imageLoader.loadImage(anyString())).thenReturn(new byte[] {});
		when(mongoDBClient.isNewsExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractData(anyString(), any(), anyString())).thenReturn(extrData);
		when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(false);

		testable.processHtml(webUrl, "", null, null, null);

		verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
	}

	@Test
	public void testShouldVisitWrongDomain() {
		when(groovyInterpreter.shouldVisit(anyString(), any())).thenReturn(true);
		testable.initialize("rambler.ru");

		webUrl = new WebURL();
		webUrl.setURL("https://www.bnkomi.ru/data/news/60691/");

		assertThat(testable.shouldVisit(webUrl), is(false));

		webUrl = new WebURL();
		webUrl.setURL("https://www.rambler.ru/data/news/60691/");

		assertThat(testable.shouldVisit(webUrl), is(true));

	}

}
