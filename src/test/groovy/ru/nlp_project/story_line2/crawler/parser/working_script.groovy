package ru.nlp_project.story_line2.crawler.parser

public class working_script {
	public static String source = "working_script"

	def extractData(source, webUrl, html) {
		return ['title': 'title', 'publication_date': null, 'content': 'content', 'image_url': null]
	}

	def shouldVisit(url) {
		return true;
	}
}
