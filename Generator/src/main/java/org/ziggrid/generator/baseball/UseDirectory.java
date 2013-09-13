package org.ziggrid.generator.baseball;

import java.io.File;

import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.utils.FileUtils;
import org.ziggrid.utils.xml.XMLElement;

public class UseDirectory  {
	private File directory;
	private String pattern;

	public UseDirectory(BaseballFactory root, XMLElement xe) {
		directory = new File(xe.required("directory"));
		pattern = xe.required("pattern");
		xe.attributesDone();
	}
	
	public void storeFiles(ListMap<String, File> seasonFiles) {
		for (File f : FileUtils.findFilesMatching(directory, pattern)) {
			String path = f.getName();
			String season = path.substring(0, 4);
			seasonFiles.add(season, f);
		}
	}
}