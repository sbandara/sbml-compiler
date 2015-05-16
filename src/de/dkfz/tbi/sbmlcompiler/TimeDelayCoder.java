package de.dkfz.tbi.sbmlcompiler;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode;
import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;
import de.dkfz.tbi.sbmlcompiler.sbml.SbmlBase;

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
	private AstNode my_entity;
	
	/**
	 * The time delay. This value can be a composite expression. Its units
	 * must be the standard time units used in this model.
	 */
	private AstNode my_delay;
	
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
	TimeDelayCoder(AstNode entity, AstNode delay, boolean onlyconc,
			SbmlCompiler compiler) {
		super(compiler);
		my_entity = entity;
		my_delay = delay;
		onlyConc = onlyconc;
	}

	void putFortranCode(FortranFunction target, Bindings bindings)
			throws SbmlCompilerException {
		String varname = getVarName();
		String nByDt = varname + "v";
		target.declareVar(nByDt);
		target.appendStatement(nByDt + " = " + SbmlCompiler.DELAY_STEPS + " / ("
				+ getFormula(my_delay, bindings, NodeType.AST_UNKNOWN) + ")");
		target.appendStatement("f(" + first_id + ") = ((" + getFormula(
				my_entity, bindings, NodeType.AST_UNKNOWN) + ")" +
				" - x(" + first_id + ")) * " + nByDt);
		for (int i = first_id + 1; i <= last_id; i ++) {
			target.appendStatement("f(" + i + ") = (x(" + (i - 1) + ") - x("
					+ i + ")) * " + nByDt);
		}
		target.declareVar(varname);
	}

	protected void initialize(Bindings bindings) throws SbmlCompilerException {
		super.initialize(bindings);
		findInnerDepends(my_entity, bindings, 0);
		findInnerDepends(my_delay, bindings, 0);
		first_id = compiler.makeId("xd");
		for (int i = 1; i < SbmlCompiler.DELAY_STEPS - 1; i ++) {
			compiler.makeId("xd");
		}
		last_id = compiler.makeId("xd");
	}

	String getPrefix() { return "dly"; }

	public SbmlBase getSbmlNode() { return null; }

	void putHeader(FortranFunction target, Bindings bindings) {
		target.appendStatement(getVarName() + " = x(" + last_id + ")");
	}

	int getTarget() { return SbmlCompiler.FFCN; }
}
