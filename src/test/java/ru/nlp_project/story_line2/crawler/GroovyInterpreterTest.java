package ru.nlp_project.story_line2.crawler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

public class GroovyInterpreterTest {

  private static Path scriptDir;

  @BeforeClass
  public static void setUpClass() throws IOException {
    scriptDir = Files.createTempDirectory("crawler");
    FileUtils.forceDeleteOnExit(scriptDir.toFile());
    FileUtils.copyDirectory(new File("src/main/groovy/ru/nlp_project/story_line2/crawler/parser"),
        scriptDir.toFile());

  }

  private GroovyInterpreter testable;

  @Before
  public void setUp() throws Exception {
    ConfigurationReader configurationReader = new ConfigurationReader(null);
    configurationReader.getConfigurationMain().scriptDir = scriptDir.toString();
    testable = GroovyInterpreter.newInstance(configurationReader);
  }

  @Test
  public void testShouldVisit() throws IOException, ResourceException, ScriptException,
      NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, InstantiationException {
    WebURL webURL = new WebURL();
    webURL.setURL("http://www.bnkomi.ru");
    assertTrue(testable.shouldVisit("bnkomi.ru", webURL));
  }


  @Test
  public void testShouldVisit_WrongDomain() throws IOException, ResourceException, ScriptException,
      NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, InstantiationException {
    assertFalse(testable.shouldVisit("xxxxx.ru", null));
  }
  
  @Test()
  public void testShouldVisit_WrongException() throws IOException, ResourceException, ScriptException,
      NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, InstantiationException {
    WebURL webURL = new WebURL();
    webURL.setURL("http://www.bnkomi.ru/1.png");
    assertFalse(testable.shouldVisit("bnkomi.ru", webURL));
  }


}
