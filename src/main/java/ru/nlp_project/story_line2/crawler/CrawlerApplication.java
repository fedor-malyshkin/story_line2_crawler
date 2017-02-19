package ru.nlp_project.story_line2.crawler;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Объект-приложение (требуется фреймворком dropwizard.io)/
 * 
 * 
 * @author fedor
 *
 */
public class CrawlerApplication extends Application<CrawlerConfiguration> {
	public static void main(String[] args) throws Exception {
		new CrawlerApplication().run(args);
	}

	@Override
	public String getName() {
		return "crawler";
	}

	@Override
	public void initialize(Bootstrap<CrawlerConfiguration> bootstrap) {}

	@Override
	public void run(CrawlerConfiguration configuration, Environment environment) throws Exception {
		final CrawlerHealthCheck healthCheck = new CrawlerHealthCheck(configuration);

		environment.healthChecks().register("crawler", healthCheck);
		Crawler crawler = Crawler.newInstance(configuration);
		environment.lifecycle().manage(crawler);
		
		

	}

}

