package org.ziggrid.generator.baseball;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.xml.XMLElement;

public class Config {
	public final List<UseFile> files = new ArrayList<UseFile>();
	public final List<UseDirectory> directories = new ArrayList<UseDirectory>();
	public final List<UseResource> resources = new ArrayList<UseResource>();

	public Config(BaseballFactory root, XMLElement xe) {
		xe.accept("factory");
		xe.attributesDone();
	}
	
	public UseDirectory directory(BaseballFactory root, XMLElement xe) {
		UseDirectory ret = new UseDirectory(root, xe);
		directories.add(ret);
		return ret;
	}

	public UseResource resource(BaseballFactory root, XMLElement xe) {
		UseResource ret = new UseResource(root, xe);
		resources.add(ret);
		return ret;
	}
	
	public UseFile file(BaseballFactory root, XMLElement xe) {
		UseFile ret = new UseFile(root, xe);
		files.add(ret);
		return ret;
	}
}
