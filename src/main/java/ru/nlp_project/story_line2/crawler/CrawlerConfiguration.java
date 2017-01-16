package ru.nlp_project.story_line2.crawler;

import java.util.ArrayList;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

/**
 * Объект-конфигурация (требуется фреймворком dropwizard.io)/
 * 
 * @author fedor
 *
 */
public class CrawlerConfiguration extends Configuration {
	static class SiteConfiguration {
		@NotEmpty
		@JsonProperty(value = "domain")
		public String domain;

		@NotEmpty
		@JsonProperty(value = "seed")
		public String seed;
	}

	@JsonProperty(value = "async")
	public boolean async = true;
	@JsonProperty(value = "crawler_per_site")
	public int crawlerPerSite = 4;
	@NotEmpty
	@JsonProperty(value = "script_dir")
	public String scriptDir;
	@JsonProperty(value = "sites")
	public ArrayList<SiteConfiguration> sites = new ArrayList<>();
	@NotEmpty
	@JsonProperty(value = "storage_dir")
	public String storageDir;
	@NotEmpty
	@JsonProperty(value = "connection_url")
	public String connectionUrl;
}
