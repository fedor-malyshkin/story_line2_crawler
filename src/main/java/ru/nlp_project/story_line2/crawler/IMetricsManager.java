package ru.nlp_project.story_line2.crawler;

public interface IMetricsManager {


	public static final String METHOD_LIST_HEADERS = "list_headers";
	public static final String METHOD_GET_NEWS_ARTICLE = "get_news_article";
	public static final String IN_APP_PREFIX = "in_app";
	public static final String METHOD_LIST_SOURCES = "list_sources";
	public static final String NO_SOURCE = "-";
	public static final String METHOD_GET_NEWS_ARTICLE_IMAGE = "get_news_article_image";

	void initialize();

	void shutdown();

	void incrementInvocation(String method, String source);

	void durationInvocation(String method, String source, long duration);

	void incrementPagesProcessed(String sourceName);

	void incrementPagesEmpty(String sourceName);

	void incrementPagesFull(String sourceName);

	void incrementExtractionEmptyPubDate(String sourceName);

	void incrementExtractionEmptyTitle(String sourceName);

	void incrementExtractionEmptyContent(String sourceName);

	void incrementExtractionEmptyImageUrl(String sourceName);

	void incrementLinkProcessed(String sourceName);
}
