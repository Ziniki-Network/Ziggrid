package org.ziggrid.generator.baseball;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

import org.ziggrid.generator.main.Timer;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.xml.XMLElement;

public class UseFile {
	private String resource;

	public UseFile(BaseballFactory root, XMLElement xe) {
		resource = xe.required("resource");
		xe.attributesDone();
	}

	public Iterator<Game> games(Timer timer) {
		URL uri = getClass().getResource(resource);
		System.out.println(uri.getPath());
		return null;
	}

	public void storeFiles(ListMap<String, File> seasonFiles) {
		URL uri = getClass().getResource(resource);
		String path = uri.getPath();
		String season = path.substring(0, 4);
		seasonFiles.add(season, new File(uri.getPath()));
	}
}
