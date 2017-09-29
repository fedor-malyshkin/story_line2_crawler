package ru.nlp_project.story_line2.crawler.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_CONTENT;
import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.EXTR_KEY_IMAGE_URL;

import com.codahale.metrics.MetricRegistry;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;
import ru.nlp_project.story_line2.crawler.IMongoDBClient;

public class ContentProcessorImplTest {

	private ContentProcessorImpl testable;
	private IMongoDBClient mongoDBClient;
	private IGroovyInterpreter groovyInterpreter;
	private WebURL webUrl;
	private CrawlerConfiguration configuration;

	@Before
	public void setUp() {
		webUrl = new WebURL();
		webUrl.setURL("https://www.bnkomi.ru/data/news/60691/");
		mongoDBClient = mock(IMongoDBClient.class);
		groovyInterpreter = mock(IGroovyInterpreter.class);
		configuration = new CrawlerConfiguration();

		testable = new ContentProcessorImpl();
		testable.crawlerConfiguration = configuration;
		testable.dbClientManager = mongoDBClient;
		testable.groovyInterpreter = groovyInterpreter;
		testable.metricRegistry = new MetricRegistry();
		testable.initialize("test.source");
	}


	@Test
	public void testProcessHtmlFromFeed() throws IOException {
		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
		extrData.put(EXTR_KEY_CONTENT, "test_content");

		when(mongoDBClient.isCrawlerEntryExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractRawData(eq("test.source"), any(), anyString()))
				.thenReturn("some text");
		when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

		testable.processHtml(webUrl, "", "some", null, "Some");

		verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
		verify(mongoDBClient).isCrawlerEntryExists(eq("test.source"), anyString());
		verify(mongoDBClient).writeCrawlerEntry(any(), eq("test.source"), anyString());
	}


	@Test
	public void testProcessHtmlFromParser() throws IOException {
		when(mongoDBClient.isCrawlerEntryExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.extractRawData(anyString(), any(), anyString())).thenReturn("some");
		when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

		testable.processHtml(webUrl, "");

		verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
		verify(mongoDBClient).isCrawlerEntryExists(eq("test.source"), anyString());
		verify(groovyInterpreter).extractRawData(eq("test.source"), any(), anyString());
		verify(mongoDBClient).writeCrawlerEntry(any(), eq("test.source"), anyString());
	}


	@Test
	public void testProcessHtml_OldPublicationDate_NoImageLoading() throws IOException {

		when(groovyInterpreter.extractRawData(eq("test.source"), any(), anyString()))
				.thenReturn("some text");
		when(mongoDBClient.isCrawlerEntryExists(anyString(), anyString())).thenReturn(false);
		when(groovyInterpreter.shouldProcess(anyString(), any())).thenReturn(true);

		testable.processHtml(webUrl, "", null, null, null);

		verify(groovyInterpreter).shouldProcess(eq("test.source"), any());
		verify(mongoDBClient, atLeastOnce()).isCrawlerEntryExists(eq("test.source"), anyString());
		verify(mongoDBClient, atLeastOnce()).writeCrawlerEntry(any(), eq("test.source"), anyString());
	}

	@Test
	public void testProcessHtml_ShouldNotProcess() throws IOException {
		HashMap<String, Object> extrData = new HashMap<>();
		extrData.put(EXTR_KEY_IMAGE_URL, "image_url");
		extrData.put(EXTR_KEY_CONTENT, "test_contnt");

		when(mongoDBClient.isCrawlerEntryExists(anyString(), anyString())).thenReturn(false);
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
