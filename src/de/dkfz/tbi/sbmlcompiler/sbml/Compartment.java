package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class Compartment extends SbmlBase {
	
	int spatial_dimensions = 3;
	Double size = null;
	String units = null, out_key = null;
	Compartment outside;
	boolean is_constant = true;
	final Model model;
	
	Compartment(Model model, Attributes atts) throws SAXException {
		super(atts);
		this.model = model;
		String str_dim = atts.getValue("spatialDimensions");
		if (str_dim != null) {
			spatial_dimensions = Integer.parseInt(str_dim);
		}
		String str_size = atts.getValue("size");
		if (str_size != null) {
			size = Double.valueOf(str_size);
		}
		units = atts.getValue("units");
		out_key = atts.getValue("outside");
		String str_constant = atts.getValue("constant");
		if (str_constant != null) {
			is_constant = Boolean.parseBoolean(str_constant);
		}
	}
	
	public int getSpatialDimentsions() { return spatial_dimensions; }
	
	public Double getSize() { return size; }

	public String getUnits() { return units; }
	
	public Compartment getOutside() throws SbmlCompilerException {
		if ((outside == null) && (out_key != null)) {
			outside = model.getCompartment(out_key);
			if (outside == null) {
				throw new SbmlCompilerException(SbmlCompilerException
						.UNKNOWN_MODEL_ENTITY, out_key);
			}
		}
		return outside;
	}
	
	public boolean isConstant() { return is_constant; }
}
