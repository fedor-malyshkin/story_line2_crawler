# Анализатор (паук) веб-сайтов для сбора необходимой информации и передачи на сервер проекта.

### Общее описание
Паук оформлен в виде микросервиса с одним конфигурационным файлом. Построен с
использованием библиотеки "dropwizard.io" и сопутствующих библиотек инфраструктуры.

### Получение данных
С учтом того, что не все сайты публикуют rss/atom-ленты приходится информацию
с некоторых получать посредством их прямого парсинга. для этого и используется скфцдук (edu.uci.ics:crawler4j), полученные страницы в дальнейшем анализируются groovy скриптами (проект  [crawler_scripts](https://github.com/fedor-malyshkin/story_line2_crawler_scripts)), которые как определяю.т необходимость извлечения информации, так и извлекают дополнительную информацию (дату публикации, ссылку на картинку и т.д.)

С сайтами которые публикуют rss/atom-ленты тоже не всё так просто: некоторые выкладывают полное содержание статьи в ленте, другие же размещают только заголовок - в данном случае приходится идти по ссылке и там так же повторять полный анализ с извлечением данных.

### Сборка
Сборка итогового компонента проекта осуществляется с помощью gradle plugin'а
"shadowJar" (com.github.johnrengelman.shadow), зависящего от задачи "build". Так
что для создания итогового файла необходимо запускать "gradle shadowJar"

### Запуск и отладка итогового микросервиса
Для запуска в рабочем и отладочноме режиме используются скриптовые файлы в корне проекта
"run.sh" и "run_debug.sh" соответственно.

### Конфигурационный файл
Далее описана структура конфигурационного файла
```yaml
---
# Количество потоков на сайт (лучше не более 4)
crawler_per_site: 1
# Папка со скриптами для парсинга контента сайтов
crawler_script_dir: /tmp/crawler/scripts
# Каталог для сохранения данных о результатах парсинга сайта (для каждого сайта
# создается подпапка с именем домена для хранения соотвествующих данных)
crawler_storage_dir: /tmp/crawler
# строка подключения к MongoDB для сохранения результатаов анализа
mongodb_connection_url: mongodb://localhost:27017/


# Блок с настройками для анализируемых сайтов
parse_sites:
   # На каждый сайт. Название домена (будет использоваться для идентфикации и записи в БД)
 - source: bnkomi.ru
   # На каждый сайт. стартовая страница для парсинга
   seed: http://bnkomi.ru
   cron_schedule: "0 0/5 * * * ?" # Fire every 5 minutes

feed_sites:
   # На каждый сайт. Название домена (будет использоваться для идентфикации и записи в БД)
 - source: komiinform.ru
   # Для тех сайтов, что содержат в feed'е весь контент в данном параметре выставляется 'false'
   parse_for_content: false
   # Для тех сайтов, что содержат в feed'е изображение в данном параметре выставляется 'false'
   parse_for_image: false
   # На каждый сайт. стартовая страница для парсинга
   feed: http://komiinform.ru/rss/news/
   # Расписание в формате cron (http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html)
   cron_schedule: "0 0/5 * * * ?" # Fire every 5 minutes
```
### Журналирование

### Метрики
influxdb_metrics:
   enabled: false
   influxdb_host: ""
   influxdb_port: 8086
   influxdb_db: ""
   influxdb_user: ""
   influxdb_password: ""
   reporting_period: 30

### Запись в БД
Уникальность записи в БД определяется на основании пары - (domain:URL)
Запись по умолчанию производится в базу данных mongodb "crawler" (коллекция "crawler_entries")
Формат записи следующий:
```json
{
    "_id" : ObjectId("587cbc11aca9f3482120b052"),
    "publication_date" : ISODate("2017-01-13T16:06:00.000Z"), // datetime in UTC
	"processing_date" : ISODate("2017-01-13T16:06:00.000Z"), // datetime in UTC
	"content" : "Около ... фактическим исполнением.",
    "path" : "/data/news/58212/",
    "source" : "bnkomi.ru",
    "title" : "Сыктывкарец ради отпуска за границей полностью погасил долг по кредиту",
    "image_url" : "bnkomi.ru/content/news/images/51898/6576-avtovaz-nameren-uvelichit-eksport-lada_mainPhoto.jpg",
    "image_data" : ........,
    "url" : "https://www.bnkomi.ru/data/news/58212/"
}
```

# Интерпретация и структура скриптов для анализа.
Основной класс для анализа и исполнения скриптов: "ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl", который ожидает найти скрипты в каталоге из переменной "crawler_script_dir".

Каждый скрипт - groovy-скрипт, определённой структуры. Примерная структура каждого скрипта такова:
```groovy
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.joda.time.format.*;

public class bnkomi_ru {
	public static String source = "bnkomi.ru"

	def extractData (source, webUrl, html) {
		// ...
		return [ 'title':title, 'publication_date':date, 'content':content, 'image_url':img ]
	}

	def shouldVisit(url)
	{
		// ...
    	return true;
	}
}
```
К скриптам предъявляются следующие условия:
1. имя класса (в данном случае "bnkomi_ru") должно совпадать с именем файла + ".groovy" (в данном случае будет "bnkomi_ru.groovy"). Общая рекомендация: приводить к нижнему регистру и заменять точку на знак подчёркивания.
1. должны пристутствовать 2 публичных метода:
  - Map<String, Object> extractData(String domain, WebURL webURL, String html)
  - boolean shouldVisit(String domain, WebURL webURL)
1. должен присутствовать публичный статический член (public static) типа "String" с именем "source"
