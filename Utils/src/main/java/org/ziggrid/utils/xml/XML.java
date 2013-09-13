package org.ziggrid.utils.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.exceptions.XMLParseException;
import org.ziggrid.utils.utils.FileUtils;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XML {
	Document doc;
	private XMLElement top;
	private final String version;
	final String fromResource;
	private final LocationAnnotator annotator;

	XML(String version)
	{
		annotator = null;
		this.fromResource = "-";
		this.version = version;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private XML(String from, InputStream stream) {
		annotator = new LocationAnnotator(from);
		try
		{
			PositionalXMLReader.readXML(stream, annotator);
		}
		catch (SAXParseException ex)
		{
			if (!annotator.hasErrors())
				throw new XMLParseException(ex);
			annotator.unwindStack();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}

		fromResource = from;
		doc = annotator.getDocument();
		version = doc.getXmlVersion();
		top = new XMLElement(this, doc.getDocumentElement());
	}
	
	private XML(String version, String tag) {
		annotator = null;
		this.fromResource = "-";
		this.version = version;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.appendChild(doc.createElement(tag));
			top = new XMLElement(this, doc.getDocumentElement());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void setErrorHandler(XMLErrorHandler errorHandler) {
		this.top.setErrorHandler(errorHandler);
		annotator.replayParseErrors(errorHandler);
	}

	public static XML fromFile(File f) {
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(f);
			XML ret = new XML(f.toString(), fis);
			return ret;
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
		finally {
			if (fis != null)
				try { fis.close(); } catch (IOException ex) { }
		}
	}

	public static XML fromResource(String name)
	{
		InputStream stream = XML.class.getResourceAsStream(name);
		if (stream == null)
		{
			stream = XML.class.getResourceAsStream("/" + name);
			if (stream == null)
				throw new UtilException("Could not find resource " + name);
		}
		return new XML(name, stream);
	}

	public static XML fromString(String s) {
		InputStream is = new ByteArrayInputStream(s.getBytes());
		return new XML("-", is);
	}

	public static XML fromContainer(String name) {
		if (name.startsWith("resource:"))
			return fromResource(name.replace("resource:", ""));
		else if (name.startsWith("file:"))
			return fromFile(new File(name.replace("file:", "")));
		else
			throw new UtilException("I cannot understand the container name: " + name);
	}

	public static XML fromStream(String nameIs, InputStream stream) {
		return new XML(nameIs, stream);
	}

	public XMLElement top() {
		if (annotator != null && annotator.hasErrors()) {
			for (String s : annotator.getErrors())
				System.err.println(s);
			throw annotator.getFirstError();
		}
		return top;
	}

	
	public static XML create(String version, String tag) {
		return new XML(version, tag);
	}

	public void write(File file) {
		try
		{
			FileUtils.assertDirectory(file.getParentFile());
			FileOutputStream fos = new FileOutputStream(file);
			write(fos, false);
			fos.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public String prettyPrint()
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			write(baos, false);
			return new String(baos.toByteArray());
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	public static String prettyPrint(String s)
	{
		try
		{
			return XML.fromString(s).prettyPrint();
		}
		catch (XMLParseException ex)
		{
			System.out.println(s);
			throw ex;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	public void write(OutputStream fos, boolean omitDeclaration) throws IOException {
		OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
		of.setVersion(version);
		of.setIndent(1);
		of.setIndenting(true);
		of.setOmitXMLDeclaration(omitDeclaration);
		XMLSerializer serializer = new XMLSerializer(fos, of);
		serializer.asDOMSerializer();
		serializer.serialize(doc);
	}

	public String asString(boolean omitDeclaration) {
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			write(baos, omitDeclaration);
			return new String(baos.toByteArray());
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	public XMLElement addElement(String tag) {
		return top.addElement(tag);
	}

	public XMLNamespace namespace(String ns, String url) {
		top.setAttribute("xmlns:"+ns, url);
		return new XMLNamespace(ns, url);
	}

	@SuppressWarnings("unchecked")
	public <T> T populate(Class<T> cls, Object cxt) {
		Constructor<T> ctor = null;
		for (Constructor<?> c : cls.getConstructors())
		{
			Class<?>[] args = c.getParameterTypes();
			if (args.length == 2 && args[1].equals(XMLElement.class))
				ctor = (Constructor<T>) c;
		}
		if (ctor == null)
			throw new UtilException("There is no (Object, XMLElement) constructor for " + cls);
		T ret;
		try {
			top.applyLocation(cxt);
			ret = ctor.newInstance(cxt, top);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
		if (ret instanceof XMLRetainInput)
			((XMLRetainInput)ret).retainOriginalXML(this);
		top.populate(cxt, ret);
		return ret;
	}

	public String applyXPath(String path) {
		try {
			XPathFactory xf = XPathFactory.newInstance();
			XPath xp = xf.newXPath();
			XPathExpression xe = xp.compile(path);
			return (String) xe.evaluate(doc, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			throw UtilException.wrap(e);
		}
	}

	public List<XMLElement> elementsMatching(String path) {
		List<XMLElement> ret = new ArrayList<XMLElement>();
		try {
			XPathFactory xf = XPathFactory.newInstance();
			XPath xp = xf.newXPath();
			XPathExpression xe = xp.compile(path);
			NodeList evaluate = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			for (int i=0;i<evaluate.getLength();i++)
			{
				Node item = evaluate.item(i);
				if (item instanceof Element)
					ret.add(new XMLElement(this, (Element) item));
				else
					throw new UtilException("Cannot return " + item + " as an element");
			}
			return ret;
		} catch (XPathExpressionException e) {
			throw UtilException.wrap(e);
		}
	}
}

	
