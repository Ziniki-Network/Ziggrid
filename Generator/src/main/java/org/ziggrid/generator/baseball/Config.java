package org.ziggrid.generator.baseball;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.xml.XMLElement;

public class Config {

	public final String couchUrl;
	public final String bucket;
	public final List<UseFile> files = new ArrayList<UseFile>();
	public final List<UseDirectory> directories = new ArrayList<UseDirectory>();

	public Config(BaseballFactory root, XMLElement xe) {
		xe.accept("factory");
		couchUrl = xe.required("couchUrl");
		bucket = xe.required("bucket");
		xe.attributesDone();
	}
	
	public UseDirectory directory(BaseballFactory root, XMLElement xe) {
		UseDirectory ret = new UseDirectory(root, xe);
		directories.add(ret);
		return ret;
	}
	
	public UseFile file(BaseballFactory root, XMLElement xe) {
		UseFile ret = new UseFile(root, xe);
		files.add(ret);
		return ret;
	}
}
