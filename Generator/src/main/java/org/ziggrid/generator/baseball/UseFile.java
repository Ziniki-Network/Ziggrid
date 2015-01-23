package org.ziggrid.generator.baseball;

import java.net.URL;
import java.util.Iterator;

import org.ziggrid.generator.main.Timer;
import org.zinutils.collections.ListMap;
import org.zinutils.utils.StreamProvider;
import org.zinutils.xml.XMLElement;

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

	public void storeFiles(ListMap<String, StreamProvider> seasonFiles) {
		URL uri = getClass().getResource(resource);
		String path = uri.getPath();
		String season = path.substring(0, 4);
		seasonFiles.add(season, new StreamProvider.File(uri.getPath()));
	}
}
