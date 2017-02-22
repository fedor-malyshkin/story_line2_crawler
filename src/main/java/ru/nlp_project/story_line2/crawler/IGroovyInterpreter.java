package ru.nlp_project.story_line2.crawler;

import java.util.Map;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.lang.Binding;


public interface IGroovyInterpreter {
	public static final String EXTR_KEY_IMAGE_URL = "image_url";
	public static final String EXTR_KEY_PUB_DATE = "publication_date";
	public static final String EXTR_KEY_TITLE = "title";
	public static final String EXTR_KEY_CONTENT = "content";

	boolean shouldVisit(String domain, WebURL webURL) throws IllegalStateException;

	/**
	 * Выполнить извлечение данных.
	 * 
	 * При анализе извлекаются следующие данные:
	 * <ul>
	 * <li>"publication_date" - дата публикации в формате java.util.Date</li>
	 * <li>"content" - содежание страницы</li>
	 * <li>"title" - заголовок страницы</li>
	 * <li>"image_url" - сылка на первую картинку</li>
	 * </ul>
	 * 
	 * @param domain источник данных
	 * @param html html контент страницы
	 * @param webURL ссылка на страницу
	 * @return ассоциативный массив или null в случае неверной (не поддерживаемой) страницу
	 * @throws IllegalStateException
	 */
	Map<String, Object> extractData(String domain, WebURL webURL, String html)
			throws IllegalStateException;

	Object executeScript(String script, Binding binding) throws Exception;

}
