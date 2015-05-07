package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class Compartment extends SbmlBase {
	
	int spatial_dimensions = 3;
	Double size = null;
	String units = null, outside_key = null;
	Compartment outside;
	final boolean is_constant;
	final Model model;
	
	Compartment(Model model, Attributes atts) throws SAXException {
		super(atts);
		this.model = model;
		String str_dim = atts.getValue("spatialDimensions");
		if (str_dim != null) {
			spatial_dimensions = Integer.parseInt(str_dim);
		}
		size = getDoubleAttOpt(atts, "size");
		units = atts.getValue("units");
		outside_key = atts.getValue("outside");
		is_constant = getBoolAttOpt(atts, "constant", true);
	}
	
	public int getSpatialDimentsions() { return spatial_dimensions; }
	
	public Double getSize() { return size; }

	public String getUnits() { return units; }
	
	public Compartment getOutside() throws SbmlCompilerException {
		if ((outside == null) && (outside_key != null)) {
			outside = (Compartment) model.getEntity(outside_key);
			if (outside == null) {
				throw new SbmlCompilerException(SbmlCompilerException
						.UNKNOWN_MODEL_ENTITY, outside_key);
			}
		}
		return outside;
	}
	
	public boolean isConstant() { return is_constant; }
}
