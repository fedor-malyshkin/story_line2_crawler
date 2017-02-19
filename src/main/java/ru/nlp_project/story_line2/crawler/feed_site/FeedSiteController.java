package ru.nlp_project.story_line2.crawler.feed_site;

import javax.inject.Inject;

import com.codahale.metrics.MetricRegistry;

import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration.FeedSiteConfiguration;
import ru.nlp_project.story_line2.crawler.dagger.CrawlerBuilder;

public class FeedSiteController {
	@Inject
	MetricRegistry metricRegistry;

	@Inject
	CrawlerConfiguration crawlerConfiguration;


	private FeedSiteConfiguration siteConfig;

	public FeedSiteController(FeedSiteConfiguration siteConfig) {
		this.siteConfig = siteConfig;
		CrawlerBuilder.getComponent().inject(this);
	}

	public void initialize() {}

	public void start() {}

	public void stop() {}


}
