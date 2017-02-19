package ru.nlp_project.story_line2.crawler.model;

/**
 * Объект - идентификатор. Предназначе в первую очередь для классов объектной модели с целью
 * различной сериализации в зависимости от типа преобразования.
 * 
 * @author fedor
 *
 */
public class Id {
	public String value = "";

	public Id(String value) {
		super();
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}



}
