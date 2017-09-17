package ru.nlp_project.story_line2.crawler.parser

public class PackageScript {
	public static String source = "package_script"

	def extractData(source, webUrl, html) {
		return PackageUtilScript.returnValue()
	}

	def shouldVisit(url) {
		return true;
	}
}
