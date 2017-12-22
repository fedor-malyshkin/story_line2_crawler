package ru.nlp_project.story_line2.crawler;

import static org.assertj.core.api.Assertions.*;

import edu.uci.ics.crawler4j.url.WebURL;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;

public class GroovyInterpreterTest {

	private static File srcDir =
			new File("src/test/groovy");
	private static Path scriptDir;
	private GroovyInterpreterImpl testable;
	private CrawlerConfiguration configuration;

	@Before
	public void setUp() throws Exception {
		scriptDir = Files.createTempDirectory("crawler");
		configuration = new CrawlerConfiguration();
		configuration.crawlerScriptDir = scriptDir.toString();
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.forceDelete(scriptDir.toFile());
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeWithNonExistingScriptDir() {
		configuration.crawlerScriptDir = "/tmp/NON_EXISTING_DIR";
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeWithEmptyScriptDir() {
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}

	/**
	 * Тестируем на стабильное поведение при отсуствии поля "source"  в классе.
	 */
	@Test
	public void testGetSourceFromScriptClass_NoSource() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("non_existing_source"), DirectoryFileFilter.DIRECTORY);
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}


	@Test
	public void testCallExtractData() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("working_script"), DirectoryFileFilter.DIRECTORY);

		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		Map<String, Object> result = testable
				.extractData("working_script", "http://working_script", "");
		assertThat(result).isNotNull().isNotEmpty().hasSize(4);
	}

	@Test
	public void testCallShouldVisit() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("working_script"), DirectoryFileFilter.DIRECTORY);
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		boolean expected = testable.shouldVisit("working_script", webURL);
		assertThat(expected).isTrue();
	}


	@Test(expected = IllegalArgumentException.class)
	public void testCallExtractData_NonExistingSource() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("working_script"), DirectoryFileFilter.DIRECTORY);
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		Map<String, Object> result = testable
				.extractData("NON_EXISTING_SOURCE", "http://working_script", "");
	}


	@Test
	public void testCallShouldVisit_NonExistingSource() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("working_script"), DirectoryFileFilter.DIRECTORY);

		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		boolean expected = testable.shouldVisit("NON_EXISTING_SOURCE", webURL);
		assertThat(expected).isFalse();
	}


	/**
	 * Проверка корректного вызова при наличии указания имени пакета в скрипте и вызова другого класса
	 * (в котором так же указан пакет)  -- интересует влияет ли неверное местоположение на разрешение
	 * имён.
	 */
	@Test
	public void testCallExtractData_WithSpecifiedPackageAndCalledUtil() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("package_"), DirectoryFileFilter.DIRECTORY);
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		Map<String, Object> result = testable
				.extractData("package_script", "http://working_script", "");
		assertThat(result).isNotNull().isNotEmpty().hasSize(4);
	}


	/**
	 * Проверка корректной загрузки скриптов при наличии рекурсивных ссылок.
	 */
	@Test
	public void testCallExtractData_RecursiveReferences() throws IOException {
		IOFileFilter filter = FileFilterUtils
				.or(FileFilterUtils.prefixFileFilter("recursive_"), DirectoryFileFilter.DIRECTORY);
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		Map<String, Object> result = testable
				.extractData("recursive_script1", "http://working_script", "");
		assertThat(result).isNotNull().isInstanceOf(Map.class);
	}

	@Test
	@Ignore
	public void testAutoRefreshGroovyCode() throws IOException {
		IOFileFilter filter = FileFilterUtils.prefixFileFilter("working_script");
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		Map<String, Object> extractData = testable
				.extractData("working_script", "http://working_script", "");
		assertThat(extractData.get("title")).isEqualTo("title");

		File fileSource = new File(srcDir, "new_working_script.groovy");
		File dstSource = new File(scriptDir.toFile(), "working_script.groovy");

		FileUtils.copyFile(fileSource, dstSource);
		extractData = testable.extractData("working_script", "http://working_script", "");
		assertThat(extractData.get("title")).isEqualTo("title2");
	}
}
