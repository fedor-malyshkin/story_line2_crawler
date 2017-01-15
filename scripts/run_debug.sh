#!/bin/sh
gradle shadowJar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000 -jar ../build/libs/crawler-*-all.jar server ../src/test/resources/ru/nlp_project/story_line2/crawler/crawler_config.yml

