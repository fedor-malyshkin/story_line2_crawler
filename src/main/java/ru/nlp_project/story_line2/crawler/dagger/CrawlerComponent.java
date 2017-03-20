package ru.nlp_project.story_line2.crawler.dagger;

import javax.inject.Singleton;

import dagger.Component;
import ru.nlp_project.story_line2.crawler.Crawler;
import ru.nlp_project.story_line2.crawler.feed_site.FeedSiteController;
import ru.nlp_project.story_line2.crawler.feed_site.FeedSiteCrawler;
import ru.nlp_project.story_line2.crawler.impl.ContentProcessorImpl;
import ru.nlp_project.story_line2.crawler.parse_site.ParseSiteController;
import ru.nlp_project.story_line2.crawler.parse_site.ParseSiteCrawler;

@Component(modules = CrawlerModule.class)
@Singleton
public abstract class CrawlerComponent {
	public abstract void inject(Crawler instance);

	public abstract void inject(ParseSiteController instance);

	public abstract void inject(FeedSiteController instance);

	public abstract void inject(ParseSiteCrawler crawler);

	public abstract void inject(FeedSiteCrawler crawler) ;

	public abstract void  inject(ContentProcessorImpl instance) ;	

}
