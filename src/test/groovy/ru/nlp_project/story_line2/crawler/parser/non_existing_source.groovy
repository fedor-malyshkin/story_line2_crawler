package ru.nlp_project.story_line2.crawler.parser;

public class non_existing_source {

		def extractData (source, webUrl, html) {
			return [ 'title':title, 'publication_date':date, 'content':content, 'image_url':img ]
		}

		def shouldVisit(url)
		{
	    	return true;
		}
}
