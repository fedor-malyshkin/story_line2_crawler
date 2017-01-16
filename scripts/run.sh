#!/bin/sh
gradle shadowJar
java -jar build/libs/crawler-*-all.jar server src/test/resources/ru/nlp_project/story_line2/crawler/crawler_config.yml

