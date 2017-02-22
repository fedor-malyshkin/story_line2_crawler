import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.joda.time.format.*;

public class komiinform_ru {
public static String domain = "komiinform.ru"

def extractData (domain, webUrl, html) {
	Document doc = Jsoup.parse(html);
	
	def content = doc.text();
	return [ 'content':content ]
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
