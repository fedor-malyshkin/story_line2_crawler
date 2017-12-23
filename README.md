# Анализатор (паук) веб-сайтов для сбора необходимой информации и передачи на сервер проекта.

### Общее описание
Паук оформлен в виде микросервиса с одним конфигурационным файлом. Построен с
использованием [Spring](http://spring.io) и сопутствующих библиотек инфраструктуры.

### Получение данных
С учтом того, что не все сайты публикуют rss/atom-ленты приходится информацию
с некоторых получать посредством их прямого парсинга. для этого и используется
crawler (edu.uci.ics:crawler4j), полученные страницы в дальнейшем анализируются
groovy скриптами (проект  [crawler_scripts](https://github.com/fedor-malyshkin/story_line2_crawler_scripts)),
которые не только определяют необходимость извлечения информации, но и извлекают дополнительную информацию (дату публикации, ссылку на картинку и т.д.)

С сайтами которые публикуют rss/atom-ленты тоже не всё так просто: некоторые выкладывают полное содержание статьи в ленте, другие же размещают только заголовок - в данном случае приходится идти по ссылке и там так же повторять полный анализ с извлечением данных.

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
# Количество дней на которое должна быть более стара новость для того, что бы
# не загружать картинки
skip_images_older_days: 30

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
    "raw_content" : "<html>....",  // только в случае невозможности предварительного извлечения данных....
    "path" : "/data/news/58212/",
    "source" : "bnkomi.ru",
    "title" : "Сыктывкарец ради отпуска за границей полностью погасил долг по кредиту",
    "image_url" : "bnkomi.ru/content/news/images/51898/6576-avtovaz-nameren-uvelichit-eksport-lada_mainPhoto.jpg",
    "url" : "https://www.bnkomi.ru/data/news/58212/"
}
```
При этом в случае если контент сайта не был получен с использованием готовых feed-ов, поля "publication_date", "title", "image_url" и "content" могут отсуствовать и требуется дополнительный анализ и извлечение информации (а поле "raw_content" -- присутствовать).


# Интерпретация и структура скриптов для анализа.
Основной класс для анализа и исполнения скриптов: "ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl", который ожидает найти скрипты в каталоге из переменной "crawler_script_dir".
