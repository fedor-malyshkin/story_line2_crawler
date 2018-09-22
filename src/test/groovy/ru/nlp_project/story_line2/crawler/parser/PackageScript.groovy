package ru.nlp_project.story_line2.crawler.parser

class PackageScript {
  public static String source = "package_script"

  def extractData(source, webUrl, html) {
    return new PackageUtilScript().returnValue()
  }

  def shouldVisit(url) {
    return true;
  }
}
