package de.dkfz.tbi.sbmlcompiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import de.dkfz.tbi.sbmlcompiler.sbml.*;
import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;

/**
 * Implements the calculation of the residual of an algebraic equation, and
 * the extraction and rescaling of the algebraic state variable's value from
 * the arguments of a model function like FFCN or GFCN. During construction,
 * the algebraic state variable is not yet identified. The state variable is
 * selected externally by {@link SbmlCompiler#assignAlgStates}.
 * @author Samuel Bandara
 */
class AlgStateCoder extends StateVariable {
 	
	/**
	 * Right-hand side of the algebraic equation this algebraic state was
	 * assigned to.
	 */
	private AstNode my_algeq;
	
	/**
	 * Set of variable names found in this algebraic equation. {@link
	 * SbmlCompiler#assignAlgStates} rules out one after another until one
	 * variable takes the role of the corresponding algebraic state.
	 */
	private HashSet<String> candidates = new HashSet<String>();
	
	/**
	 * Returns the set of variable names found in this algebraic equation. After
	 * algebraic states have been assigned, this set contains the algebraic
	 * state variable as its sole element.
	 * @return set containing the variable names found in this algebraic
	 * equation, or, if other candidates have been ruled out, the name of the
	 * algebraic state variable 
	 */
	protected HashSet<String> getCandidates() {
		return candidates;
	}
	
	/**
	 * Identifies candidates for the role of the algebraic state variable,
	 * recursively.
	 */
	private void findStateCandidates(AstNode root) {
		if ((root.isName()) && (root.getType() != NodeType.AST_NAME_TIME)) {
			if (! root.isFunction()) {
				candidates.add(root.getName());
			}
		}
		else {
    		for (int i = 0; i < root.getNumChildren(); i ++) {
    			findStateCandidates(root.getChild(i));
    		}
		}
	}
	
	/**
	 * @param algeq right-hand side of the algebraic equation
	 */
	AlgStateCoder(AstNode algeq, SbmlCompiler compiler) {
		super(compiler);
		my_algeq = algeq;
		findStateCandidates(my_algeq);
	}

	void putFortranCode(FortranFunction target,
			Map<String, FortranCoder> bindings) throws SbmlCompilerException {
		String code = "g(" + getId() + ") = ";
		code += getFormula(my_algeq, bindings, NodeType.AST_UNKNOWN);
		target.appendStatement(code);
	}
	
	protected void initialize(Map<String, FortranCoder> bindings)
			throws SbmlCompilerException {
		super.initialize(bindings);
		findInnerDepends(my_algeq, bindings, 0);
	}
	
	public SbmlBase getSbmlNode() { return null; }

	String getPrefix() { return "xa"; }

	void putHeader(FortranFunction target, Map<String, FortranCoder>bindings) {
		String name = getVarName();
		target.declareVar(name);
		String code = name + " = x(" + (getId() +
				compiler.getGreatestId("xd")) + ")";
		target.appendStatement(code);
	}

	int getTarget() {
		return SbmlCompiler.GFCN;
	}

	void registerToFunction(ArrayList<FortranFunction> code) {
		code.get(SbmlCompiler.GFCN).outputs.add(this);
	}
}
