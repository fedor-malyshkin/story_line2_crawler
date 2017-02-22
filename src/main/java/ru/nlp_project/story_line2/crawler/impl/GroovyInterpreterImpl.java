package ru.nlp_project.story_line2.crawler.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;

/**
 * Интерпретатор groovy. Для скриптов, анализирующих html-страницы источников.
 * 
 * @author fedor
 *
 */
public class GroovyInterpreterImpl implements IGroovyInterpreter {

	private static final String GROOVY_EXT_NAME = "groovy";
	private static final String SCRIPT_DOMAIN_STATIC_FILED = "domain";
	private static final String SCRIPT_SHOULD_VISIT_METHOD_NAME = "shouldVisit";
	private static final String SCRIPT_EXTRACT_DATA_METHOD_NAME = "extractData";
	private GroovyScriptEngine scriptEngine;
	private HashMap<String, Class<?>> domainMap;
	private Logger logger;

	private GroovyInterpreterImpl() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	public static GroovyInterpreterImpl newInstance(CrawlerConfiguration configuration)
			throws IllegalStateException {
		GroovyInterpreterImpl result = new GroovyInterpreterImpl();
		result.initialize(configuration);
		return result;
	}

	private void initialize(CrawlerConfiguration configuration) throws IllegalStateException {
		domainMap = new HashMap<String, Class<?>>();
		try {
			scriptEngine = new GroovyScriptEngine(configuration.scriptDir);
			Collection<File> files = FileUtils.listFiles(new File(configuration.scriptDir),
					new String[] {GROOVY_EXT_NAME}, false);
			for (File file : files) {
				String name = file.getName();
				Class<?> scriptClass = scriptEngine.loadScriptByName(name);
				Field field = scriptClass.getField(SCRIPT_DOMAIN_STATIC_FILED);
				String domain = (String) field.get(null);
				domainMap.put(domain.toLowerCase(), scriptClass);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#shouldVisit(java.lang.String,
	 * edu.uci.ics.crawler4j.url.WebURL)
	 */
	@Override
	public boolean shouldVisit(String domain, WebURL webURL) throws IllegalStateException {
		// важная отсечка сайтов из других доменов!!!
		if (!domainMap.containsKey(domain.toLowerCase()))
			return false;

		Class<?> class1 = domainMap.get(domain.toLowerCase());
		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_SHOULD_VISIT_METHOD_NAME, Object.class);
			Boolean result = (Boolean) method.invoke(instance, webURL);

			return result.booleanValue();
		} catch (Exception e) {
			logger.error("Exception while processing 'shouldVisit' ({}, {})", domain,
					webURL.getPath(), e);
			throw new IllegalStateException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#extractData(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> extractData(String domain, WebURL webURL, String html)
			throws IllegalStateException {
		if (!domainMap.containsKey(domain.toLowerCase())) {
			logger.error("No script for domain: '{}'", domain);
			throw new IllegalArgumentException("No script for domain: " + domain);
		}

		Class<?> class1 = domainMap.get(domain.toLowerCase());

		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_EXTRACT_DATA_METHOD_NAME, Object.class,
					Object.class, Object.class);
			Map<String, Object> result =
					(Map<String, Object>) method.invoke(instance, domain, webURL, html);
			return result;
		} catch (Exception e) {
			logger.error("Exception while processing {}:{}", domain, webURL.getPath(), e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Object executeScript(String script, Binding binding) throws Exception {
		GroovyShell shell = new GroovyShell(scriptEngine.getGroovyClassLoader(), binding);
		return shell.evaluate(script);
	}

	
	



}
