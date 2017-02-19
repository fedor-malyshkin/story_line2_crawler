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
	public static class FeedSiteConfiguration {
		@NotEmpty
		@JsonProperty(value = "source")
		public String source;


		@NotEmpty
		@JsonProperty(value = "feed")
		public String feed;

		@NotEmpty
		@JsonProperty(value = "cron_schedule")
		public String cronSchedule;
	}


	public static class ParseSiteConfiguration {
		@NotEmpty
		@JsonProperty(value = "source")
		public String source;

		@NotEmpty
		@JsonProperty(value = "seed")
		public String seed;
	}

	@JsonProperty(value = "async")
	public boolean async = true;
	@JsonProperty(value = "crawler_per_site")
	public int crawlerPerSite = 4;
	@NotEmpty
	@JsonProperty(value = "crawler_script_dir")
	public String scriptDir;
	@JsonProperty(value = "parse_sites")
	public ArrayList<ParseSiteConfiguration> parseSites = new ArrayList<>();
	@JsonProperty(value = "feed_sites")
	public ArrayList<FeedSiteConfiguration> feedSites = new ArrayList<>();

	@NotEmpty
	@JsonProperty(value = "crawler_storage_dir")
	public String storageDir;
	@NotEmpty
	@JsonProperty(value = "mongodb_connection_url")
	public String connectionUrl;
}
