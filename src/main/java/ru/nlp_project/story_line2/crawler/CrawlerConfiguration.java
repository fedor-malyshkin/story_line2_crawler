package ru.nlp_project.story_line2.crawler;

import java.time.ZoneId;
import java.util.ArrayList;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import ru.nlp_project.story_line2.crawler.utils.DateTimeUtils;

/**
 * Объект-конфигурация (требуется фреймворком dropwizard.io)
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

		@NotEmpty
		@JsonProperty(value = "parse_for_content")
		public boolean parseForContent;

		@NotEmpty
		@JsonProperty(value = "parse_for_image")
		public boolean parseForImage;

		@JsonProperty(value = "zone_id")
		public void setZoneId(String text) {
			zoneId = DateTimeUtils.converToZoneId(text);
		}

		@JsonIgnore
		public ZoneId zoneId = ZoneId.systemDefault();
	}


	public static class ParseSiteConfiguration {
		@NotEmpty
		@JsonProperty(value = "source")
		public String source;

		@NotEmpty
		@JsonProperty(value = "seed")
		public String seed;

		@NotEmpty
		@JsonProperty(value = "cron_schedule")
		public String cronSchedule;

		@JsonProperty(value = "zone_id")
		public void setZoneId(String text) {
			zoneId = DateTimeUtils.converToZoneId(text);
		}

		@JsonIgnore
		public ZoneId zoneId = ZoneId.systemDefault();
	}

	public static class MetricsConfiguration {
		// enabled: true
		@NotEmpty
		@JsonProperty(value = "enabled")
		public boolean enabled = false;

		// influxdb_host: ""
		@JsonProperty(value = "influxdb_host")
		public String influxdbHost;


		// influxdb_port: ""
		@JsonProperty(value = "influxdb_port")
		public int influxdbPort;

		// influxdb_db: ""
		@JsonProperty(value = "influxdb_db")
		public String influxdbDB;

		// influxdb_user: ""
		@JsonProperty(value = "influxdb_user")
		public String influxdbUser;

		// influxdb_password: ""
		@JsonProperty(value = "influxdb_password")
		public String influxdbPassword;

		// reporting_period: 30
		@NotEmpty
		@JsonProperty(value = "reporting_period")
		public int reportingPeriod;
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

	@JsonProperty(value = "influxdb_metrics")
	public MetricsConfiguration metrics = new MetricsConfiguration();

	@NotEmpty
	@JsonProperty(value = "crawler_storage_dir")
	public String storageDir;
	@NotEmpty
	@JsonProperty(value = "mongodb_connection_url")
	public String connectionUrl;

	@JsonProperty(value = "skip_images_older_days")
	public int skipImagesOlderDays = 30;
}
