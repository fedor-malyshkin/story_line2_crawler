package ru.nlp_project.story_line2.crawler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrawlerNewsArticle {
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

  @JsonProperty("raw_content")
  String rawContent;

  @JsonProperty("raw_content_size")
  long rawContentSize;


  public CrawlerNewsArticle() {
    super();
  }

  public CrawlerNewsArticle(String source, String path, String url, Date publicationDate,
      Date processingDate, String title, String imageUrl, String rawContent) {
    super();
    this.publicationDate = publicationDate;
    this.processingDate = processingDate;
    this.path = path;
    this.source = source;
    this.title = title;
    this.url = url;
    this.imageUrl = imageUrl;
    this.rawContent = rawContent;
    this.rawContentSize = rawContent.getBytes().length;
  }
}
