package ru.nlp_project.story_line2.crawler.parser

public class RecursiveScript2 {
	public static String source = "recursive_script2"

	def extractData(source, webUrl, html) {
		println(RecursiveScript1.class)
		return [:]
	}

	def shouldVisit(url) {
		return true;
	}
}
