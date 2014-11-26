package de.dkfz.tbi.sbmlcompiler;

import java.util.Map;

import org.sbml.libsbml.ASTNode;
import org.sbml.libsbml.SBase;
import org.sbml.libsbml.libsbml;

/**
 * Workaround to support time-delayed equations. See external documentation
 * for details on the mathematics applied. Diffusion across a membrane
 * occurs from the past value referred to, which can be composite, into the
 * first compartment of the time-delayer tube. Each compartment of that tube
 * corresponds to a hidden differential state. The speed of diffusion is
 * calculated once in a call. The header assignment extracts the past value
 * from the last compartment of the time-delayer tube. 
 * @author Samuel Bandara
 */
class TimeDelayCoder extends StateVariable {
	
	/**
	 * The past value referred to in this time-delayed function. This value
	 * can be a composite expression.
	 */
	private ASTNode my_entity;
	
	/**
	 * The time delay. This value can be a composite expression. Its units
	 * must be the standard time units used in this model.
	 */
	private ASTNode my_delay;
	
	/**
	 * Index of the first differential state used for implementing the
	 * time-delayer tube.
	 */
	private int first_id;
	
	/**
	 * Index of the last differential state used for implementing the
	 * time-delayer tube.
	 */
	private int last_id;
	
	/**
	 * @param entity expression for the past value referred to in this
	 * time-delayed function
	 * @param delay expression for the length of delay
	 * @param onlyconc wether quantities of species must be transformed to
	 * concentrations if amounts are given
	 */
	TimeDelayCoder(ASTNode entity, ASTNode delay, boolean onlyconc,
			SbmlCompiler compiler) {
		super(compiler);
		my_entity = entity;
		my_delay = delay;
		onlyConc = onlyconc;
	}

	void putFortranCode(FortranFunction target,
			Map<String, FortranCoder> bindings) throws SbmlCompilerException {
		String varname = getVarName();
		String nByDt = varname + "v";
		target.declareVar(nByDt);
		target.appendStatement(nByDt + " = " + SbmlCompiler.delaySteps + " / ("
				+ getFormula(my_delay, bindings, libsbml.AST_UNKNOWN) + ")");
		target.appendStatement("f(" + first_id + ") = ((" + getFormula(
				my_entity, bindings, libsbml.AST_UNKNOWN) + ")" +
				" - x(" + first_id + ")) * " + nByDt);
		for (int i = first_id + 1; i <= last_id; i ++) {
			target.appendStatement("f(" + i + ") = (x(" + (i - 1) + ") - x("
					+ i + ")) * " + nByDt);
		}
		target.declareVar(varname);
	}

	protected void initialize(Map<String, FortranCoder> bindings) throws
			SbmlCompilerException {
		super.initialize(bindings);
		findInnerDepends(my_entity, bindings, 0);
		findInnerDepends(my_delay, bindings, 0);
		first_id = compiler.makeId("xd");
		for (int i = 1; i < SbmlCompiler.delaySteps - 1; i ++) {
			compiler.makeId("xd");
		}
		last_id = compiler.makeId("xd");
	}

	String getPrefix() { return "dly"; }

	public SBase getSbmlNode() { return null; }

	void putHeader(FortranFunction target, Map<String, FortranCoder> bindings) {
		target.appendStatement(getVarName() + " = x(" + last_id + ")");
	}

	int getTarget() { return SbmlCompiler.FFCN; }
}
