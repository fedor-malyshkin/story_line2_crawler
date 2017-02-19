package ru.nlp_project.story_line2.crawler.dagger;

import com.codahale.metrics.MetricRegistry;

import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;

public class CrawlerBuilder {
	private static CrawlerComponent component;
	private static CrawlerConfiguration crawlerConfiguration;


	public static CrawlerComponent getComponent() {
		if (component == null) {
			MetricRegistry metricRegistry = new MetricRegistry();
			CrawlerModule module = new CrawlerModule(crawlerConfiguration, metricRegistry);
			component = DaggerCrawlerComponent.builder().crawlerModule(module).build();
		}
		return component;
	}

	public static void setCrawlerConfiguration(CrawlerConfiguration configuration) {
		crawlerConfiguration = configuration;
	}
}
