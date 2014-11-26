package de.dkfz.tbi.sbmlcompiler;

import java.util.ArrayList;
import java.util.Map;

import org.sbml.libsbml.ASTNode;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.SBase;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;
import org.sbml.libsbml.libsbml;

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
	private SBase ref_obj = null;
	
	/**
	 * Array of the names of the reactions this species is involved in as a
	 * reactant or product. If this array exists, <code>my_diffeq</code>
	 * must be <code>null</code>.
	 */
	private String my_reaction[] = null;
	
	/**
	 * Differential expression of the rate rule controlling this entity of
	 * the model. If <code>my_diffeq</code> exists, <code>my_reaction</code>
	 * must be <code>null</code>.
	 */
	private ASTNode my_diffeq = null;

	/**
	 * @param obj libSBML object of the species this coder represents
	 * @param reaction array of the names of the reactions this species is
	 * involved in as a reactant or product
	 */
	DiffStateCoder(SBase obj, String reaction[], SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		my_reaction = reaction;
	}
	
	/**
	 * @param obj libSBML object of the model entity this coder represents
	 * @param diffeq abstract syntax tree of the rate rule
	 */
	DiffStateCoder(SBase obj, ASTNode diffeq, SbmlCompiler compiler) {
		super(compiler);
		ref_obj = obj;
		my_diffeq = diffeq;
	}
	
	void putHeader(FortranFunction target, Map<String, FortranCoder> bindings) {
		String name = getVarName();
		target.declareVar(name);
		String code = name + " = x(" + getId() + ")";
		target.appendStatement(code);
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
	void putFortranCode(FortranFunction target,
			Map<String, FortranCoder> bindings) throws SbmlCompilerException {
		String code = "f(" + getId() + ") = ";
		if (my_reaction != null) {
			Species species = (Species)ref_obj;
			if (! species.isSetInitialAmount()) {
				code += "(";
			}
			String spec_name = species.getId();
			for (int i = 0; i < my_reaction.length;) {
				FortranCoder coder = null;
				coder = bindings.get(my_reaction[i]);
				if ((coder == null) || (coder instanceof ConstantCoder)) {
					code += "+ 0 ";
				}
				else {
					Reaction rxn = (Reaction)coder.getSbmlNode();
					ListOf listOfSpecRef = rxn.getListOfProducts();
					SpeciesReference specRef = null;
					for (int k = 0; k < rxn.getNumProducts(); k ++) {
						SpeciesReference s_ref = (SpeciesReference)
								listOfSpecRef.get(k);
						if (s_ref.getSpecies().equals(spec_name)) {
							code += "+ ";
							specRef = s_ref;
							break;
						}
					}
					if (specRef == null) {
						listOfSpecRef = rxn.getListOfReactants();
						for (int k = 0; k < rxn.getNumReactants(); k ++) {
							SpeciesReference s_ref = (SpeciesReference)
									listOfSpecRef.get(k);
							if (s_ref.getSpecies().equals(spec_name)) {
								code += "- ";
								specRef = s_ref;
								break;
							}
						}
					}
					if (specRef.getStoichiometry() != 1) {
						code += Double.toString(specRef.getStoichiometry())
								+ " * ";
					}
					code += coder.getVarName();
					if (++ i < my_reaction.length) {
						code += " ";
					}
				}
			}
			if (! species.isSetInitialAmount()) {
				FortranCoder vc = bindings.get(species.getCompartment());
				if (! ((vc instanceof ConstantCoder) && (((ConstantCoder)vc)
						.getValue() == 1))) {
					code += ") / " + vc.getVarName();
				}
			}
		}
		else if (my_diffeq != null) {
			code += getFormula(my_diffeq, bindings, libsbml.AST_UNKNOWN);
		}
		else throw new InternalError();
		target.appendStatement(code);
	}
	
	protected void initialize(Map<String, FortranCoder> bindings)
			throws SbmlCompilerException {
		super.initialize(bindings);
		if (my_reaction != null) {
			for (int i = 0; i < my_reaction.length; i ++) {
				if (bindings.containsKey(my_reaction[i])) {
					addDepend(my_reaction[i]);
				}
			}
			Species species = (Species)ref_obj;
			if (! species.isSetInitialAmount()) {
				addDepend(species.getCompartment());
			}
		}
		else {
			findInnerDepends(my_diffeq, bindings, 0);
		}
	}
	
	public SBase getSbmlNode() { return ref_obj; }

	String getPrefix() { return "xd"; }

	int getTarget() { return SbmlCompiler.FFCN; }
	
	void registerToFunction(ArrayList<FortranFunction> code) {
		code.get(SbmlCompiler.FFCN).outputs.add(this);
	}
}
