package org.ziggrid.generator.baseball;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.zinutils.collections.ListMap;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StreamProvider;
import org.zinutils.utils.StringUtil;
import org.zinutils.utils.ZUJarEntry;
import org.zinutils.utils.ZUJarFile;
import org.zinutils.xml.XMLElement;

public class UseResource  {
	private final String resource;
	private final String pattern;

	public UseResource(BaseballFactory root, XMLElement xe) {
		resource = xe.required("resource");
		pattern = xe.required("pattern");
		xe.attributesDone();
	}
	
	public void storeFiles(ListMap<String, StreamProvider> seasonFiles) {
		List<File> pathElts = FileUtils.splitJavaPath(System.getProperty("java.class.path"));
		for (File f : pathElts) {
			if (f.isDirectory()) {
				File from = FileUtils.combine(f, resource);
				if (from.isDirectory()) {
					for (File g : FileUtils.findFilesMatching(from, pattern)) {
						String path = g.getName();
						String season = path.substring(0, 4);
						seasonFiles.add(season, new StreamProvider.File(g));
					}
				}
			} else if (f.isFile() && f.getName().endsWith(".jar")) {
				Pattern match = StringUtil.globMatcher(resource + "/" + pattern);
				ZUJarFile jf = new ZUJarFile(f);
				for (ZUJarEntry je : jf) {
					String name = je.getName();
					if (!match.matcher(name).matches())
						continue;
					File g = new File(name);
					String path = g.getName();
					String season = path.substring(0, 4);
					seasonFiles.add(season, new StreamProvider.FromJar(f, name));
				}
			}
		}
	}
}