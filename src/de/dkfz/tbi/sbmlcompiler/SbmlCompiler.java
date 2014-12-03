/*
 *  SbmlCompiler  Copyright (C) 2005, 2014 Samuel Bandara
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.dkfz.tbi.sbmlcompiler;

import java.util.*;
import org.sbml.libsbml.*;

final public class SbmlCompiler {
	
	public final static int FFCN = 0, GFCN = 1, PLOTFCN = 2, MFCN1 = 3;
	
	/**
	 * Maximum number of characters in a line of FORTRAN code. If the number of
	 * characters exceeds <code>wrapLine</code>, the line is wrapped at the
	 * previous blank character. 
	 */
	public static final int wrapLine = 72;
	
	/**
	 * Number of differential states to be used for implementing a time-delayer
	 * tube.
	 */
	public static final int delaySteps = 5;
    
	/**
     * Dependency model of the <i>in vivo</i> situation. This dependency model
     * is created by the constructor of <code>SbmlCompiler</coder> while reading
     * an SBML file.
     */
    private Map<String, FortranCoder> inVivoBindings = new HashMap<String,
    		FortranCoder>();
    
    /**
     * The libSBML representation of the SBML model.
     */
    Model model;
    
    Model getModel() { return model; }
    
    /**
     * The libSBML reader used for loading the SBML file.
     */
    private SBMLReader reader;
    
    /**
     * The libSBML representation of the SBML file.
     */
    private SBMLDocument document;

    private String sbmlString;
    
    /**
     * Map from species to the reactions the species is involved in.
     */
    private HashMap<String, ArrayList<String>> reactome = new HashMap<String,
    		ArrayList<String>>();
    
    private void addToReactome(ListOf listOfSpecies, String reaction) {
		for (int k = 0; k < listOfSpecies.size(); k ++) {
			String species;
			SBase spec_ref = listOfSpecies.get(k); 
			species = ((SimpleSpeciesReference)spec_ref).getSpecies();
			if (! inVivoBindings.containsKey(species)) {
				ArrayList<String> reactions;
				if (reactome.containsKey(species)) {
					reactions = reactome.get(species);
				}
				else {
					reactions = new ArrayList<String>();
					reactome.put(species, reactions);
				}
				reactions.add(reaction);
			}
		}
    }
    
	/**
     * Builds the in vivo dependency model {@link SbmlCompiler#inVivoBindings}
     * from an SBML string or file. For each entity of the model, the correct
     * specialization of {@link FortranCoder} is identified and instantiated.
     * Each instance is put to the <code>inVivoBindings</code> map with the
     * <code>id</code> of the SBML entity it is coding for, serving as key.
     * @param sbml either an SBML string or a full file name of an SBML file
     * @param fromString wether <code>sbml</code> is an SBML string
     */
    public SbmlCompiler(String sbml, boolean fromString) throws
    		SbmlCompilerException {

    	System.loadLibrary("sbmlj");
    	
    	/* Makes libSBML read the model file sbml_file. If something goes wrong
    	 * an SbmlCompilerException CANNOT_READ_SBML is thrown.
    	 */
    	reader = new SBMLReader();
    	if (fromString) {
    		document = reader.readSBMLFromString(sbml);
    		this.sbmlString = sbml;
    	}
    	else {//from file
    		document = reader.readSBML(sbml);
    		this.sbmlString = document.toSBML();
    	}
		try {
			model = document.getModel();
			// if (model == null) throw new Exception();
    	}
    	catch (Exception e) {
    		throw new SbmlCompilerException(SbmlCompilerException
    				.CANNOT_READ_SBML, sbmlString);
    	}
    	
    	/* Walks the list of rules. Algebraic rules are stored in an ArrayList
    	 * for identifying algebraic states after other kinds of determination
    	 * have been checked. Assignment rules are implemented using EvalCoder.
    	 * For rate rules, DiffStateCoder is used.
    	 */
		ListOf listOfRules = model.getListOfRules();
		ArrayList<AlgStateCoder> algebraicEquations =
				new ArrayList<AlgStateCoder>();
		for (int i = 0; i < listOfRules.size(); i ++) {
			Rule rule = (Rule)listOfRules.get(i);
			if (rule instanceof AlgebraicRule) {
				algebraicEquations.add(new AlgStateCoder(rule.getMath(), this));
			}
			else {
				String v_name = null;
				FortranCoder coder = null;
				if (rule instanceof AssignmentRule) {
					v_name = ((AssignmentRule)rule).getVariable();
				}
				else {
					v_name = ((RateRule)rule).getVariable();
				}
				SBase ref_obj = model.getSpecies(v_name);
				if (ref_obj == null) {
					ref_obj = model.getParameter(v_name);
				}
				if (ref_obj == null) {
					ref_obj = model.getCompartment(v_name);
				}
				if (ref_obj == null) {
					throw new SbmlCompilerException(SbmlCompilerException
							.UNKNOWN_MODEL_ENTITY, v_name);
				}
				ASTNode expr = rule.getMath();
				if (rule instanceof AssignmentRule) {
					coder = new EvalCoder(ref_obj, expr, "asgn", false, this);
				}
				else {
					coder = new DiffStateCoder(ref_obj, expr, this);
				}
				inVivoBindings.put(v_name, coder);
			}
		}
		
		/* Walks the list of reactions for finding local parameters and for
		 * building the model's reactome involving reactants and products. This
		 * reactome is then used for deriving differential equations for the
		 * species. This is why modifiers are added to the reactome later, in
		 * a separate step, as by the definition of an enzyme, for modifiers,
		 * the reactions they are involved in do not matter.
		 */
    	ListOf listOfReactions = model.getListOfReactions();
		for (int i = 0; i < listOfReactions.size(); i ++) {
			Reaction reaction = (Reaction)listOfReactions.get(i);
			ListOf listOfParams=reaction.getKineticLaw().getListOfParameters();
			for (int k = 0; k < listOfParams.size(); k ++) {
				Parameter param = (Parameter)listOfParams.get(k);
				String name = param.getId();
				ParameterCoder param_coder = new ParameterCoder(param,
						param.getId(), this);
				inVivoBindings.put(name, param_coder);
			}
	    	String rxn_name = reaction.getId();
	    	addToReactome(reaction.getListOfReactants(), rxn_name);
	    	addToReactome(reaction.getListOfProducts(), rxn_name);
			EvalCoder rxn_coder = new EvalCoder(reaction, reaction
					.getKineticLaw().getMath(), "rxn", true, this);
			inVivoBindings.put(rxn_name, rxn_coder);
		}
		for (Iterator<String> i = reactome.keySet().iterator(); i.hasNext();) {
			String spec_name = i.next();
			Species species = model.getSpecies(spec_name);
			ArrayList<String> v_rxns = reactome.get(spec_name);
			String a_rxns[] = new String[v_rxns.size()];
			for (int k = 0; k < v_rxns.size(); k ++) {
				a_rxns[k] = (String)v_rxns.get(k);
			}
			DiffStateCoder diff_coder = new DiffStateCoder(species, a_rxns,
					this);
			inVivoBindings.put(spec_name, diff_coder);
		}
		for (int i = 0; i < listOfReactions.size(); i ++) {
			Reaction reaction = (Reaction)listOfReactions.get(i);
			addToReactome(reaction.getListOfModifiers(), reaction.getId());
		}
		
		HashSet<String> algebraicVariable = new HashSet<String>();
		/* Walks the list of global parameters. If the constant field is set to
		 * true, the parameter cannot be controlled by a rule, but is
		 * identifiable. If it is not constant and not put to the dependency
		 * model, already, it is a candidate for an algebraic state.
		 */
		ListOf listOfParams = model.getListOfParameters();
    	for (int i = 0; i < listOfParams.size(); i ++) {
    		Parameter param = (Parameter)listOfParams.get(i);
    		String param_name = param.getId();
    		idFromNameMap.put(param.getName(), param_name);
    		if (param.getConstant()) {
				ParameterCoder param_coder = new ParameterCoder(param,
						param.getName(), this);
    			inVivoBindings.put(param_name, param_coder);
    		}
    		else if (! inVivoBindings.containsKey(param_name)) {
    			algebraicVariable.add(param_name);
    		}
    	}
		
    	/* Walks the list of compartments. The same rules apply as for the list
    	 * of global parameters. The topology of the model is also built here.
    	 */
    	ListOf listOfVolumes = model.getListOfCompartments();
		for (int i = 0; i < listOfVolumes.size(); i ++) {
			Compartment volume = (Compartment)listOfVolumes.get(i);
			String vol_name = volume.getId();
			idFromNameMap.put(volume.getName(), vol_name);
			if (volume.getConstant()) {
				ConstantCoder const_coder = new ConstantCoder(volume, this);
				inVivoBindings.put(vol_name, const_coder);
			}
			else if (! inVivoBindings.containsKey(vol_name)) {
				algebraicVariable.add(vol_name);
			}
		}
		
    	/* Walks the list of species. The same rules apply as above, but it is
    	 * important to node that, as the in vivo situation is modeled here, a
    	 * reaction determation of a species can be overridden by a constant if
    	 * the species is declared constant.
    	 */
		ListOf listOfSpecies = model.getListOfSpecies();
		for (int i = 0; i < listOfSpecies.size(); i ++) {
			Species species = (Species)listOfSpecies.get(i);
			String spec_name = species.getId();
			idFromNameMap.put(species.getName(), spec_name);
			if (species.getConstant()) {
				ConstantCoder const_coder = new ConstantCoder(species, this);
				inVivoBindings.put(spec_name, const_coder);
			}
			else if (! inVivoBindings.containsKey(spec_name)) {
				algebraicVariable.add(spec_name);
			}
		}

		/* Assign algebraic variables to an appropriate algebraic equation. See
		 * external documentation for details on this algorithm.
		 */
		for (int i = 0; i < algebraicEquations.size(); i ++) {
			AlgStateCoder coder = algebraicEquations.get(i);
			HashSet<String> candidates = coder.getCandidates();
			candidates.retainAll(algebraicVariable);
			candidates.removeAll(inVivoBindings.keySet());
			if (candidates.isEmpty()) {
				throw new SbmlCompilerException(SbmlCompilerException
						.ALGEBRAIC_CONSTRAINT, null);
			}
			Iterator<String> k = candidates.iterator();
			while (k.hasNext()) {
				String candidate = k.next();
				boolean remove = false;
				if (! k.hasNext()) {
					remove = true;
				}
				boolean found = false;
				for (int n = i + 1; n < algebraicEquations.size(); n ++) {
					AlgStateCoder other = algebraicEquations
							.get(n);
					HashSet<String> others_candidates = other.getCandidates();
					if (others_candidates.contains(candidate)) {
						if (remove == true) {
							others_candidates.remove(candidate);
						}
						found = true;
					}
				}
				if ((! found) || (remove)) {
					inVivoBindings.put(candidate, coder);
					algebraicVariable.remove(candidate);
					break;
				}
			}
		}
		if (! algebraicVariable.isEmpty()) {
			throw new SbmlCompilerException(SbmlCompilerException
					.UNDEFINED_VOLATILE, algebraicVariable);
		}
		
		/* Adds pi and e which are constants defined by the SBML standard. As
		 * with all members of a dependency model, pi or e are included to the
		 * FORTRAN code, only, if they satisfy a dependency.
		 */
		if (! inVivoBindings.containsKey("pi")) {
    		inVivoBindings.put("pi", new ConstantCoder(3.14159, this));
    	}
    	if (! inVivoBindings.containsKey("e")) {
    		inVivoBindings.put("e", new ConstantCoder(2.71828, this));
    	}
    }

    public Map<String, FortranCoder> getInVivoBindings() {
    	Map<String, FortranCoder> clone = new HashMap<String, FortranCoder>();
    	clone.putAll(inVivoBindings);
    	return clone;
    }

    /**
     * Recursively visits the coders <code>parent</code> depends on, and makes
     * theses put their FORTRAN code to the <code>target</code> function of type
     * <code>visitor</code>. See external documentation for details on this
     * algorithm.
     * @param parent coder which has the children to be visited
     * @param bindings dependency model of the experiment
     * @param target target function where code is to be put
     * @param visitor type of the target function
     * @throws SbmlCompilerException
     */
    private void visitDependents(FortranCoder parent,
    		Map<String, FortranCoder> bindings, FortranFunction target,
    		int visitor) throws SbmlCompilerException {
    	HashSet<String> dependents = parent.getCodeDependencies();
    	parent.goodbye(visitor);
    	for (Iterator<String> i = dependents.iterator(); i.hasNext();) {
    		String name = i.next();
    		FortranCoder coder = bindings.get(name);
    		if (coder instanceof StateVariable) {
    			StateVariable statevar = (StateVariable)coder;
   				statevar.closeLoop(target, bindings, visitor);
    		}
    		else if (! coder.isVisited(visitor)) {
	    		visitDependents(coder, bindings, target, visitor);
	    		coder.putFortranCode(target, bindings);
    		}
    	}
    }
        
    private int n_visit_flags = 0;
    
    int getVisitFlagCount() {
    	return n_visit_flags;
    }
    
    /**
     * Generates FORTRAN code from the dependency model of an experiment.
     * @param bindings dependency model of the experiment
     * @param labels names to be given to the generated FORTRAN functions: ffcn
     * name, gfcn name, plotfcn name, names of the measurement functions ordered
     * as in <code>measurements</code> 
     * @param mfcns list of <code>EvalCoders</code> defining measurements
     * @param plotVars receives the identifier strings of volatile model
     * entities in the exact same order as written by the plot function
     * @return array of code each for a single FORTRAN function, in this order:
     * ffcn, gfcn, plot function, measurement functions in the order passed by
     * the caller
     * @throws SbmlCompilerException
     */
    public ArrayList<FortranFunction> compile(Map<String, FortranCoder>
    		bindings, String prefix, Set<String> mfcns) throws
    		SbmlCompilerException {
    	
    	ArrayList<FortranFunction> fn = new ArrayList<FortranFunction>();
    	fn.add(FFCN, new FortranFunction(prefix + "ffcn", "f"));
    	fn.add(GFCN, new FortranFunction(prefix + "gfcn", "g"));
    	fn.add(PLOTFCN, new  FortranFunction(prefix + "plot",
    			FortranFunction.PLOT));
    	n_visit_flags = 3 + mfcns.size();
    	for (Iterator<FortranCoder> k = bindings.values().iterator();
    			k.hasNext();) {
    		k.next().registerToFunction(fn);
    	}
    	sequence = new HashMap<String, Integer>();
		for (Iterator<String> i = mfcns.iterator(); i.hasNext();) {
			String fn_name = i.next();
			FortranCoder h_coder = bindings.get(fn_name);
			h_coder.fastPrepare(bindings);
			FortranFunction mfcn = new FortranFunction(prefix + fn_name,
					h_coder.getVarName());
			mfcn.outputs.add(h_coder);
			fn.add(mfcn);
		}
		Set<FortranCoder> entry = new HashSet<FortranCoder>();
 		for (int fn_id = 0; fn_id < fn.size(); fn_id ++) {
 			FortranFunction target = fn.get(fn_id);	
 			for (Iterator<FortranCoder> k = target.outputs.iterator();
 					k.hasNext();) {
 				FortranCoder coder = k.next();
 				if (! coder.isInitialized()) {
 					coder.init(bindings);
 				}
 				visitDependents(coder, bindings, target, fn_id);
 				coder.putFortranCode(target, bindings);
 				entry.add(coder);
 			}
		}
 		for (Iterator<FortranCoder> k = entry.iterator(); k.hasNext();) {
 			k.next().unprepare(bindings);
 		}
 		sequence = null;
    	return fn;
    }
    
    private Map<String, String> idFromNameMap = new HashMap<String, String>();
 
	/**
	 * Contains the prefixes as keys and <code>Integers</code> for counting
	 * as values.
	 */
	private HashMap<String, Integer> sequence = new HashMap<String,
			Integer>();

	/**
	 * Increments the counter for the given <code>prefix</code> if it
	 * already exists, or creates one, otherwise. 
	 * @param prefix prefix of a FORTRAN variable name
	 * @return unique number for each <code>prefix</code>, starting from one
	 * and incremented by one.
	 */
	int makeId(String prefix) {
			int id;
			if (sequence.containsKey(prefix)) {
				id = sequence.get(prefix).intValue() + 1;
			}
			else {
				id = 1;
			}
		sequence.put(prefix, new Integer(id));
		return id;
	}
	
	/**
	 * Returns the greatest thus last number returned for <code>prefix
	 * </code>. It is the number of variable names starting with <code>
	 * prefix</code>. 
	 * @param prefix prefix of a FORTRAN variable name
	 * @return number of variable names starting with <code>prefix</code>
	 */
	int getGreatestId(String prefix) {
		if (sequence.containsKey(prefix)) {
			return sequence.get(prefix).intValue();
		}
		return 0;
	}
    
    public String getIdFromName(String name) {
    	return idFromNameMap.get(name);
    }
    
	public String getSbmlString() {
		return sbmlString;
	}
	
	private static final String usageText = "usage: sbmlcompiler SBMLFILE.XML";
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println(usageText);
			return;
		}
		String sbml_file = args[0];
		try {
			SbmlCompiler cmplr = new SbmlCompiler(sbml_file, false);
			Map<String, FortranCoder> bnds = cmplr.getInVivoBindings();
			ArrayList<FortranFunction> fn = cmplr.compile(bnds, "",
					new HashSet<String>());
			System.out.println(fn.get(0).toString());
		}
		catch (SbmlCompilerException e) {
			System.out.println(e.getMessage());
		}
	}
}
