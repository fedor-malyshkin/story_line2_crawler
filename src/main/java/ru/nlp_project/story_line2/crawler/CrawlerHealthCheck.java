package ru.nlp_project.story_line2.crawler;

import com.codahale.metrics.health.HealthCheck;

/**
 * Объект для проверки здоровья сервиса (требуется фреймворком dropwizard.io)/
 * 
 * @author fedor
 *
 */
public class CrawlerHealthCheck extends HealthCheck {

	public CrawlerHealthCheck(CrawlerConfiguration configuration) {}

	@Override
	protected Result check() throws Exception {
		return Result.healthy();
	}

}
