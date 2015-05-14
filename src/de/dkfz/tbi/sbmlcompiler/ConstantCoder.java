package de.dkfz.tbi.sbmlcompiler;

import java.util.Map;

import de.dkfz.tbi.sbmlcompiler.sbml.Compartment;
import de.dkfz.tbi.sbmlcompiler.sbml.Parameter;
import de.dkfz.tbi.sbmlcompiler.sbml.SbmlBase;
import de.dkfz.tbi.sbmlcompiler.sbml.Species;

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
	private SbmlBase ref_obj = null;
	
	/**
	 * Value of the constant.
	 */
	private double my_value = 0;
	
	/**
	 * @param obj libSBML object of the model entity this coder represents
	 */
	public ConstantCoder(SbmlBase obj, SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		if (ref_obj instanceof Compartment) {
			Compartment compartment = (Compartment) ref_obj;
			if (compartment.getSize() != null) {
				my_value = compartment.getSize();
			}
			else my_value = 1;
		}
		else if (ref_obj instanceof Species) {
			Species species = (Species) ref_obj;
			if (species.getInitialConcentration() != null) {
				my_value = species.getInitialConcentration();
			}
			else if (species.getInitialAmount() != null) {
				my_value = species.getInitialAmount();
			}
		}
		else if (ref_obj instanceof Parameter) {
			Parameter param = (Parameter) ref_obj;
			if (param.getValue() != null) {
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
	
	public SbmlBase getSbmlNode() { return ref_obj; }

	String getPrefix() { return "const"; }
}
