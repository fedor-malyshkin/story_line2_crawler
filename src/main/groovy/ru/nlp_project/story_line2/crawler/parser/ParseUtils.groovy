import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.joda.time.format.*;
import java.net.URL;

public class ParseUtils {

public static String domain = "utils"

    def static makeFullPath(webUrl, link) {
        URL url = new URL(webUrl.url);
        def result = url.host + link
        result = result.replace('//', '/')
        result = url.protocol + '://' + result
        return result;	
    }

}
