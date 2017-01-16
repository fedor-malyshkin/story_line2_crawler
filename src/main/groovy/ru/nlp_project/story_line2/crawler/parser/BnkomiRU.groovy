import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.joda.time.format.*;

public class BnkomiRU {
public static String domain = "bnkomi.ru"

def extractData (html) {
	Document doc = Jsoup.parse(html);
	
	def cont = doc.select("div.b-news-single")
	if (cont.empty) return null;
	
	def title = doc.select("h2.title").text();
	def date = doc.select("div.b-news-single  > div.date").text();
	
	// 05.07.2016 19:55
	DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");
	date = fmt.parseDateTime(date).toDate();
	
	
	def content = doc.select("div.b-news-single  > div.cnt").text();
	return [ 'title':title, 'date':date, 'content':content ]
}

def shouldVisit(url)
{
    def href = url.getURL().toLowerCase();
    def pattern = ~/.*\.(css|js|gif|jpg|png|mp3|mp3|zip|gz)$/ 
    def matcher = (href =~ pattern)
    if (matcher.matches())         
        return false
    return true;
}

}
