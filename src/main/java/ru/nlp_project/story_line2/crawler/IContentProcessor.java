package ru.nlp_project.story_line2.crawler;

import edu.uci.ics.crawler4j.url.WebURL;
import java.util.Date;

public interface IContentProcessor {

  /**
   * Провести анализ ссылки на предмет "стоит ли посещать?" (для краулеров сайта). В большинстве
   * своём нужно (особенно если это текстовый контент), т.к. это формирует базу ссылок для
   * последующего анализаю
   */
  boolean shouldVisit(DataSourcesEnum dataSource, WebURL url);

  /**
   * Следует ли обрабатывать?. Тут надо подходить уже значительно более скептически, т.к.
   * большинство ссылок на сайте не содержат данных для анализа.
   */
  boolean shouldProcess(DataSourcesEnum dataSource, WebURL url);

  /**
   * Выполнить обработку HTML содержимого страницы или feed'а.
   *
   * В большинстве свом алгоритм таков:
   * <ol>
   * <li>проверить следует ли обрабатывать ({@link #shouldProcess(WebURL)})</li>
   * <li>проверить может есть такая запись
   * ({@link IMongoDBClient#isCrawlerEntryExists(String, String)})</li>
   * <li>выполнить извлечение данных ({@link #shouldProcess(WebURL)})</li>
   * <li>выполнить сериализацию данных
   * ({@link IGroovyInterpreter#extractRawData(String, WebURL, String)})</li>
   * <li>выполнить запись данных
   * ({@link IMongoDBClient#writeCrawlerEntry(com.mongodb.DBObject, String, String)})</li>
   * </ol>
   *
   * @param webURL ссылка на страницу
   * @param htmlContent HTML содержимое
   */
  void processHtml(DataSourcesEnum dataSource, WebURL webURL, String htmlContent);

  /**
   * Выполнить обработку HTML содержимого страницы или feed'а.
   *
   * В большинстве свом алгоритм таков:
   * <ol>
   * <li>проверить следует ли обрабатывать ({@link #shouldProcess(WebURL)})</li>
   * <li>проверить может есть такая запись
   * ({@link IMongoDBClient#isCrawlerEntryExists(String, String)})</li>
   * <li>выполнить извлечение данных ({@link #shouldProcess(WebURL)})</li>
   * <li>выполнить сериализацию данных
   * ({@link IGroovyInterpreter#extractRawData(String, WebURL, String)})</li>
   * <li>выполнить запись данных
   * ({@link IMongoDBClient#writeCrawlerEntry(com.mongodb.DBObject, String, String)})</li>
   * </ol>
   *
   * @param webURL ссылка на страницу
   * @param htmlContent HTML содержимое
   * @param title возможно заголовок (если не null - не следует замещать результатами анализа)
   * @param publicationDate возможно дата публикации (если не null - не следует замещать
   * результатами анализа)
   * @param imageUrl возможно ссылка на картинку (если не null - не следует замещать результатами
   * анализа)
   */
  void processHtml(DataSourcesEnum dataSource, WebURL webURL, String htmlContent, String title,
      Date publicationDate,
      String imageUrl);

  void initialize(String source);

  enum DataSourcesEnum {
    PARSE, RSS;
  }

}
