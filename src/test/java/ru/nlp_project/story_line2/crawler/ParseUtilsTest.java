package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import edu.uci.ics.crawler4j.url.WebURL;
import groovy.lang.Binding;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;

public class ParseUtilsTest {

	private static Path scriptDir;
	private static Path htmlsDir;

	@BeforeClass
	public static void setUpClass() throws IOException {
		htmlsDir = Files.createTempDirectory("crawler-htmls");
		scriptDir = Files.createTempDirectory("crawler");
		FileUtils.forceDeleteOnExit(scriptDir.toFile());
		FileUtils.forceDeleteOnExit(htmlsDir.toFile());
		FileUtils.copyDirectory(
				new File("src/main/groovy/ru/nlp_project/story_line2/crawler/parser"),
				scriptDir.toFile());
		FileUtils.copyDirectory(
				new File("src/test/resources/ru/nlp_project/story_line2/crawler/parser"),
				htmlsDir.toFile());

	}

	private IGroovyInterpreter testable;

	@Before
	public void setUp() throws Exception {
		CrawlerConfiguration configuration = new CrawlerConfiguration();
		configuration.scriptDir = scriptDir.toString();
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}

	@Test
	public void test_makeFullPath() throws Exception {

		WebURL webURL = new WebURL();
		webURL.setURL("https://www.bnkomi.ru/data/news/51840");

		Binding binding = new Binding();

		binding.setVariable("webUrl", webURL);
		binding.setVariable("link", "/data/news/51840");

		Object executeScript =
				testable.executeScript("ParseUtils.makeFullPath(webUrl, link)", binding);
		assertEquals("https://www.bnkomi.ru/data/news/51840", executeScript);
	}



}
