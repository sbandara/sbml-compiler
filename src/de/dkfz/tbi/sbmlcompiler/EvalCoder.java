package de.dkfz.tbi.sbmlcompiler;

import java.util.Map;

import org.sbml.libsbml.ASTNode;
import org.sbml.libsbml.SBase;
import org.sbml.libsbml.libsbml;

/**
 * Evaluates simple mathematical expressions. <code>EvalCoder</code> only
 * minimally implements the abstract methods of <code>FortranCoder</code>,
 * but allows for free choice of the prefix of the variable name. It mainly
 * serves for implementing the evaluation of kinetic laws and assignment
 * rules, and for evaluation tasks delegated from other coders. 
 * @author Samuel Bandara
 */
class EvalCoder extends FortranCoder {
	
	/**
	 * Abstract syntax tree of the expression to be evaluated.
	 */
	private ASTNode funcDef;
	
	/**
	 * The libSBML object of the model entity this coder represents. 
	 */
	private SBase ref_obj;
	
	/**
	 * Prefix to be used when generating a FORTRAN variable name.
	 */
	private String my_prefix;
	
	/**
	 * @param obj libSBML object of the model entity this coder represents
	 * or <code>null</code> if none 
	 * @param function abstract syntax tree of the expression to be
	 * evaluated
	 * @param prefix prefix to be chosen when generating a FORTRAN variable
	 * name
	 * @param onlyconc whether quantities of species must be transformed to
	 * concentrations if amounts are given
	 */
	public EvalCoder(SBase obj, ASTNode function, String prefix, boolean
			onlyconc, SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		funcDef = function;
		my_prefix = prefix;
		onlyConc = onlyconc;
	}
	
	void putFortranCode(FortranFunction target,
			Map<String, FortranCoder> bindings) throws SbmlCompilerException {
		String name = getVarName();
		target.declareVar(name);
		String formula = getFormula(funcDef, bindings, libsbml.AST_UNKNOWN);
		String assignment = name + " = " + formula;
		target.appendStatement(assignment);
	}

	protected void initialize(Map<String, FortranCoder> bindings)
			throws SbmlCompilerException {
		findInnerDepends(funcDef, bindings, 0);
	}
	
	public SBase getSbmlNode() { return ref_obj; }

	String getPrefix() { return my_prefix; }
}
