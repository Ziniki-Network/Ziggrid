package org.ziggrid.api;

import java.util.Set;

public interface IModel {

	String getSHA(String doc);

	Set<Definition> willProcess(String type);

	boolean restrictionIncludes(String name);

}