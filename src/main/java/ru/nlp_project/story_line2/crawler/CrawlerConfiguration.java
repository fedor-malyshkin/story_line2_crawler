package ru.nlp_project.story_line2.crawler;

import java.util.ArrayList;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fedor
 */
@ConfigurationProperties(ignoreUnknownFields = false, prefix = "config")
public class CrawlerConfiguration {

	public boolean async = true;
	public int crawlerPerSite = 4;
	public String crawlerScriptDir;
	public ArrayList<ParseSiteConfiguration> parseSites = new ArrayList<>();
	public ArrayList<FeedSiteConfiguration> feedSites = new ArrayList<>();
	public MetricsConfiguration influxdbMetrics = new MetricsConfiguration();
	public String crawlerStorageDir;
	public String mongodbConnectionUrl;
	public int skipImagesOlderDays = 30;

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("CrawlerConfiguration{");
		sb.append("async=").append(async);
		sb.append(", crawlerPerSite=").append(crawlerPerSite);
		sb.append(", crawlerScriptDir='").append(crawlerScriptDir).append('\'');
		sb.append(", parseSites=").append(parseSites);
		sb.append(", feedSites=").append(feedSites);
		sb.append(", influxdbMetrics=").append(influxdbMetrics);
		sb.append(", crawlerStorageDir='").append(crawlerStorageDir).append('\'');
		sb.append(", mongodbConnectionUrl='").append(mongodbConnectionUrl).append('\'');
		sb.append(", skipImagesOlderDays=").append(skipImagesOlderDays);
		sb.append('}');
		return sb.toString();
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public int getCrawlerPerSite() {
		return crawlerPerSite;
	}

	public void setCrawlerPerSite(int crawlerPerSite) {
		this.crawlerPerSite = crawlerPerSite;
	}

	public String getCrawlerScriptDir() {
		return crawlerScriptDir;
	}

	public void setCrawlerScriptDir(String crawlerScriptDir) {
		this.crawlerScriptDir = crawlerScriptDir;
	}

	public ArrayList<ParseSiteConfiguration> getParseSites() {
		return parseSites;
	}

	public void setParseSites(
			ArrayList<ParseSiteConfiguration> parseSites) {
		this.parseSites = parseSites;
	}

	public ArrayList<FeedSiteConfiguration> getFeedSites() {
		return feedSites;
	}

	public void setFeedSites(
			ArrayList<FeedSiteConfiguration> feedSites) {
		this.feedSites = feedSites;
	}


	public MetricsConfiguration getInfluxdbMetrics() {
		return influxdbMetrics;
	}

	public void setInfluxdbMetrics(
			MetricsConfiguration influxdbMetrics) {
		this.influxdbMetrics = influxdbMetrics;
	}

	public String getCrawlerStorageDir() {
		return crawlerStorageDir;
	}

	public void setCrawlerStorageDir(String crawlerStorageDir) {
		this.crawlerStorageDir = crawlerStorageDir;
	}

	public String getMongodbConnectionUrl() {
		return mongodbConnectionUrl;
	}

	public void setMongodbConnectionUrl(String mongodbConnectionUrl) {
		this.mongodbConnectionUrl = mongodbConnectionUrl;
	}

	public int getSkipImagesOlderDays() {
		return skipImagesOlderDays;
	}

	public void setSkipImagesOlderDays(int skipImagesOlderDays) {
		this.skipImagesOlderDays = skipImagesOlderDays;
	}

	public static class FeedSiteConfiguration {

		public String source;
		public String feed;
		public String cronSchedule;
		public boolean parseForContent;
		public boolean parseForImage;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("FeedSiteConfiguration{");
			sb.append("source='").append(source).append('\'');
			sb.append(", feed='").append(feed).append('\'');
			sb.append(", cronSchedule='").append(cronSchedule).append('\'');
			sb.append(", parseForContent=").append(parseForContent);
			sb.append(", parseForImage=").append(parseForImage);
			sb.append('}');
			return sb.toString();
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getFeed() {
			return feed;
		}

		public void setFeed(String feed) {
			this.feed = feed;
		}

		public String getCronSchedule() {
			return cronSchedule;
		}

		public void setCronSchedule(String cronSchedule) {
			this.cronSchedule = cronSchedule;
		}

		public boolean isParseForContent() {
			return parseForContent;
		}

		public void setParseForContent(boolean parseForContent) {
			this.parseForContent = parseForContent;
		}

		public boolean isParseForImage() {
			return parseForImage;
		}

		public void setParseForImage(boolean parseForImage) {
			this.parseForImage = parseForImage;
		}
	}

	public static class ParseSiteConfiguration {

		public String source;
		public String seed;
		public String cronSchedule;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("ParseSiteConfiguration{");
			sb.append("source='").append(source).append('\'');
			sb.append(", seed='").append(seed).append('\'');
			sb.append(", cronSchedule='").append(cronSchedule).append('\'');
			sb.append('}');
			return sb.toString();
		}

		public String getSeed() {
			return seed;
		}

		public void setSeed(String seed) {
			this.seed = seed;
		}

		public String getCronSchedule() {
			return cronSchedule;
		}

		public void setCronSchedule(String cronSchedule) {
			this.cronSchedule = cronSchedule;
		}

		public String getSource() {

			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}
	}

	public static class MetricsConfiguration {

		public boolean enabled = false;
		public String influxdbHost;
		public int influxdbPort;
		public String influxdbDb;
		public String influxdbUser;
		public String influxdbPassword;
		public int reportingPeriod;
		public int logReportingPeriod;

		public int getLogReportingPeriod() {
			return logReportingPeriod;
		}

		public void setLogReportingPeriod(int logReportingPeriod) {
			this.logReportingPeriod = logReportingPeriod;
		}

		public String getInfluxdbHost() {
			return influxdbHost;
		}

		public void setInfluxdbHost(String influxdbHost) {
			this.influxdbHost = influxdbHost;
		}

		public int getInfluxdbPort() {
			return influxdbPort;
		}

		public void setInfluxdbPort(int influxdbPort) {
			this.influxdbPort = influxdbPort;
		}

		public String getInfluxdbDb() {
			return influxdbDb;
		}

		public void setInfluxdbDb(String influxdbDb) {
			this.influxdbDb = influxdbDb;
		}

		public String getInfluxdbUser() {
			return influxdbUser;
		}

		public void setInfluxdbUser(String influxdbUser) {
			this.influxdbUser = influxdbUser;
		}

		public String getInfluxdbPassword() {
			return influxdbPassword;
		}

		public void setInfluxdbPassword(String influxdbPassword) {
			this.influxdbPassword = influxdbPassword;
		}

		public int getReportingPeriod() {
			return reportingPeriod;
		}

		public void setReportingPeriod(int reportingPeriod) {
			this.reportingPeriod = reportingPeriod;
		}

		public boolean isEnabled() {

			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("MetricsConfiguration{");
			sb.append("enabled=").append(enabled);
			sb.append(", influxdbHost='").append(influxdbHost).append('\'');
			sb.append(", influxdbPort=").append(influxdbPort);
			sb.append(", influxdbDb='").append(influxdbDb).append('\'');
			sb.append(", influxdbUser='").append(influxdbUser).append('\'');
			sb.append(", influxdbPassword='").append(influxdbPassword).append('\'');
			sb.append(", reportingPeriod=").append(reportingPeriod);
			sb.append(", logReportingPeriod=").append(logReportingPeriod);
			sb.append('}');
			return sb.toString();
		}
	}
}
