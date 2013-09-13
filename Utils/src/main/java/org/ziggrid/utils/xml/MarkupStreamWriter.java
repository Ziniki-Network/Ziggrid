package org.ziggrid.utils.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.UtilException;

/** This class is designed to be able to cope with the demands of
 * all the similar-yet-different markup languages out there, but
 * still be 95% a drop-in replacement for XMLStreamWriter.
 * 
 * Specifically, it gives enough additional control to support
 * writing OOB things like mustaches and ASP data
 *
 * <p>
 * &copy; 2013 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class MarkupStreamWriter {
	public class StackFrame {
		final String label;
		final Mode prev;

		public StackFrame(Mode mode, String wrapper) {
			this.prev = mode;
			this.label = wrapper;
		}
	}

	public enum Mode {
		BODY, INELEMENT, BLOCKMUSTACHE;

		public void assertMode(Mode... outof) {
			for (Mode m : outof) {
				if (m == this)
					return;
			}
			throw new UtilException("In mode " + this + ", not one of " + CollectionUtils.listOf(outof));
		}
	}
	
	private final Writer writer;
	private Mode mode = Mode.BODY;
	private final List<StackFrame> stack = new ArrayList<StackFrame>();
	
	public MarkupStreamWriter() {
		this.writer = new StringWriter();
	}
	
	public MarkupStreamWriter(Writer out) {
		this.writer = out;
	}

	public void writeStartElement(String wrapper) {
		mode.assertMode(Mode.BODY, Mode.BLOCKMUSTACHE);
		try {
			writer.append("<");
			writer.append(wrapper);
			stack.add(0, new StackFrame(mode, wrapper));
			mode = Mode.INELEMENT;
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void writeAttribute(String key, String value) {
		mode.assertMode(Mode.INELEMENT);
		try {
			writer.append(" ");
			writer.append(key);
			writer.append("=\"");
			writer.append(encode(value));
			writer.append("\"");
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	/** Force the generator into "body" mode by closing any opening
	 * tags that may be out there.  This is useful in order to force
	 * non-self-closing tags as well as to be able to combine code
	 * that does arbitrary output (such as mustaches) with code that
	 * needs to be sure the opening tag is complete.
	 * 
	 * This method is called implicitly from writeCharacters.
	 */
	public void selectBodyMode() {
		try {
			if (mode == Mode.INELEMENT) {
				writer.append(">");
				mode = Mode.BODY;
			} else
				mode.assertMode(Mode.BODY, Mode.BLOCKMUSTACHE);
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void writeCharacters(String chars) {
		try {
			selectBodyMode();
			writer.append(encode(chars));
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void writeEndElement() {
		if (stack.isEmpty())
			throw new UtilException("Cannot end element, since none open");
		try {
			StackFrame pop = stack.remove(0);
			if (mode == Mode.INELEMENT) {
				writer.append("/>");
			} else {
				mode.assertMode(Mode.BODY);
				writer.append("</");
				writer.append(pop.label);
				writer.append(">");
				mode = pop.prev;
			}
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	/** Close an element without actually writing anything.
	 * This is useful to support some HTML constructs such as <input> that don't want any kind of close tag
	 */
	public void closeElement() {
		if (stack.isEmpty())
			throw new UtilException("Cannot end element, since none open");
		stack.remove(0);
	}

	public void writeMustache(String mustache) {
		try {
			if (mode == Mode.INELEMENT)
				writer.append(" ");
			writer.append("{{");
			writer.append(mustache);
			writer.append("}}");
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
		
	}

	public void writeBlockMustache(String header, String content) {
		try {
			writer.append("{{#");
			writer.append(header);
			writer.append(" ");
			writer.append(content);
			writer.append("}}");
			stack.add(0, new StackFrame(mode, header));
			mode = Mode.BLOCKMUSTACHE;
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void writeEndMustache() {
		if (stack.isEmpty())
			throw new UtilException("Cannot end mustache, since none open");
		mode.assertMode(Mode.BLOCKMUSTACHE);
		try {
			StackFrame pop = stack.remove(0);
			writer.append("{{/");
			writer.append(pop.label);
			writer.append("}}");
			mode = pop.prev;
		} catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	private CharSequence encode(String s) {
		StringBuilder ret = new StringBuilder(s);
		for (int i=0;i<ret.length();i++) {
			if (ret.charAt(i) == '"')
				ret.replace(i, i+1, "&quot;");
			else if (ret.charAt(i) == '<')
				ret.replace(i, i+1, "&lt;");
			else if (ret.charAt(i) == '>')
				ret.replace(i, i+1, "&gt;");
			else if (ret.charAt(i) == '&')
				ret.replace(i, i+1, "&amp;");
		}
		return ret;
	}

	public void flush() {
		// TODO Auto-generated method stub
		
	}
	
	public String getOutput() {
		if (writer instanceof StringWriter) {
			flush();
			return writer.toString();
		}
		throw new UtilException("Cannot extract string from " + writer);
	}
}
