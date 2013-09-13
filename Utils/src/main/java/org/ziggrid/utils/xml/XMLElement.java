package org.ziggrid.utils.xml;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.ziggrid.utils.exceptions.InvalidXMLTagException;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.exceptions.XMLMissingAttributeException;
import org.ziggrid.utils.exceptions.XMLUnprocessedAttributeException;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XMLElement implements Externalizable {
	private final XML inside;
	private Element elt;
	private final HashSet<String> attrsProcessed = new HashSet<String>();
	private XMLErrorHandler handler;

	XMLElement(XML inside, Element elt) {
		this.inside = inside;
		this.elt = elt;
	}
	
	public void setErrorHandler(XMLErrorHandler handler) {
		this.handler = handler;
	}

	public String tag() {
		return elt.getTagName();
	}

	public boolean hasTag(String tag) {
		return tag().equals(tag);
	}

	public void assertTag(String tag) {
		if (!tag.equals(tag()))
			throw new UtilException("The element " + this + " does not have tag " + tag);
	}

	// Linear Access to attributes
	public String required(String attr) {
		if (!elt.hasAttribute(attr)) {
			XMLMissingAttributeException ex = new XMLMissingAttributeException("The required attribute '" + attr + "' was not found on " + this);
			if (handler != null)
				handler.missingAttribute(getStartLocation(), getEndLocation(), ex);
			throw ex;
		}
		attrsProcessed.add(attr);
		return elt.getAttribute(attr);
	}

	/** If you want to allow an attribute, but don't want to get its value,
	 * you can "accept" it.  This will not complain if it is not there.
	 * 
	 * @param attr the attribute to accept
	 */
	public void accept(String attr) {
		if (elt.hasAttribute(attr))
			attrsProcessed.add(attr);
	}
	
	public String optional(String attr) {
		if (elt.hasAttribute(attr))
		{
			attrsProcessed.add(attr);
			return elt.getAttribute(attr);
		}
		return null;
	}
	
	public String optional(String attr, String def) {
		if (elt.hasAttribute(attr))
		{
			attrsProcessed.add(attr);
			return elt.getAttribute(attr);
		}
		return def;
	}
	
	public void attributesDone() {
		if (attrsProcessed.size() != elt.getAttributes().getLength())
		{
			StringBuilder msg = new StringBuilder("At end of attributes processing for " + tag() + ", attributes were unprocessed:");
			XMLUnprocessedAttributeException ex = null;
			for (String a : attributes())
				if (!attrsProcessed.contains(a)) {
					msg.append(" " + a);
					ex = new XMLUnprocessedAttributeException(a, msg.toString());
					if (handler != null)
						handler.unprocessedAttribute(getStartLocation(), getEndLocation(), ex);
				}
			if (ex != null && handler == null)
				throw ex;
		}
	}
	
	// Random Access
	public String get(String attr) {
		if (!elt.hasAttribute(attr)) {
			XMLMissingAttributeException ex = new XMLMissingAttributeException("The required attribute '" + attr + "' was not found on " + this);
			if (handler != null)
				handler.missingAttribute(getStartLocation(), getEndLocation(), ex);
			throw ex;
		}
		return elt.getAttribute(attr);
	}
	
	// Children functions
	public List<XMLElement> elementChildren() {
		return elementChildren(null);
	}
	
	public List<XMLElement> elementChildren(String tagged) {
		ArrayList<XMLElement> ret = new ArrayList<XMLElement>();
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Element && (tagged == null || ((Element)n).getTagName().equals(tagged))) {
				XMLElement child = new XMLElement(inside, (Element)n);
				child.setErrorHandler(handler);
				ret.add(child);
			}
		}
		return ret;
	}

	public ArrayList<Object> mixed() {
		ArrayList<Object> ret = new ArrayList<Object>();
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Element) {
				XMLElement child = new XMLElement(inside, (Element)n);
				child.setErrorHandler(handler);
				ret.add(child);
			} else if (n instanceof Text) {
				ret.add(((Text)n).getData());
			}
		}
		return ret;
	}

	// The idea of this is to use introspection and reflection to figure out what the object wants,
	// and then give it that in the most appropriate order.
	// I would like this to involve dynamically re-ordering the input file if the alternative would be an error,
	// but that seems difficult to get right in the general case.
	public void populate(Object cxt, Object callbacks) {
		trackAs(cxt, elt);
		ObjectMetaInfo info = new ObjectMetaInfo(callbacks);
		
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Element)
			{
				XMLElement xe = new XMLElement(inside, (Element)n);
				trackAs(cxt, elt);
				xe.setErrorHandler(handler);
				try {
					Object inner = info.dispatch(cxt, xe);
					if (inner == null)
					{
						xe.assertNoSubContents();
						// then there had better be no non-whitespace children of xe
					}
					else if (!(inner instanceof XMLCompletelyHandled))
					{
						xe.populate(cxt, inner);
					}
				} catch (InvalidXMLTagException ex) {
					if (!xe.hasHandler())
						throw ex;
				} catch (XMLMissingAttributeException ex) {
					if (!xe.hasHandler())
						throw ex;
				} catch (XMLUnprocessedAttributeException ex) {
					if (!xe.hasHandler())
						throw ex;
				}
			}
			else if (n instanceof Text)
			{
				if (!info.wantsText)
					continue;
				if (callbacks instanceof XMLContextTextReceiver)
					((XMLContextTextReceiver)callbacks).receiveText(cxt, ((Text)n).getData());
				else if (callbacks instanceof XMLTextReceiver)
					((XMLTextReceiver)callbacks).receiveText(((Text)n).getData());
				else
					throw new UtilException("There is no valid text handler");
			}
		}
		if (callbacks instanceof XMLNotifyOnComplete) {
			trackAs(cxt, elt);
			((XMLNotifyOnComplete)callbacks).complete(cxt);
		}
	}

	private void trackAs(Object cxt, Element lookAt) {
		if (cxt instanceof XMLTrackLocation)
			((XMLTrackLocation)cxt).elementLocation((Location)elt.getUserData(LocationAnnotator.START_FROM), (Location)elt.getUserData(LocationAnnotator.END_AT));
	}

	public void assertNoSubContents() {
		NodeList nl = elt.getChildNodes();
		int len = nl.getLength();
		for (int i=0; i<len; i++)
		{
			Node n = nl.item(i);
			
			if (n instanceof Comment)
				continue;
			if (n instanceof Text)
			{
				String s = ((Text)n).getData();
				for (char c : s.toCharArray())
				{
					if (!Character.isWhitespace(c))
						throw new UtilException(this + " cannot have non-whitespace children");
				}
				continue;
			}
			throw new UtilException("This node cannot have " + n.getClass() + " as a child"); 
		}
	}

	@Override
	public String toString() {
		return "XMLElement[" + elt.getTagName() + " @ {" + inside.fromResource + ":" + elt.getUserData("lineNumber")+"}]";
	}

	public String serialize() {
		return serialize(true);
	}

	public String serialize(boolean withXMLDeclaration) {
		OutputFormat of = new OutputFormat();
		of.setOmitXMLDeclaration(!withXMLDeclaration);
		StringWriter fos = new StringWriter();
		XMLSerializer serializer = new XMLSerializer(fos, of);
		try {
			serializer.asDOMSerializer();
			serializer.serialize(elt);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
		return fos.toString();
	}

	public void serializeChildrenTo(StringBuilder sb) {
		OutputFormat of = new OutputFormat();
		of.setOmitXMLDeclaration(true);
		StringWriter fos = new StringWriter();
		XMLSerializer serializer = new XMLSerializer(fos, of);
		try {
			NodeList childNodes = elt.getChildNodes();
			for (int i=0;i<childNodes.getLength();i++)
				serializer.serialize(childNodes.item(i));
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
		sb.append(fos.toString());
	}

	public void serializeAttribute(StringBuilder sb, String attr) {
		Attr node = elt.getAttributeNode(attr);
		sb.append(attr);
		sb.append("=");
		sb.append("'");
		// TODO: this really needs escaping
		sb.append(node.getNodeValue());
		sb.append("'");
	}

	public String text() {
		return elt.getTextContent();
	}

	public String textFrom(String... elements)
	{
		XMLElement e = this;
		for (String s : elements)
			e = e.uniqueElement(s);
		return e.text();
	}
	
	public List<String> attributes() {
		List<String> ret = new ArrayList<String>();
		NamedNodeMap attributes = elt.getAttributes();
		for (int i=0;i<attributes.getLength();i++)
			ret.add(((Attr)attributes.item(i)).getName());
		return ret;
	}

	public XMLElement addElement(String tag) {
		Element child = inside.doc.createElement(tag);
		elt.appendChild(child);
		return new XMLElement(inside, child);
	}


	public void addText(String text) {
		elt.appendChild(inside.doc.createTextNode(text));
	}

	public XMLElement addElement(XMLElement xe) {
		XMLElement ret = addElement(xe.tag());
		for (String attr : xe.attributes())
			ret.setAttribute(attr, xe.get(attr));
		for (XMLElement elt : xe.elementChildren())
			ret.addElement(elt);
		return ret;
	}

	public XMLElement setAttribute(String attr, String value) {
		elt.setAttribute(attr, value);
		return this;
	}

	public boolean hasAttribute(String attr) {
		return elt.hasAttribute(attr);
	}

	public XMLElement setAttribute(XMLNSAttr attr, String value) {
		attr.applyTo(elt, value);
		return this;
	}

	public XMLElement uniqueElement(String string) {
		XMLElement ret = null;
		for (XMLElement e : elementChildren())
			if (e.hasTag(string))
			{
				if (ret == null)
				{
					ret = e;
					continue;
				}
				throw new UtilException("There was more than one element tagged " + string);
			}
		if (ret != null)
			return ret;
		throw new UtilException("There was no element called " + string);
	}

	public XMLElement()
	{
		inside = new XML("1.0");
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(elt);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		elt = (Element) in.readObject();
	}

	public int requiredInt(String parm) {
		return Integer.parseInt(required(parm));
	}

	public int optionalInt(String parm, int def) {
		return Integer.parseInt(optional(parm, Integer.toString(def)));
	}

	public boolean requiredBoolean(String parm, boolean b) {
		return Boolean.parseBoolean(required(parm));
	}

	public boolean optionalBoolean(String parm, boolean b) {
		return Boolean.parseBoolean(optional(parm, Boolean.toString(b)));
	}

	public void applyLocation(Object cxt) {
		if (cxt instanceof XMLWantsLocationInfo)
			((XMLWantsLocationInfo)cxt).elementLocation(getStartLocation(), getEndLocation());
	}

	public Location getStartLocation() {
		return (Location)elt.getUserData(LocationAnnotator.START_FROM);
	}

	public Location getEndLocation() {
		return (Location)elt.getUserData(LocationAnnotator.END_AT);
	}
	
	public boolean hasHandler() {
		return handler != null;
	}

	public XMLErrorHandler getHandler() {
		return handler;
	}
}
