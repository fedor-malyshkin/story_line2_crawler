package ru.nlp_project.story_line2.crawler.impl;

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

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
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
	private static final String SCRIPT_SOURCE_STATIC_FILED = "source";
	private static final String SCRIPT_SHOULD_VISIT_METHOD_NAME = "shouldVisit";
	private static final String SCRIPT_EXTRACT_DATA_METHOD_NAME = "extractData";
	private GroovyScriptEngine scriptEngine;
	private HashMap<String, Class<?>> sourceMap;
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
		File dir = new File(configuration.scriptDir);
		if (!dir.isDirectory() || !dir.exists())
			throw new IllegalStateException(
					String.format("'%s' not exists.", configuration.scriptDir));

		sourceMap = new HashMap<String, Class<?>>();
		try {
			scriptEngine = createGroovyScriptEngine(configuration);
			Collection<File> files = FileUtils.listFiles(new File(configuration.scriptDir),
					new String[] {GROOVY_EXT_NAME}, false);

			if (files.isEmpty())
				throw new IllegalStateException(
						String.format("No script files in '%s'.", configuration.scriptDir));

			for (File file : files) {
				Class<?> scriptClass = loadScriptClassByName(file);
				String source = getSourceFromScriptClass(scriptClass);
				sourceMap.put(source.toLowerCase(), scriptClass);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	protected GroovyScriptEngine createGroovyScriptEngine(CrawlerConfiguration configuration) throws IOException {
		GroovyScriptEngine result = new GroovyScriptEngine(configuration.scriptDir);
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setRecompileGroovySource(true);
		result.setConfig(compilerConfiguration);
		return result;
	}

	protected String getSourceFromScriptClass(Class<?> scriptClass) {
		String source;
		try {
			Field field = scriptClass.getField(SCRIPT_SOURCE_STATIC_FILED);
			source = (String) field.get(null);

		} catch (IllegalAccessException | IllegalArgumentException
				| java.lang.NoSuchFieldException e) {
			throw new IllegalStateException(
					String.format("Error while gettings 'source' member (must be public static): ",
							e.getMessage()));
		}
		return source;

	}

	protected Class<?> loadScriptClassByName(File file) throws ResourceException, ScriptException {
		String name = file.getName();
		Class<?> scriptClass = scriptEngine.loadScriptByName(name);
		return scriptClass;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.nlp_project.story_line2.crawler.IGroovyInterpreter#shouldVisit(java.lang.String,
	 * edu.uci.ics.crawler4j.url.WebURL)
	 */
	@Override
	// TODO: проверить как вызывается - что бы не было кросс-извлечения
	public boolean shouldVisit(String source, WebURL webURL) throws IllegalStateException {
		// важная отсечка сайтов из других доменов!!!
		if (!sourceMap.containsKey(source.toLowerCase()))
			return false;
		Class<?> class1 = sourceMap.get(source.toLowerCase());
		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_SHOULD_VISIT_METHOD_NAME, Object.class);
			Boolean result = (Boolean) method.invoke(instance, webURL);
			return result.booleanValue();
		} catch (Exception e) {
			logger.error("Exception while processing 'shouldVisit' ({}, {})", source,
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
	// TODO: проверить как вызывается - что бы не было кросс-извлечения
	public Map<String, Object> extractData(String source, WebURL webURL, String html)
			throws IllegalStateException {
		if (webURL == null)
			throw new IllegalArgumentException("webURL is null.");
		if (!sourceMap.containsKey(source.toLowerCase())) {
			logger.error("No script with 'extractData' for source: '{}'", source);
			throw new IllegalArgumentException("No script for domain: " + source);
		}

		Class<?> class1 = sourceMap.get(source.toLowerCase());

		try {
			Object instance = class1.newInstance();
			Method method = class1.getMethod(SCRIPT_EXTRACT_DATA_METHOD_NAME, Object.class,
					Object.class, Object.class);
			Map<String, Object> result =
					(Map<String, Object>) method.invoke(instance, source, webURL, html);
			return result;
		} catch (Exception e) {
			logger.error("Exception while processing {}:{}", source, webURL.getPath(), e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Object executeScript(String script, Binding binding) throws Exception {
		GroovyShell shell = new GroovyShell(scriptEngine.getGroovyClassLoader(), binding);
		return shell.evaluate(script);
	}



}
