package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Model extends SbmlBase {
	
	final HashMap<String, Compartment> compartments = new HashMap<String,
			Compartment>();
	
	Model(Attributes atts) throws SAXException {
		super(atts);
	}
	
	void addCompartment(Compartment compartment) {
		compartments.put(compartment.getId(), compartment);
	}
	
	public Compartment getCompartment(String id) {
		return compartments.get(id);
	}
}
