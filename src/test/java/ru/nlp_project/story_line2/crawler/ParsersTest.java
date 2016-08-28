package ru.nlp_project.story_line2.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParsersTest {

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

	private GroovyInterpreter testable;

	@Before
	public void setUp() throws Exception {
		CrawlerConfiguration configuration = new CrawlerConfiguration();
		configuration.scriptDir = scriptDir.toString();
		testable = GroovyInterpreter.newInstance(configuration);
	}

	@Test
	public void test_BnkomiRU() throws IOException {
		String content = FileUtils.readFileToString(
				new File(htmlsDir.toString() + File.separator + "bnkomi.ru.html"));
		Map<String, Object> data = testable.extractData("bnkomi.ru", content);
		// 05.07.2016 19:55
		assertEquals("2016-07-05T19:55:00.000+03:00", data.get("date").toString());
		assertEquals("Минимальная цена билета на ЧМ-2018 для россиян - 1280 рублей",
				data.get("title"));
		assertEquals(950, data.get("content").toString().length());
		assertTrue(data.get("content").toString()
				.startsWith("Минимальная цена билета на матч чемпионата мира"));
		assertTrue(data.get("content").toString()
				.endsWith("на матч открытия составит $550, на финал — $1100."));
	}


	@Test
	public void test_BnkomiRU_Doc() throws IOException {
		String content = FileUtils.readFileToString(
				new File(htmlsDir.toString() + File.separator + "bnkomi.ru.doc.html"));
		Map<String, Object> data = testable.extractData("bnkomi.ru", content);
		// 05.07.2016 19:55
		assertEquals("2016-07-07T12:47:00.000+03:00", data.get("date").toString());
		assertEquals(
				"На обсуждение: «В России к осени разработают новую концепцию поддержки автопрома»",
				data.get("title"));
		assertEquals(4663, data.get("content").toString().length());
		assertTrue(data.get("content").toString().startsWith(
				"В правительстве изучают возможность создания нового механизма привлечения"));
		assertTrue(data.get("content").toString().endsWith(
				"2-3 уровней — при условии господдержки и с учетом наличия сырьевой базы."));
	}
}
