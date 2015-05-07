package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Parameter extends SbmlBase {
	
	final Double value;
	final String units;
	final boolean is_constant;
	
	Parameter(Attributes atts) throws SAXException {
		super(atts);
		value = getDoubleAttOpt(atts, "value");
		units = atts.getValue("units");
		is_constant = getBoolAttOpt(atts, "constant", true);
	}
	
	public Double getValue() { return value; }
	
	public String getUnits() { return units; }
	
	public boolean isConstant() { return is_constant; }
}
