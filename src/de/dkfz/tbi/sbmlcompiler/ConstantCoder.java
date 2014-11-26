package de.dkfz.tbi.sbmlcompiler;

import java.util.Map;
import org.sbml.libsbml.*;

/**
 * Implements a FORTRAN constant. Offering the keyword PARAMETER, FORTRAN
 * allows for the definition of constants. <code>ConstantCoder</code> is
 * mainly used for defining the constants pi and e, which <code>SbmlCompiler
 * </code> includes on demand, and for encoding constant values of model
 * entities. 
 * @author Samuel Bandara
 */
class ConstantCoder extends FortranCoder {

	/**
	 * The libSBML object of the model entity this coder represents. 
	 */
	private SBase ref_obj = null;
	
	/**
	 * Value of the constant.
	 */
	private double my_value = 0;
	
	/**
	 * @param obj libSBML object of the model entity this coder represents
	 */
	public ConstantCoder(SBase obj, SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		if (ref_obj instanceof Compartment) {
			Compartment compartment = (Compartment)ref_obj;
			if (compartment.isSetVolume()) {
				my_value = compartment.getVolume();
			}
			else my_value = 1;
		}
		else if (ref_obj instanceof Species) {
			Species species = (Species)ref_obj;
			if (species.isSetInitialConcentration()) {
				my_value = species.getInitialConcentration();
			}
			else if (species.isSetInitialAmount()) {
				my_value = species.getInitialAmount();
			}
		}
		else if (ref_obj instanceof Parameter) {
			Parameter param = (Parameter)ref_obj;
			if (param.isSetValue()) {
				my_value = param.getValue();
			}
		}
	}
	
	/**
	 * @param value value of the constant
	 */
   	ConstantCoder(double value, SbmlCompiler compiler) {
   		super(compiler);
		my_value = value;
	}
   	
   	/**
   	 * Returns the value of the constant.
   	 * @return value of the constant
   	 */
	double getValue() { return my_value; }
	
	void putFortranCode(FortranFunction target, Map<String,
			FortranCoder> bindings) throws SbmlCompilerException {
		String name = getVarName();
		target.declareVar(name);
		target.defineConst(name + " = " + Double.toString(my_value));
	}
	
	protected void initialize(Map<String, FortranCoder> bindings) { }
	
	public SBase getSbmlNode() { return ref_obj; }

	String getPrefix() { return "const"; }
}
