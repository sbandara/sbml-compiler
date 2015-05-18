package de.dkfz.tbi.sbmlcompiler;

import java.util.ArrayList;
import java.util.Iterator;

import de.dkfz.tbi.sbmlcompiler.sbml.*;
import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;
import de.dkfz.tbi.sbmlcompiler.sbml.Reaction.*;

/**
 * Implements the calculation of the derivative of a differential state, and
 * the extraction and rescaling of the differential state variable's value
 * from the arguments of a model function like FFCN or GFCN. The model
 * entity represented by this state can either be controlled by a rate rule,
 * or, if the model entity is a species, by a set of reactions.
 * @author Samuel Bandara
 */
class DiffStateCoder extends StateVariable {
	
	/**
	 * The libSBML object of the model entity this coder represents. 
	 */
	private SbmlBase ref_obj = null;
	
	/**
	 * Array of the names of the reactions this species is involved in as a
	 * reactant or product. If this array exists, <code>my_diffeq</code>
	 * must be <code>null</code>.
	 */
	private String[] reactions = null;
	private final ArrayList<EvalCoder> st_coder; 
	
	/**
	 * Differential expression of the rate rule controlling this entity of
	 * the model. If <code>my_diffeq</code> exists, <code>my_reaction</code>
	 * must be <code>null</code>.
	 */
	private AstNode diffeq = null;
	
	/**
	 * @param obj libSBML object of the species this coder represents
	 * @param reaction array of the names of the reactions this species is
	 * involved in as a reactant or product
	 */
	DiffStateCoder(SbmlBase obj, String[] reactions, SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		this.reactions = reactions;
		st_coder = new ArrayList<EvalCoder>();
	}
	
	/**
	 * @param obj libSBML object of the model entity this coder represents
	 * @param diffeq abstract syntax tree of the rate rule
	 */
	DiffStateCoder(SbmlBase obj, AstNode diffeq, SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		this.diffeq = diffeq;
		st_coder = null;
	}
	
	void putHeader(FortranFunction target, Bindings bindings) {
		String name = getVarName();
		target.declareVar(name);
		String code = name + " = x(" + getId() + ")";
		target.appendStatement(code);
	}
	
	private void evaluateReaction(FortranFunction target, StringBuilder code,
			Bindings bindings) throws SbmlCompilerException {
		Species species = (Species) ref_obj;
		if (species.getInitialAmount() == null) {
			code.append('(');
		}
		for (int k = 0; k < reactions.length; k ++) {
			FortranCoder coder = null;
			coder = bindings.get(reactions[k]);
			if ((coder == null) || (coder instanceof ConstantCoder)) {
				code.append("+ 0 ");
			}
			else {
				Reaction rxn = (Reaction) coder.getSbmlNode();
				Iterator<SpeciesReference> it = rxn.getSpecies().iterator();
				while (it.hasNext()) {
					SpeciesReference ref = it.next();
					if (ref.getSpecies() != species) {
						continue;
					}
					ReferenceType ref_type = ref.getRefType();
					if (ref_type == ReferenceType.PRODUCT) {
						code.append("+ ");
					}
					else if (ref_type == ReferenceType.REACTANT) {
						code.append("- ");
					}
					Integer stoichiometry = ref.getStoichiometry();
					if (stoichiometry == null) {
						String st_var = st_coder.remove(0).getVarName();
						code.append(st_var).append(" * ");
					}
					else if (stoichiometry != 1.0) {
						code.append(stoichiometry).append(" * ");
					}
					code.append(coder.getVarName()).append(' ');
				}
			}
		}
		if (species.getInitialAmount() == null) {
			FortranCoder vc = bindings.get(species.getCompartment()
					.getId());
			code.append(')');
			if (! ((vc instanceof ConstantCoder) && (((ConstantCoder) vc)
					.getValue() == 1))) {
				code.append(" / ").append(vc.getVarName());
			}
		}
	}
	
	/**
	 * Sums up the kinetics of each reaction the species represented by
	 * this coder is involved in, or implements the rate rule controlling
	 * the model entity represented by this coder. If reactions control the
	 * quantitative change of of a species, consuming kinetics, each
	 * multiplied by stochiometry, are subtracted from producing kinetics
	 * multiplied by stochiometry, too, and, if the species quantity is
	 * given in concentration units, the resulting amount is divided by the
	 * volume of the compartment the species is located in.
	 */
	void putFortranCode(FortranFunction target, Bindings bindings)
			throws SbmlCompilerException {
		StringBuilder code = new StringBuilder("f(").append(getId())
				.append(") = ");
		if (reactions != null) {
			evaluateReaction(target, code, bindings);
		}
		else if (diffeq != null) {
			code.append(getFormula(diffeq, bindings, NodeType.AST_UNKNOWN));
		}
		else throw new InternalError();
		target.appendStatement(code.toString());
	}
	
	void initialize(Bindings bindings) throws SbmlCompilerException {
		super.initialize(bindings);
		if (reactions != null) {
			Species species = (Species) ref_obj;
			for (int k = 0; k < reactions.length; k ++) {
				addDepend(reactions[k]);
				Reaction rxn = (Reaction) bindings.get(reactions[k])
						.getSbmlNode();
				Iterator<SpeciesReference> it = rxn.getSpecies().iterator();
				while (it.hasNext()) {
					SpeciesReference ref = it.next();
					AstNode st_math = ref.getStochiometryMath();
					if ((st_math != null) && (ref.getSpecies() == species)) {
						EvalCoder eval_coder = new EvalCoder(null, st_math,
								"st", false, compiler);
						String st_sym = eval_coder.fastPrepare(bindings);
						addDepend('#' + st_sym);
						st_coder.add(eval_coder);
					}
				}
			}
			if (species.getInitialAmount() == null) {
				addDepend(species.getCompartment().getId());
			}
		}
		else {
			findInnerDepends(diffeq, bindings, 0);
		}
	}
	
	public SbmlBase getSbmlNode() { return ref_obj; }
	
	String getPrefix() { return "xd"; }
	
	int getTarget() { return SbmlCompiler.FFCN; }
	
	void registerToFunction(ArrayList<FortranFunction> code) {
		code.get(SbmlCompiler.FFCN).outputs.add(this);
	}
}
