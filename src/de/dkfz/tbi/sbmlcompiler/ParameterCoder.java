package de.dkfz.tbi.sbmlcompiler;

import de.dkfz.tbi.sbmlcompiler.sbml.SbmlBase;

/**
 * Implements a reference to an estimated parameter by assigning its value
 * to a local FORTRAN variable. Instead of a scaled item of the array of
 * parameters, a mark consisting of a dollar sign enclosing the name of the
 * parameter on each side, is put. 
 * @author Samuel Bandara
 */
class ParameterCoder extends FortranCoder {
	
	/**
	 * The libSBML object of the model entity this coder implements. 
	 */
	private SbmlBase refObj;
	
	/**
	 * The id of the model entity or the name of a calibration parameter.
	 */
	private String paramName;
	
	/**
	 * @param param libSBML object of the model entity this coder implements
	 * @param name id of the model entity or the name of a calibration
	 * parameter, which will be decorated, and used as mark
	 */
	ParameterCoder(SbmlBase param, String name, SbmlCompiler compiler) {
		super(compiler);
		refObj = param;
		paramName = name;
	}
	
	void putFortranCode(FortranFunction target, Bindings bindings)
			throws SbmlCompilerException {
		String stmt = getVarName();
		target.declareVar(stmt);
		stmt += " = $" + paramName + "$";
		target.appendStatement(stmt);
	}
	
	protected void initialize(Bindings bindings) { }
	
	public SbmlBase getSbmlNode() { return refObj; }

	String getPrefix() { return "par"; }
}
