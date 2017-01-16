package ru.nlp_project.story_line2.crawler.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
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
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
				| IllegalAccessException | IOException | ResourceException | ScriptException e) {
			throw new IllegalStateException(e);
		}
	}


	/* (non-Javadoc)
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#shouldVisit(java.lang.String, edu.uci.ics.crawler4j.url.WebURL)
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
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException
				| IllegalStateException | InvocationTargetException | NoSuchMethodException
				| InstantiationException e) {
			String msg = String.format("Exception while processing 'shouldVisit' ('%s', '%s')",
					domain, webURL);
			logger.error(msg, e);
			throw new IllegalStateException(e);
		}
	}

	/* (non-Javadoc)
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#extractData(java.lang.String, java.lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> extractData(String domain, String html)
			throws IllegalStateException {
		if (!domainMap.containsKey(domain.toLowerCase()))
			throw new IllegalArgumentException("No script for domain: " + domain);

		Class<?> class1 = domainMap.get(domain.toLowerCase());

		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_EXTRACT_DATA_METHOD_NAME, Object.class);
			Map<String, Object> result = (Map<String, Object>) method.invoke(instance, html);
			return result;
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException
				| IllegalStateException | InvocationTargetException | NoSuchMethodException
				| InstantiationException e) {
			String msg = String.format("Exception while processing 'extractData' ('%s', 'html')",
					domain);
			logger.error(msg, e);
			throw new IllegalStateException(e);
		}
	}
}
