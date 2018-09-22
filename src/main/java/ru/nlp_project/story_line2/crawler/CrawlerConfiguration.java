package ru.nlp_project.story_line2.crawler;

import java.util.ArrayList;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fedor
 */
@ConfigurationProperties(ignoreUnknownFields = false, prefix = "config")
@Data
public class CrawlerConfiguration {

  public boolean async = true;
  public int crawlerPerSite = 4;
  public String crawlerScriptDir;
  public ArrayList<ParseSiteConfiguration> parseSites = new ArrayList<>();
  public ArrayList<FeedSiteConfiguration> feedSites = new ArrayList<>();
  public MetricsConfiguration influxdbMetrics = new MetricsConfiguration();
  public String crawlerStorageDir;
  public String kafkaConnectionUrl;
  public int skipImagesOlderDays = 30;

  @Data
  public static class FeedSiteConfiguration {

    public String source;
    public String feed;
    public String cronSchedule;
    public boolean parseForContent;
    public boolean parseForImage;
  }

  @Data
  public static class ParseSiteConfiguration {

    public String source;
    public String seed;
    public String cronSchedule;

  }

  @Data
  public static class MetricsConfiguration {

    public boolean enabled = false;
    public String influxdbHost;
    public int influxdbPort;
    public String influxdbDb;
    public String influxdbUser;
    public String influxdbPassword;
    public int reportingPeriod;
    public int logReportingPeriod;

  }
}
