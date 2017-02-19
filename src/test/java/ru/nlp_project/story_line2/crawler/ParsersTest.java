package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static ru.nlp_project.story_line2.crawler.IGroovyInterpreter.*;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;

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

	private IGroovyInterpreter testable;

	@Before
	public void setUp() throws Exception {
		CrawlerConfiguration configuration = new CrawlerConfiguration();
		configuration.scriptDir = scriptDir.toString();
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}

	@Test
	public void test_BnkomiRU() throws IOException {
		String content = FileUtils.readFileToString(
				new File(htmlsDir.toString() + File.separator + "bnkomi.ru.html"));
		WebURL webURL = new WebURL();
		webURL.setURL("www.bnkomi.ru/data/news/51840");
		Map<String, Object> data = testable.extractData("bnkomi.ru", webURL, content);
		// 05.07.2016 19:55
		assertEquals("Tue Jul 05 19:55:00 MSK 2016", data.get(EXTR_KEY_PUB_DATE).toString());
		assertEquals("Минимальная цена билета на ЧМ-2018 для россиян - 1280 рублей",
				data.get(EXTR_KEY_TITLE));
		assertEquals(950, data.get(EXTR_KEY_CONTENT).toString().length());
		assertTrue(data.get(EXTR_KEY_CONTENT).toString()
				.startsWith("Минимальная цена билета на матч чемпионата мира"));
		assertTrue(data.get(EXTR_KEY_CONTENT).toString()
				.endsWith("на матч открытия составит $550, на финал — $1100."));
	}


	@Test
	public void test_BnkomiRU_Doc() throws IOException {
		String content = FileUtils.readFileToString(
				new File(htmlsDir.toString() + File.separator + "bnkomi.ru.doc.html"));
		WebURL webURL = new WebURL();
		webURL.setURL("www.bnkomi.ru/data/doc/51898/");
		Map<String, Object> data = testable.extractData("bnkomi.ru", webURL, content);
		// 05.07.2016 19:55
		assertEquals("Thu Jul 07 12:47:00 MSK 2016", data.get(EXTR_KEY_PUB_DATE).toString());
		assertEquals(
				"На обсуждение: «В России к осени разработают новую концепцию поддержки автопрома»",
				data.get("title"));
		assertEquals(4663, data.get(EXTR_KEY_CONTENT).toString().length());
		assertTrue(data.get(EXTR_KEY_CONTENT).toString().startsWith(
				"В правительстве изучают возможность создания нового механизма привлечения"));
		assertTrue(data.get(EXTR_KEY_CONTENT).toString().endsWith(
				"2-3 уровней — при условии господдержки и с учетом наличия сырьевой базы."));
		assertEquals(
				"bnkomi.ru/content/news/images/51898/6576-avtovaz-nameren-uvelichit-eksport-lada_mainPhoto.jpg",
				data.get(EXTR_KEY_IMAGE_URL));
	}
}
