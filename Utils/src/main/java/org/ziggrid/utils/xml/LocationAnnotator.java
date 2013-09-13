package org.ziggrid.utils.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import org.ziggrid.utils.exceptions.UtilException;

public class LocationAnnotator extends DefaultHandler implements LexicalHandler {
    final static String START_FROM = "startFrom";
    final static String END_AT = "endAt";
    private Locator locator;
    final Stack<Element> elementStack = new Stack<Element>();
    final StringBuilder textBuffer = new StringBuilder();
	private final Document doc;
	private Location last;
	private boolean debugMode = false;
	private List<XMLParseError> errors = new ArrayList<XMLParseError>();
	private final String file;

    public LocationAnnotator(String file) {
    	this.file = file;
		try {
	        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	        doc = docBuilder.newDocument();
    	} catch (ParserConfigurationException ex) {
    		throw UtilException.wrap(ex);
    	}
	}

	public Document getDocument() {
		return doc;
	}

	public boolean hasErrors() {
		return errors.size() > 0;
	}

	public List<String> getErrors() {
		List<String> ret = new ArrayList<String>();
		for (XMLParseError ex : errors)
			ret.add(ex.toString());
		return ret;
	}
	
	public void replayParseErrors(XMLErrorHandler errorHandler) {
		for (XMLParseError ex : errors)
			errorHandler.parseError(ex);
	}

	public XMLParseError getFirstError() {
		return errors.get(0);
	}

	@Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator; // Save the locator, so that it can be used later for line tracking when traversing nodes.
    }

	private Location currentLocation() {
		return new Location(file, locator.getLineNumber(), locator.getColumnNumber());
	}
	
	private void setLast() {
		last = currentLocation();
	}

    @Override
	public void fatalError(SAXParseException e) throws SAXException {
    	errors.add(new XMLParseError(last, currentLocation(), e));
	}

    @Override
	public void error(SAXParseException e) throws SAXException {
    	errors.add(new XMLParseError(last, currentLocation(), e));
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
    	errors.add(new XMLParseError(last, currentLocation(), e));
	}

	@Override
	public void startDocument() throws SAXException {
    	debug("StartDoc " + locator.getLineNumber() + ":" + locator.getColumnNumber());
    	setLast();
	}


	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
    	debug("ProcInst " + locator.getLineNumber() + ":" + locator.getColumnNumber());
    	setLast();
	}

	@Override
	public void notationDecl(String arg0, String arg1, String arg2)
			throws SAXException {
		debug("NotationDecl");
    	setLast();
	}

	@Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {
        addTextIfNeeded();
    	debug(qName + " " + locator.getLineNumber() + ":" + locator.getColumnNumber());
        final Element el = doc.createElement(qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            el.setAttribute(attributes.getQName(i), attributes.getValue(i));
        }
        el.setUserData(START_FROM, last, null);
        el.setUserData(END_AT, currentLocation(), null);
        elementStack.push(el);
    	setLast();
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        addTextIfNeeded();
        final Element closedEl = elementStack.pop();
        if (elementStack.isEmpty()) { // Is this the root element?
            doc.appendChild(closedEl);
        } else {
            final Element parentEl = elementStack.peek();
            parentEl.appendChild(closedEl);
        }
    	setLast();
    }

	public void unwindStack() {
		while (!elementStack.isEmpty()) {
			endElement(null, null, null);
		}
	}

    @Override
    public void characters(final char ch[], final int start, final int length) throws SAXException {
    	debug("Chars: " + locator.getLineNumber() + ":" + locator.getColumnNumber());
    	debug(new String(ch).substring(start, start+length));
        textBuffer.append(ch, start, length);
    	setLast();
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
    		throws SAXException {
    	debug("WS: " + locator.getLineNumber() + ":" + locator.getColumnNumber());
    	setLast();
    }

    // Outputs text accumulated under the current node
    private void addTextIfNeeded() {
        if (textBuffer.length() > 0) {
            final Element el = elementStack.peek();
            final Node textNode = doc.createTextNode(textBuffer.toString());
            el.appendChild(textNode);
            textBuffer.delete(0, textBuffer.length());
        }
    }


	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
    	debug("Comment " + locator.getLineNumber() + ":" + locator.getColumnNumber());
    	setLast();
	}


	@Override
	public void endCDATA() throws SAXException {
		// TODO Auto-generated method stub
    	setLast();
		
	}


	@Override
	public void endDTD() throws SAXException {
		// TODO Auto-generated method stub
    	setLast();
		
	}


	@Override
	public void endEntity(String name) throws SAXException {
		// TODO Auto-generated method stub
    	setLast();
		
	}


	@Override
	public void startCDATA() throws SAXException {
    	debug("StartCDATA " + locator.getLineNumber() + ":" + locator.getColumnNumber());
    	setLast();
	}


	@Override
	public void startDTD(String arg0, String arg1, String arg2)
			throws SAXException {
		// TODO Auto-generated method stub
		
    	setLast();
	}


	@Override
	public void startEntity(String arg0) throws SAXException {
		// TODO Auto-generated method stub
		
    	setLast();
	}

	private void debug(String string) {
		if (debugMode )
			System.out.println(string);
	}
}
