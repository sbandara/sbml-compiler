/*
 *  SbmlCompiler  Copyright (C) 2005, 2014-2015 Samuel Bandara
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static de.dkfz.tbi.sbmlcompiler.SbmlCompilerException.*;
import de.dkfz.tbi.sbmlcompiler.sbml.*;
import de.dkfz.tbi.sbmlcompiler.sbml.Rule.RuleType;

final public class SbmlCompiler {
	
	public final static int FFCN = 0, GFCN = 1, PLOTFCN = 2, MFCN1 = 3;
	public static final int WRAP_LINE = 72, DELAY_STEPS = 5;
    
    private Bindings def_bindings = new Bindings();
    private Model model;
    
    private ArrayList<AlgStateCoder> readRules() throws SbmlCompilerException {
    	final int n_rules = model.getNumRules();
    	ArrayList<AlgStateCoder> alg_eqns = new ArrayList<AlgStateCoder>();
    	for (int k = 0; k < n_rules; k ++) {
    		Rule rule = model.getRule(k);
    		if (rule.getType() == RuleType.ALGEBRAIC_RULE) {
    			alg_eqns.add(new AlgStateCoder(rule.getExpression(), this));
    		}
    		else {
    			SbmlBase object = rule.getVariable();
    			FortranCoder coder = null;
    			if (rule.getType() == RuleType.ASSIGNMENT) {
    				coder = new EvalCoder(object, rule.getExpression(), "asgn",
    						false, this);
    			}
    			else if (rule.getType() == RuleType.RATE_RULE) {
    				coder = new DiffStateCoder(object, rule.getExpression(),
    						this);
    			}
				def_bindings.put(object.getId(), coder);
    		}
    	}
    	return alg_eqns;
    }
    
    private void resolveAlgebraics(ArrayList<AlgStateCoder> alg_eqns,
    		HashSet<String> alg_vars) throws SbmlCompilerException {
		for (int i = 0; i < alg_eqns.size(); i ++) {
			AlgStateCoder coder = alg_eqns.get(i);
			HashSet<String> candidates = coder.getCandidates();
			candidates.retainAll(alg_vars);
			candidates.removeAll(def_bindings.getIds());
			if (candidates.isEmpty()) {
				throw new SbmlCompilerException(ALGEBRAIC_CONSTRAINT, null);
			}
			Iterator<String> k = candidates.iterator();
			while (k.hasNext()) {
				String candidate = k.next();
				boolean remove = false;
				if (! k.hasNext()) {
					remove = true;
				}
				boolean found = false;
				for (int n = i + 1; n < alg_eqns.size(); n ++) {
					AlgStateCoder other = alg_eqns.get(n);
					HashSet<String> others_candidates = other.getCandidates();
					if (others_candidates.contains(candidate)) {
						if (remove == true) {
							others_candidates.remove(candidate);
						}
						found = true;
					}
				}
				if ((! found) || (remove)) {
					def_bindings.put(candidate, coder);
					alg_vars.remove(candidate);
					break;
				}
			}
		}
		if (! alg_vars.isEmpty()) {
			throw new SbmlCompilerException(UNDEFINED_VOLATILE, alg_vars);
		}
    }
    
    public SbmlCompiler(InputStream is) throws SbmlCompilerException {
    	ModelParser parser = new ModelParser();
    	try {
    		model = parser.parse(is);
    	}
    	catch (Exception e) {
    		throw new SbmlCompilerException(CANNOT_READ_SBML, e);
    	}
		ArrayList<AlgStateCoder> alg_eqns = readRules();
    	HashSet<String> alg_vars = new HashSet<String>();
    	Iterator<String> it = model.getEntityIds().iterator();
    	while (it.hasNext()) {
    		String obj_id = it.next();
    		if (def_bindings.getIds().contains(obj_id)) {
    			continue;
    		}
       		SbmlBase object = model.getEntity(obj_id);
    		if (object.isConstant()) {
    			FortranCoder coder = null;
    			if (object instanceof Parameter) {
    				coder = new ParameterCoder(object, obj_id, this);
    			}
    			else {
    				coder = new ConstantCoder(object, this);
    			}
				def_bindings.put(obj_id, coder);
    		}
    		else if (object instanceof Species) {
    			String[] rxns = model.getEntityIds().stream().map(r -> model
    					.getEntity(r)).filter(r -> r instanceof Reaction)
    					.map(r -> r.getId()).toArray(size -> new String[size]);
    			if (rxns.length == 0) {
    				alg_vars.add(obj_id);
    			}
    			else {
    				def_bindings.put(obj_id, new DiffStateCoder(object, rxns,
    						this));
    			}
    		}
    		else if (object instanceof Reaction) {
    			def_bindings.put(obj_id, new EvalCoder(object, ((Reaction)
    					object).getKineticLaw(), "rxn", true, this));
    		}
    		else if ((object instanceof Parameter) || (object instanceof
    				Compartment)) {
    			alg_vars.add(obj_id);
    		}
    	}
    	Set<String> id_set = def_bindings.getIds();
		if (! id_set.contains("pi")) {
    		def_bindings.put("pi", new ConstantCoder(3.14159, this));
    	}
    	if (! id_set.contains("exponentiale")) {
    		def_bindings.put("exponentiale", new ConstantCoder(2.71828, this));
    	}
		resolveAlgebraics(alg_eqns, alg_vars);
    }
    
    public Model getModel() { return model; }
    
    public Bindings getDefaultBindings() {
    	return def_bindings.clone();
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
    private void visitDependents(FortranCoder parent, Bindings bindings,
    		FortranFunction target, int visitor) throws SbmlCompilerException {
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
    
    int getVisitFlagCount() { return n_visit_flags; }
    
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
    public ArrayList<FortranFunction> compile(Bindings bindings, String prefix,
    		HashSet<String> mfcns) throws SbmlCompilerException {
    	ArrayList<FortranFunction> fn = new ArrayList<FortranFunction>();
    	fn.add(FFCN, new FortranFunction(prefix + "ffcn", "f"));
    	fn.add(GFCN, new FortranFunction(prefix + "gfcn", "g"));
    	fn.add(PLOTFCN, new  FortranFunction(prefix + "plot",
    			FortranFunction.PLOT));
    	n_visit_flags = 3 + mfcns.size();
    	for (Iterator<FortranCoder> k = bindings.getCoders().iterator();
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
		HashSet<FortranCoder> entry = new HashSet<FortranCoder>();
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
    
    /**
	 * Contains the prefixes as keys and <code>Integers</code> for counting
	 * as values.
	 */
	private HashMap<String, Integer> sequence = new HashMap<String, Integer>();
	
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
			id = sequence.get(prefix) + 1;
		}
		else {
			id = 1;
		}
		sequence.put(prefix, id);
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
			return sequence.get(prefix);
		}
		return 0;
	}
}
