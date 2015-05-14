package de.dkfz.tbi.sbmlcompiler;

import java.util.Map;

import de.dkfz.tbi.sbmlcompiler.sbml.SbmlBase;

/**
 * Implements a reference to an optimal control by assigning its value to a
 * local FORTRAN variable. For indexing the array of controls, the <code>id
 * </code> of this <code>FortranCoder</code> with prefix "q" is used.
 * @author Samuel Bandara
 */
class ControlCoder extends FortranCoder {
	
	/**
	 * The libSBML object of the model entity this coder implements.
	 */
	private SbmlBase refObj;
	
	public final static byte CONSTANT = 1, PW_CONSTANT = 2;
	
	private byte discretize;
	
	/**
	 * @param param libSBML object of the model entity this coder implements
	 * @param scale scaling factor to be applied to this item of the array
	 * of controls
	 */
	public ControlCoder(SbmlBase obj, byte discretize, SbmlCompiler compiler) {
		super(compiler);
		refObj = obj;
		this.discretize = discretize;
	}
	
	void putFortranCode(FortranFunction target,
			Map<String, FortranCoder> bindings) throws SbmlCompilerException {
		String var = getVarName();
		target.declareVar(var);
		switch(discretize) {
		case CONSTANT:
			target.appendStatement(var +" = q(" + getId() + ")");
			break;
		case PW_CONSTANT:
			target.appendComment("DISCRETIZE1( " + var +
					", rwh, iwh )");
			break;
		default:
			throw new SbmlCompilerException(
					SbmlCompilerException.UNSUPPORTED_CONTROL_TYPE, this);
		}
	}
	
	protected void initialize(Map<String, FortranCoder> bindings) { }
	
	public SbmlBase getSbmlNode() { return refObj; }

	String getPrefix() {
		if (discretize == CONSTANT) { 
			return "q";
		}
		else return "u";
	}
}
