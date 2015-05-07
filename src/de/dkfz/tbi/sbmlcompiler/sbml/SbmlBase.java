package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class SbmlBase {

	String name = null;
	String id = null;
	
	SbmlBase(Attributes atts) throws SAXException {
		id = atts.getValue("id");
		if (id == null) {
			throw new SAXException("Found element without ID.");
		}
		name = atts.getValue("name");
	}
	
	public String getName() { return name; }
	
	public String getId() { return id; }
}
