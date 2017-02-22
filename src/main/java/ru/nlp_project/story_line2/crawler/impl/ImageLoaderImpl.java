package ru.nlp_project.story_line2.crawler.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import ru.nlp_project.story_line2.crawler.IImageLoader;

public class ImageLoaderImpl implements IImageLoader {

	public byte[] loadImage(String imageUrl) throws IOException {
			URL url = new URL(imageUrl);
			InputStream openStream = url.openStream();
			return IOUtils.toByteArray(openStream);
	}

}
