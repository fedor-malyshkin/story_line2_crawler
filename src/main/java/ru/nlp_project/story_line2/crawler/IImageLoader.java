package ru.nlp_project.story_line2.crawler;

import java.io.IOException;

public interface IImageLoader {
	public byte[] loadImage(String imageUrl) throws IOException;
}
