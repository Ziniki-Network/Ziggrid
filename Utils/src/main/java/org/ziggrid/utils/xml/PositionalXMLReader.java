package org.ziggrid.utils.xml;
// PositionalXMLReader.java

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class PositionalXMLReader {

    public static void readXML(final InputStream is, final LocationAnnotator handler) throws IOException, SAXException {
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
//            factory.setFeature("http://xml.org/sax/features/declaration-handler", true);
            parser = factory.newSAXParser();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

//        parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
//        parser.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
//        parser.getXMLReader().setContentHandler(handler);
//        parser.getXMLReader().setErrorHandler(handler);
//        parser.getXMLReader().setFeature("http://xml.org/sax/features/lexical-handler/parameter-entities", true);
        parser.parse(is, handler);
    }
}
