package ru.nlp_project.story_line2.crawler;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.undercouch.bson4jackson.types.ObjectId;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrawlerNewsArticle {

	public CrawlerNewsArticle() {
		super();
	}

	public CrawlerNewsArticle(Date creationDate, Date date, String content, String path,
			String domain, String title, String url) {
		super();
		this.creationDate = creationDate;
		this.date = date;
		this.content = content;
		this.path = path;
		this.domain = domain;
		this.title = title;
		this.url = url;
	}

	@JsonProperty("_id")
	ObjectId _id;
	/**
	 * фактическая дата записис в БД
	 */
	@JsonProperty("creation_date")
	Date creationDate = new Date();
	/**
	 * дата новости
	 */
	@JsonProperty("date")
	Date date;
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
	@JsonProperty("domain")
	String domain;
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
}
