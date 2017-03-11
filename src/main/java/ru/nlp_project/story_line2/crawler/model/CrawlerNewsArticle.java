package ru.nlp_project.story_line2.crawler.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrawlerNewsArticle {

	public CrawlerNewsArticle() {
		super();
	}

	public CrawlerNewsArticle(String source, String path, String url, Date publicationDate,
			Date processingDate, String title, String content, String imageUrl, byte[] imageData) {
		super();
		this.publicationDate = publicationDate;
		this.processingDate = processingDate;
		this.content = content;
		this.path = path;
		this.source = source;
		this.title = title;
		this.url = url;
		this.imageUrl = imageUrl;
		this.imageData = imageData;
	}

	@JsonProperty("_id")
	Id _id;
	/**
	 * фактическая дата записис в БД
	 */
	@JsonProperty("processing_date")
	Date processingDate = new Date();
	/**
	 * дата новости
	 */
	@JsonProperty("publication_date")
	Date publicationDate;
	/**
	 * текст новости/статьи
	 */
	@JsonProperty("content")
	String content;
	/**
	 * путь внутри сайта
	 */
	@JsonProperty("path")
	String path;
	/**
	 * домен сайта (без протокола)
	 */
	@JsonProperty("source")
	String source;
	/**
	 * заголовок новости/статьи
	 */
	@JsonProperty("title")
	String title;
	/**
	 * полный адрес статьи
	 */
	@JsonProperty("url")
	String url;

	/**
	 * ссылка на страницу
	 */
	@JsonProperty("image_url")
	String imageUrl;

	@JsonProperty("image_data")
	byte[] imageData;
}
