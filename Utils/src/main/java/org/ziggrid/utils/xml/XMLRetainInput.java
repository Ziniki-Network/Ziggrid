package org.ziggrid.utils.xml;

/** If the populated top-level object wants to retain a memory of its original XML input
 * (after parsing), it should implement this interface.  After construction, but before population,
 * the "retainOriginalXML" method will be called.
 *
 * <p>
 * &copy; 2013 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public interface XMLRetainInput {
	/** Called when the object is created.
	 * 
	 * @param xml the top level XML node being used to do the population.
	 */
	void retainOriginalXML(XML xml);
}
