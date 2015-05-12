package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class Species extends SbmlBase {
	
	final String compartment_key;
	Compartment compartment = null;
	Double initial_amount = null, initial_conc = null;
	String substance_units = null, size_units = null;
	boolean has_only_substance_units = false, boundary_condition = false;
	Integer charge = null;
	boolean is_constant = false;
	private final Model model;
	
	Species(Model model, Attributes atts) throws SAXException {
		super(atts);
		this.model = model;
		compartment_key = atts.getValue("compartment");
		initial_amount = getDoubleAttOpt(atts, "initialAmount");
		initial_conc = getDoubleAttOpt(atts, "initialConcentration");		
		substance_units = atts.getValue("substanceUnits");
		size_units = atts.getValue("spatialSizeUnits");
		has_only_substance_units = getBoolAttOpt(atts, "hasOnlySubstanceUnits",
				false);
		boundary_condition = getBoolAttOpt(atts, "boundaryCondition", false);
		String str_charge = atts.getValue("charge");
		if (str_charge != null) {
			charge = Integer.valueOf(str_charge);
		}
		is_constant = getBoolAttOpt(atts, "constant", false);
	}
	
	public Compartment getCompartment() throws SbmlCompilerException {
		if (compartment == null) {
			compartment = (Compartment) model.getEntity(compartment_key);
			if (compartment == null) {
				throw new SbmlCompilerException(SbmlCompilerException
						.UNKNOWN_MODEL_ENTITY, compartment_key);
			}
		}
		return compartment;
	}
	
	public Double getInitialAmount() { return initial_amount; }
	
	public Double getInitialConcentration() { return initial_conc; }
	
	public String getSubstanceUnits() { return substance_units; }

	public String getSizeUnits() { return size_units; }
	
	public boolean hasOnlySubstanceUnits() { return has_only_substance_units; }
	
	public boolean isBoundary() { return boundary_condition; }
	
	public Integer getCharge() { return charge; }
	
	public boolean isConstant() { return is_constant; }
}
