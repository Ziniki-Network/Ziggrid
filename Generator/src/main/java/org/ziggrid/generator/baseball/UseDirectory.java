package org.ziggrid.generator.baseball;

import java.io.File;

import org.zinutils.collections.ListMap;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StreamProvider;
import org.zinutils.xml.XMLElement;

public class UseDirectory  {
	private File directory;
	private String pattern;

	public UseDirectory(BaseballFactory root, XMLElement xe) {
		directory = new File(xe.required("directory"));
		pattern = xe.required("pattern");
		xe.attributesDone();
	}
	
	public void storeFiles(ListMap<String, StreamProvider> seasonFiles) {
		if (!directory.isDirectory())
			return;
		for (File f : FileUtils.findFilesMatching(directory, pattern)) {
			String path = f.getName();
			String season = path.substring(0, 4);
			seasonFiles.add(season, new StreamProvider.File(f));
		}
	}
}