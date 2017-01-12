package ru.nlp_project.story_line2.crawler;

import java.util.Map;

import edu.uci.ics.crawler4j.url.WebURL;


public interface IGroovyInterpreter {

	boolean shouldVisit(String domain, WebURL webURL) throws IllegalStateException;

	Map<String, Object> extractData(String domain, String html) throws IllegalStateException;

}
