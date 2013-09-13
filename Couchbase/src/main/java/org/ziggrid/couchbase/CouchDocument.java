package org.ziggrid.couchbase;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.utils.exceptions.UtilException;

public class CouchDocument {
	Logger logger = LoggerFactory.getLogger("CouchDocument");
	private final Map<String, CouchView> views = new TreeMap<String, CouchView>();
	final String docName;
	
	public CouchDocument(String docName) {
		this.docName = docName;
	}

	public CouchView create(String viewName) {
		CouchView ret = new CouchView(this, viewName);
		views.put(viewName, ret);
		return ret;
	}

	public String asJson() {
		try {
			StringWriter sw = new StringWriter();
			JsonFactory jf = new JsonFactory();
			JsonGenerator sb = jf.createJsonGenerator(sw);
			sb.writeStartObject();
			sb.writeStringField("language", "javascript");
			sb.writeFieldName("views");
			sb.writeStartObject();
			for (CouchView v : views.values()) {
				v.asJson(sb);
			}
			sb.writeEndObject();
			sb.writeEndObject();
			sb.flush();
			return sw.toString();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public String getName() {
		return docName;
	}

	public void writeToCouch(HttpClient cli, String baseUri) throws HttpException, IOException {
		if (!baseUri.endsWith("/"))
			baseUri += "/";
		PutMethod put = new PutMethod(baseUri + "_design/"+getName());
		put.setRequestEntity(new StringRequestEntity(asJson(), "application/json", "ISO-8859-1"));
		cli.executeMethod(put);
		String reply = put.getResponseBodyAsString();
		if (!reply.startsWith("{"))
			throw new UtilException(reply);
		try
		{
			JSONObject jsReply = new JSONObject(reply);
			if (!jsReply.has("ok") || !jsReply.getBoolean("ok"))
				throw new UtilException("Failed to write design document '" + getName() + "' (" + reply + ")");
			logger.info("Wrote design document " + getName());
		}
		catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public String toString() {
		return "DesignDoc[" + docName + "]";
	}
}
