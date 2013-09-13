package org.ziggrid.couchbase;

import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.jsgen.JSBuilder;
import org.ziggrid.utils.jsgen.JSEntry;
import org.ziggrid.utils.jsgen.JSFile;
import org.ziggrid.utils.jsgen.JSFunction;
import org.ziggrid.utils.jsgen.JSVar;

public class CouchView {
	private final static Logger logger = LoggerFactory.getLogger("CouchView");
	private final CouchDocument doc;
	private final JSFile file;
	private JSFunction map;
	private JSEntry reduce;
	private final String viewName;

	public CouchView(CouchDocument doc, String viewName) {
		logger.debug("Creating Couch View " + doc.docName+"."+viewName);
		this.doc = doc;
		this.viewName = viewName;
		this.file = new JSFile();
	}

	public void asJson(JsonGenerator sb) throws Exception {
		sb.writeFieldName(viewName);
		sb.writeStartObject();
		if (map != null) {
			JSBuilder builder = file.getBuilder();
			map.toScript(builder);
			sb.writeStringField("map", builder.toString());
		}
		if (reduce != null) {
			if (reduce instanceof JSVar)
				sb.writeStringField("reduce", ((JSVar)reduce).getName());
			else if (reduce instanceof JSFunction) {
				JSBuilder builder = file.getBuilder();
				((JSFunction)reduce).toScript(builder);
				sb.writeStringField("reduce", builder.toString());
			} else
				throw new UtilException("Cannot handle " + reduce + " as a reduce function");
		}
		sb.writeEndObject();
	}

	@Override
	public String toString() {
		try {
			StringWriter sw = new StringWriter();
			JsonFactory jf = new JsonFactory();
			JsonGenerator sb = jf.createJsonGenerator(sw);
			sw.append("View["+doc.docName+"/"+viewName+":");
			sb.writeStartObject();
			asJson(sb);
			sb.writeEndObject();
			sb.flush();
			sw.append("]");
			return sw.toString();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public JSFunction needMap() {
		if (map == null)
			map = file.getBlock().newFunction("doc", "meta");
		return map;
	}

	public JSFunction needReduce() {
		if (reduce == null)
			reduce = file.getBlock().newFunction("key", "values", "rereduce");
		else if (!(reduce instanceof JSFunction))
			throw new UtilException("Cannot treat " + reduce + " as a function");
		return (JSFunction)reduce;
	}

	private void standardReduce(String code) {
		if (reduce != null)
			throw new UtilException("Cannot define reduce more than once");
		reduce = file.newSymbol(code);
	}

	public void reduceCount() {
		standardReduce("_count");
	}

	public void reduceSum() {
		standardReduce("_sum");
	}

	public void reduceStats() {
		standardReduce("_stats");
	}
}
