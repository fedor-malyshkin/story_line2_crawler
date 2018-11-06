package ru.nlp_project.story_line2.crawler.feed_site;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.IContentProcessor;
import ru.nlp_project.story_line2.crawler.IDatabaseProcessor;

public class FeedSiteCrawlerTest {

  private IDatabaseProcessor databaseProcessor;
  private FeedSiteCrawler testable;
  private IContentProcessor contentProcessor;
  private CrawlerConfiguration crawlerConfiguration;
  private FeedSiteConfiguration siteConfig;

  @Before
  public void setUp() {
    databaseProcessor = Mockito.mock(IDatabaseProcessor.class);
    contentProcessor = Mockito.mock(IContentProcessor.class);
    siteConfig = new FeedSiteConfiguration();
    crawlerConfiguration = new CrawlerConfiguration();
    testable = new FeedSiteCrawler(crawlerConfiguration, siteConfig, databaseProcessor, contentProcessor);
  }

  @Test
  public void testParseNewRecord() throws Exception {
    Mockito.when(databaseProcessor.contains(Mockito.any())).thenReturn(false);
    testable.parseFeed(getTestableRss());
    Mockito.verify(contentProcessor, Mockito.times(20)).processHtml(Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any());
    Mockito.verify(databaseProcessor, Mockito.times(20)).add(Mockito.any());
  }


  @Test
  public void testParseNewRecordExceptExisting() throws Exception {
    Mockito.when(databaseProcessor.contains(Mockito.any())).thenReturn(false);
    Mockito.when(databaseProcessor.contains(Mockito.eq("https://komiinform.ru/news/144543/"))).thenReturn(true);
    testable.parseFeed(getTestableRss());
    Mockito.verify(contentProcessor, Mockito.times(19)).processHtml(Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any(),
                                                                    Mockito.any());
    Mockito.verify(databaseProcessor, Mockito.times(19)).add(Mockito.any());
  }


  private String getTestableRss() throws IOException {
    InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ru/nlp_project/story_line2/crawler/feed_site/komiinform.rss");
    return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
  }
}