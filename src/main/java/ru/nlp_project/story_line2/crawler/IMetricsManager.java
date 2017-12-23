package ru.nlp_project.story_line2.crawler;

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

	void incrementPagesProcessed(String sourceName);

	void incrementPagesEmpty(String sourceName);

	void incrementPagesFull(String sourceName);

	void incrementExtractionEmptyPubDate(String sourceName);

	void incrementExtractionEmptyTitle(String sourceName);

	void incrementExtractionEmptyContent(String sourceName);

	void incrementExtractionEmptyImageUrl(String sourceName);

	void incrementLinkProcessed(String sourceName);
}
