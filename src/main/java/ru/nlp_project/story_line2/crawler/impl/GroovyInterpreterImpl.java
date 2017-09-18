package ru.nlp_project.story_line2.crawler.impl;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nlp_project.story_line2.crawler.CrawlerConfiguration;
import ru.nlp_project.story_line2.crawler.IGroovyInterpreter;

/**
 * Интерпретатор groovy. Для скриптов, анализирующих html-страницы источников.
 *
 * @author fedor
 */
public class GroovyInterpreterImpl implements IGroovyInterpreter {

	private static final String GROOVY_EXT_NAME = "groovy";
	private static final String SCRIPT_SOURCE_STATIC_FILED = "source";
	private static final String SCRIPT_SHOULD_VISIT_METHOD_NAME = "shouldVisit";
	private static final String SCRIPT_EXTRACT_RAW_DATA_METHOD_NAME = "extractRawData";
	private static final String SCRIPT_EXTRACT_DATA_METHOD_NAME = "extractData";
	private static final String SCRIPT_SHOULD_PROCESS_METHOD_NAME = "shouldProcess";
	private GroovyScriptEngine scriptEngine;
	private HashMap<String, Class<?>> sourceMap;
	private Logger log;

	private GroovyInterpreterImpl() {
		log = LoggerFactory.getLogger(this.getClass());
	}

	public static GroovyInterpreterImpl newInstance(CrawlerConfiguration configuration)
			throws IllegalStateException {
		GroovyInterpreterImpl result = new GroovyInterpreterImpl();
		result.initialize(configuration);
		return result;
	}

	@Override
	public boolean shouldProcess(String source, WebURL url) {
		// важная отсечка сайтов из других доменов!!!
		if (!sourceMap.containsKey(source.toLowerCase())) {
			return false;
		}
		Class<?> class1 = sourceMap.get(source.toLowerCase());
		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_SHOULD_PROCESS_METHOD_NAME, Object.class);
			return (Boolean) method.invoke(instance, url);
		} catch (Exception e) {
			log.error("Exception while processing 'shouldVisit' ({}, {})", source, url.getPath(),
					e);
			throw new IllegalStateException(e);
		}
	}

	private void initialize(CrawlerConfiguration configuration) throws IllegalStateException {
		File dir = new File(configuration.scriptDir);
		if (!dir.isDirectory() || !dir.exists()) {
			throw new IllegalStateException(
					String.format("'%s' not exists.", configuration.scriptDir));
		}
		sourceMap = new HashMap<>();
		try {
			scriptEngine = createGroovyScriptEngine(configuration);
			Collection<File> files = FileUtils.listFiles(new File(configuration.scriptDir),
					new String[]{GROOVY_EXT_NAME}, true);

			if (files.isEmpty()) {
				throw new IllegalStateException(
						String.format("No script files in '%s'.", configuration.scriptDir));
			}

			for (File file : files) {
				Class<?> scriptClass = loadScriptClassByName(file);
				String source = getSourceFromScriptClass(scriptClass);
				if (source != null) {
					sourceMap.put(source.toLowerCase(), scriptClass);
					log.debug("Loaded script '{}' for source '{}' (as: '{}').", file.getName(), source,
							scriptClass.getName());
				} else {
					log.debug("Loaded misc script '{}' (as: '{}').", file.getName(),
							scriptClass.getName());
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	private GroovyScriptEngine createGroovyScriptEngine(CrawlerConfiguration configuration)
			throws IOException {
		GroovyScriptEngine result = new GroovyScriptEngine(configuration.scriptDir);
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setRecompileGroovySource(true);
		result.setConfig(compilerConfiguration);
		return result;
	}

	/**
	 * Получить имя источника (source) для обработки из скрипта.
	 *
	 * Ожидаем публичное статчиное текстовое поле, совпадающее с именем источника.
	 *
	 * @param scriptClass класс скрипта
	 */
	private String getSourceFromScriptClass(Class<?> scriptClass) {
		String source;
		try {
			Field field = scriptClass.getField(SCRIPT_SOURCE_STATIC_FILED);
			source = (String) field.get(null);

		} catch (java.lang.NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException | IllegalArgumentException
				e) {
			throw new IllegalStateException(
					String.format("Error while gettings 'source' member (must be public static): ",
							e.getMessage()), e);
		}
		return source;

	}

	private Class<?> loadScriptClassByName(File file) throws ResourceException, ScriptException {
		String name = file.getAbsolutePath();
		return (Class<?>) scriptEngine.loadScriptByName(name);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#shouldVisit(java.lang.String,
	 * edu.uci.ics.crawler4j.url.WebURL)
	 */
	@Override
	public boolean shouldVisit(String source, WebURL webURL) throws IllegalStateException {
		if (webURL == null) {
			throw new IllegalArgumentException("'webURL' must be not null.");
		}

		// важная отсечка сайтов из других доменов!!!
		Class<?> class1;
		try {
			class1 = getSourceScriptClass(source);
		} catch (IllegalArgumentException e) {
			return false;
		}

		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_SHOULD_VISIT_METHOD_NAME, Object.class);
			return (Boolean) method.invoke(instance, webURL);
		} catch (Exception e) {
			log.error("Exception while processing 'shouldVisit' ({}, {})", source, webURL.getPath(),
					e);
			throw new IllegalStateException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#extractRawData(java.lang.String,
	 * edu.uci.ics.crawler4j.url.WebURL, java.lang.String)
	 */
	@Override
	public String extractRawData(String source, WebURL webURL, String html)
			throws IllegalStateException {
		if (webURL == null) {
			throw new IllegalArgumentException("'webURL' must be not null.");
		}
		if (html == null) {
			throw new IllegalArgumentException("'html' must be not null.");
		}

		Class<?> class1 = getSourceScriptClass(source);

		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_EXTRACT_RAW_DATA_METHOD_NAME, Object.class,
					Object.class, Object.class);
			return (String) method.invoke(instance, source, webURL, html);
		} catch (Exception e) {
			log.error("Exception while processing {}:{}", source, webURL.getPath(), e);
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Получить класс скрипта по имени источника (source).
	 *
	 * @param source имени источника
	 */
	private Class<?> getSourceScriptClass(String source) throws IllegalArgumentException {
		if (!sourceMap.containsKey(source.toLowerCase())) {
			log.error("No script with 'extractData' for source: '{}'", source.toLowerCase());
			throw new IllegalArgumentException("No script for source: " + source.toLowerCase());
		}
		return sourceMap.get(source.toLowerCase());
	}

	@Override
	public Object executeScript(String script, Binding binding) throws Exception {
		GroovyShell shell = new GroovyShell(scriptEngine.getGroovyClassLoader(), binding);
		return shell.evaluate(script);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#extractData(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	// TODO: проверить как вызывается - что бы не было кросс-извлечения
	public Map<String, Object> extractData(String source, String url, String html)
			throws IllegalStateException {
		if (url == null) {
			throw new IllegalArgumentException("'url' must be not null.");
		}
		if (html == null) {
			throw new IllegalArgumentException("'html' must be not null.");
		}

		Class<?> class1 = getSourceScriptClass(source);

		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_EXTRACT_DATA_METHOD_NAME, Object.class,
					Object.class, Object.class);
			return (Map<String, Object>) method.invoke(instance, source, url, html);
		} catch (Exception e) {
			log.error("Exception while processing {}:{}", source, url, e);
			throw new IllegalStateException(e);
		}
	}


}
