package ru.nlp_project.story_line2.crawler;

import ru.nlp_project.story_line2.crawler.IContentProcessor.DataSourcesEnum;

public interface IMetricsManager {

  public static final String IN_APP_PREFIX = "in_app";
  public static final String SERVICE = "crawler";
  public static final String METRIC_NAME_PAGE_PROCESSED = "pages_processed";
  public static final String METRIC_NAME_PAGE_EMPTY = "pages_empty";
  public static final String METRIC_NAME_PAGE_FULL = "pages_full";
  public static final String METRIC_NAME_EXTRACTED_EMPTY_PUB_DATE = "extracted_empty_pub_dates";
  public static final String METRIC_NAME_EXTRACTED_EMPTY_CONTENT = "extracted_empty_contents";
  public static final String METRIC_NAME_EXTRACTED_EMPTY_TITLE = "extracted_empty_titles";
  public static final String METRIC_NAME_EXTRACTED_EMPTY_IMAGE_URL = "extracted_empty_image_urls";
  public static final String METRIC_NAME_LINK_PROCESSED = "links_processed";

  void initialize();

  void shutdown();

  void incrementPagesProcessed(DataSourcesEnum dataSource, String sourceName);

  void incrementPagesEmpty(DataSourcesEnum dataSource, String sourceName);

  void incrementPagesFull(DataSourcesEnum dataSource, String sourceName);

  void incrementExtractionEmptyPubDate(DataSourcesEnum dataSource, String sourceName);

  void incrementExtractionEmptyTitle(DataSourcesEnum dataSource, String sourceName);

  void incrementExtractionEmptyContent(DataSourcesEnum dataSource, String sourceName);

  void incrementExtractionEmptyImageUrl(DataSourcesEnum dataSource, String sourceName);

  void incrementLinkProcessed(DataSourcesEnum dataSource, String sourceName);
}
