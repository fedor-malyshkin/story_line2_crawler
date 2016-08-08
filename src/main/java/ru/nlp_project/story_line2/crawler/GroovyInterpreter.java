package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

public class GroovyInterpreter {

  private static final String GROOVY_EXT_NAME = "groovy";
  private static final String SCRIPT_DOMAIN_STATIC_FILED = "domain";
  private static final String SCRIPT_SHOULD_VISIT_METHOD_NAME = "shouldVisit";
  private static final String SCRIPT_EXTRACT_DATA_METHOD_NAME = "extractData";
  private GroovyScriptEngine scriptEngine;
  private HashMap<String, Class<?>> domainMap;

  private GroovyInterpreter() {}

  public static GroovyInterpreter newInstance(ConfigurationReader configurationReader)
      throws IllegalStateException {
    GroovyInterpreter result = new GroovyInterpreter();
    result.initialize(configurationReader);
    return result;
  }

  private void initialize(ConfigurationReader configurationReader) throws IllegalStateException {
    domainMap = new HashMap<String, Class<?>>();
    try {
      scriptEngine = new GroovyScriptEngine(configurationReader.getConfigurationMain().scriptDir);
      Collection<File> files =
          FileUtils.listFiles(new File(configurationReader.getConfigurationMain().scriptDir),
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


  public boolean shouldVisit(String domain, WebURL webURL) throws IllegalStateException {
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
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> extractData(String domain, String html) throws IllegalStateException {
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
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }
}
