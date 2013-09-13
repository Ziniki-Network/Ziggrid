package org.ziggrid.utils.xml;

import org.ziggrid.utils.exceptions.InvalidXMLTagException;
import org.ziggrid.utils.exceptions.XMLMissingAttributeException;
import org.ziggrid.utils.exceptions.XMLUnprocessedAttributeException;

public interface XMLErrorHandler {

	void parseError(XMLParseError ex);

	void missingAttribute(Location from, Location to, XMLMissingAttributeException ex);

	void unprocessedAttribute(Location from, Location to, XMLUnprocessedAttributeException ex);

	void invalidTag(Location start, Location end, InvalidXMLTagException ex);

}
