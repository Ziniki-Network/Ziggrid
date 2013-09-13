package org.ziggrid.utils.jsgen;

import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.utils.PrettyPrinter;

public class JSBuilder {
	private final PrettyPrinter pp = new PrettyPrinter();
	private final JsonFactory jf = new JsonFactory();
	private boolean isPretty;
	private boolean objectComma;
	private boolean openedCurly;
	private boolean openedSquare;
	private boolean requireCommaOrNewLine;
	
	public JSBuilder() {
		pp.indentWidth(2);
		pp.setNlAtEnd(false);
	}
	
	public void setPretty(boolean b) {
		isPretty = b;
	}

	public void orb() {
		append("(");
	}
	
	public void crb() {
		append(")");
	}
	
	public void ocb() {
		if (isPretty && !pp.hasWhitespace() && pp.lastChar() != '(') append(" ");
		append("{");
		this.openedCurly = true;
	}
	
	public void ccb() {
		ccb(true);
	}
	public void ccb(boolean withFinalNL) {
		if (!openedCurly && isPretty)
			pp.indentLess();
		this.objectComma = false;
		this.openedCurly = false;
		append("}");
		if (isPretty)
			requireCommaOrNewLine = withFinalNL;
	}

	public void osb() {
		append("[");
		this.openedSquare = true;
	}
	
	public void csb() {
		if (!openedSquare && isPretty)
			pp.indentLess();
		this.objectComma = false;
		this.openedSquare = false;
		append("]");
//		if (isPretty)
//			requireCommaOrNewLine = true;
	}

	public void semi(boolean wantNL) {
		append(";");
		if (isPretty && wantNL)
			pp.requireNewline();
	}

	public void objectComma() {
		this.objectComma = true;
	}

	public void assign(String op) {
		if (isPretty) append(" " + op + " ");
		else append(op);
	}
	
	public void ident(String s) {
		if (!pp.hasWhitespace() && Character.isJavaIdentifierPart(pp.lastChar()))
			pp.append(' ');
		append(s);
	}

	public void fieldName(String field) {
		if (field == null)
			throw new UtilException("field name cannot be null");
		else
			append(field.toString());
		append(":");
		if (isPretty) append(" ");
	}
	
	public void writeJSON(Object value) {
		try {
			StringWriter sw = new StringWriter();
			JsonGenerator jg = jf.createJsonGenerator(sw);
			writeJSON(jg, value);
			jg.flush();
			append(sw.toString());
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static void writeJSON(JsonGenerator jg, Object value) {
		try {
			if (value == null)
				jg.writeNull();
			else if (value instanceof Boolean)
				jg.writeBoolean((Boolean) value);
			else if (value instanceof String)
				jg.writeString((String) value);
			else if (value instanceof Integer)
				jg.writeNumber((Integer)value);
			else if (value instanceof Double)
				jg.writeNumber((Double)value);
			else
				throw new UtilException("JSBuilder cannot write a value of type " + value.getClass());
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public void append(String s) {
		if (openedCurly) {
			if (isPretty)
				pp.indentMore();
			openedCurly = false;
			openedSquare = false;
		}
		if (objectComma) {
			pp.append(",");
			objectComma = false;
			if (isPretty)
				pp.requireNewline();
			requireCommaOrNewLine = false;
		} else if (s.startsWith(")") || s.startsWith("}") || s.startsWith(";")) {
			;
		} else if (requireCommaOrNewLine) {
			pp.requireNewline();
			requireCommaOrNewLine = false;
		}
		pp.append(s);
	}
	
	@Override
	public String toString() {
		return pp.toString();
	}
}
