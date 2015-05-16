package de.dkfz.tbi.sbmlcompiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import de.dkfz.tbi.sbmlcompiler.sbml.*;
import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;

/**
 * Element of a FORTRAN function. A <code>FortranCoder</code> generates a
 * piece of FORTRAN code that implements the task this coder is responsible
 * for. It must specify the names of all other coders that must have put
 * down their piece of code this <code>FortranCoder</code> relys on. The
 * <code>SbmlCompiler</code> then guarantees that all its dependencies are
 * satisfied when {@link de.dkfz.tbi.sbmlcompiler.FortranCoder#putFortranCode
 * } is invoked on this coder. Often, a <code>FortranCoder</code> represents
 * an entity of the model.
 * @author Samuel Bandara
 */
public abstract class FortranCoder {
	
	/**
	 * Reference to the <code>SbmlCompiler</code> that coordinates this
	 * compilation run.
	 */
	final protected SbmlCompiler compiler;
	
	FortranCoder(SbmlCompiler compiler) {
		this.compiler = compiler;
	}
	
	/**
	 * Name of the FORTRAN variable that is used for storing the results of
	 * the task this coder is responsible for. For example, evaluations of
	 * a mathematical expression assign the value to this variable.
	 */
	private String varName = null;
	
	/**
	 * Number created by {@link SbmlCompiler.IdGenerator}, starting from one
	 * and incrementing, that is used for generating unique variable names.
	 * <code>id</code> is unique for each prefix. 
	 */
	private int id;
	
	/**
	 * Wether quantities of species that are expressed as amounts must be
	 * transformed to concentrations when used in mathematical expressions
	 * of this coder. SBML kinetic laws always refer to concentrations, even
	 * if the quantity of the species is defined as an amount. 
	 */
	protected boolean onlyConc = false;
	
	/**
	 * Returns the <code>id</code> of the coder, which is unique for each
	 * prefix of FORTRAN variable names. The <code>id</code> is used to
	 * index algebraic states, differential states, and controls.  
	 * @return suffix number of this <code>FortranCoder</code>
	 */
	protected final int getId() { return id; };
	
	/**
	 * Contains the name of each FortranCoder this coder depends on. This
	 * set is expected to be complete when the call to {@link SbmlCompiler
	 * .FortranCoder#initialize} has returned. {@link SbmlCompiler
	 * .FortranCoder#findInnerDepends} automatically identifies the
	 * dependencies of mathematical expressions and stores them in this set.
	 */
	private final HashSet<String> depends = new HashSet<String>();
	
	/**
	 * Adds the name of <code>FortranCoders</code> to the set of
	 * dependencies of this coder. If the dependency is a species that uses
	 * amounts as quantity, and if only concentrations may be used in
	 * mathematical expressions, the volume of the species' compartment is
	 * also added to the set.
	 * @param dependency name of a FortranCoder to be added to the set of
	 * dependencies  
	 */
	final void addDepend(String dependency) throws SbmlCompilerException {
		depends.add(dependency);
		if (onlyConc) {
			SbmlBase entity = compiler.getModel().getEntity(dependency);
			if (entity instanceof Species) {
				Species species = (Species) entity;
				if (species.getInitialAmount() != null) {
					depends.add(species.getCompartment().getId());
				}
			}
		}
	}
	
	/**
	 * Contains the FORTRAN variable names of subtasks that were delegated
	 * to other <code>FortranCoders</code>, for example evaluations of
	 * function calls. During initialization, {@link SbmlCompiler
	 * .FortranCoder#findInnerDepends} stores the variable names of these
	 * coders in this <code>callQueue</code>. While generating FORTRAN code,
	 * {@link SbmlCompiler.FortranCoder#findInnerDepends} takes am item from
	 * this <code>callQueue</code> whenever a delegated subtask is to be
	 * referenced. 
	 */
	private final ArrayList<String> callQueue = new ArrayList<String>();
	
	/**
	 * Index pointer for accessing members of the callQueue. It is used
	 * instead of popping entries from the queue, because multiple rounds of
	 * compilation are supported. 
	 */
	private int queueIndex;
	
	/**
	 * Wether this <code>FortranCoder</code> has implemented its task in the
	 * FORTRAN function indicated by the index.
	 */
	private boolean visited[];
	
	/**
	 * Wether this <code>FortranCoder</code> has been initialized.
	 */
	private boolean initialized;

	/**
	 * Returns wether this <code>FortranCoder</code> has been initialized.
	 * @return wether this <code>FortranCoder</code> has been initialized
	 */
	final boolean isInitialized() { return initialized; }
	
	/**
	 * Writes FORTRAN code to the {@link SbmlCompiler.FortranFunction}
	 * <code>target</code> in order to satisfy the dependency of other
	 * <code>FortranCoder</code> objects that rely on this one, or to
	 * generate an output of the <code>target</code> function. The caller
	 * guarantees that all <code>FortranCoders</code> this coder has
	 * declared as its dependencies, have written their FORTRAN code to the
	 * <code>target</code> when this method is invoked.
	 * @param target FORTRAN function into which code must be written
	 * @param bindings dependency model of the experiment
	 * @throws SbmlCompiler.SbmlCompilerException
	 */
	abstract void putFortranCode(FortranFunction target, Bindings bindings)
			throws SbmlCompilerException;
	
	/**
	 * Finds a name for this coder, invokes <code>initialize</code> on this
	 * coder, and recursively initializes the coders this coder depends on.
	 * <code>init</code> is called by the <code>SbmlCompiler</code> right
	 * before compilation. A unique FORTRAN variable name is generated based
	 * on the prefix supplied by the implementation of {@link SbmlCompiler
	 * .FortranCoder#getPrefix}. This variable name is globally accessible
	 * through the {@link SbmlCompiler.FortranCoder#getVarName} method.
	 * Here, the implementation of {@link SbmlCompiler.FortranCoder
	 * #initialize} is invoked, which must add the names of all <code>
	 * FortranCoder</code> objects this coder depends on, to the set {@link
	 * SbmlCompiler.FortranCoder#depends}. That set is then used to
	 * initialize these objects recursively.
	 * @param bindings dependency model of the experiment
	 * @param id_gen <code>IdGenerator</code> to pull a unique FORTRAN
	 * variable index from
	 * @throws SbmlCompiler.SbmlCompilerException
	 */
	final void init(Bindings bindings) throws SbmlCompilerException {
		String prefix = getPrefix();
		id = compiler.makeId(prefix);
		varName = prefix + id;
		depends.clear();
		callQueue.clear();
		queueIndex = 0;
		visited = new boolean[compiler.getVisitFlagCount()];
		initialized = true;
		initialize(bindings);
		for (Iterator<String> k = depends.iterator(); k.hasNext();) {
			String name = k.next();
			FortranCoder coder = bindings.get(name);
			if (coder == null) {
				throw new InternalError("Did not find coder object for id " +
						name);
			}
			if (! coder.isInitialized()) {
				coder.init(bindings);
			}
		}
	}
	
	/**
	 * Finds out which other <code>FortranCoders</code> this coder depends
	 * on, and adds their names to the set of dependencies. Where
	 * mathematical expressions represented by ASTs are involved, it is
	 * strongly recommended to use the method {@link SbmlCompiler
	 * .FortranCoder#findInnerDepends} for this. It is legal to instantiate
	 * other <code>FortranCoders</code> and to insert them into the
	 * dependency model <code>bindings</code>, for example for delegating
	 * subtasks, or to replace other <code>FortranCoders</code>.
	 * @param bindings dependency model of the experiment
	 * @param id_gen <code>IdGenerator</code> to pull a unique FORTRAN
	 * variable index from
	 * @throws SbmlCompilerException
	 */
	protected abstract void initialize(Bindings bindings)
			throws SbmlCompilerException;
	
	/**
	 * @return prefix of the unique FORTRAN variable name to be created for
	 * this coder
	 */
	abstract String getPrefix();
	
	/**
	 * Generates the dependency list from an AST representing a mathematical
	 * expression. This method should be used whenever dealing with
	 * mathematical expressions represented by ASTs. As it cooperates with
	 * {@link SbmlCompiler.FortranCoder#getFormula}, this method must be
	 * invoked from {@link SbmlCompiler.FortranCoder#initialize} if later,
	 * during the <code>putFortranCode</code> call, <code>getFormula</code>
	 * is used to transform the AST <code>root</code> to a mathematical
	 * expression in FORTRAN code.
	 * @param root mathematical expression represented by an AST
	 * @param bindings dependency model of the experiment
	 * @param j 
	 */
	final void findInnerDepends(AstNode root, Bindings bindings, int j)
			throws SbmlCompilerException {
		NodeType type = root.getType();
		Model model = compiler.getModel();
		if ((root.isFunction()) && (type != NodeType.AST_FUNCTION_ROOT) &&
				(type != NodeType.AST_FUNCTION_POWER)) {
			String fn_name = root.getName();
			FortranCoder fn_coder = null;
			if (type == NodeType.AST_FUNCTION_DELAY) {
				AstNode reference = root.getChild(0);
				AstNode delay = root.getChild(1);
				fn_coder = new TimeDelayCoder(reference, delay, onlyConc,
						compiler);
			}
			else {
				Function fn_def = (Function) model.getEntity(fn_name);
				if (fn_def != null) {
					fn_coder = new FunctionCoder(fn_def.getDefinition(), root,
							onlyConc, compiler);
				}
				else if (isReserved(fn_name)) {
					throw new SbmlCompilerException(
						SbmlCompilerException.FUNCTION_NOT_FOUND, fn_name);
				}
			}
			if (fn_coder != null) {
				String varname = fn_coder.fastPrepare(bindings);
				callQueue.add(varname);
				addDepend('#' + varname);
				return;
			}
		}
		else if ((! root.isNumber()) && (! root.isOperator()) && (! root
				.isFunction())) {
			addDepend(root.getName());
		}
		for (int i = 0; i < root.getNumChildren(); i ++) {
			findInnerDepends(root.getChild(i), bindings, j + 1);
		}
	}
	
	/**
	 * Returns the name of the FORTRAN variable that is used for storing the
	 * results of the task this coder is responsible for.
	 * @return unique variable name of this coder as used in FORTRAN code
	 */
	final String getVarName() { return varName; }
	
	/**
	 * Finds the FORTRAN variable name in the dependency model that resolves
	 * the dependency indicated by name. If the dependency is a species that
	 * uses amounts as quantity, and if only concentrations may be used in
	 * mathematical expressions, this method returns a valid FORTRAN
	 * expression for the concentration of this species.
	 * @param bindings dependency model of the experiment
	 * @param name of a node in the dependency model
	 * @return FORTRAN variable name or expression for the named dependency
	 * @throws SbmlCompiler.SbmlCompilerException
	 */
	protected String getVarName(Bindings bindings, String name)
			throws SbmlCompilerException {
		FortranCoder coder = bindings.get(name);
		String varname = coder.getVarName();
		if ((! (coder instanceof ControlCoder)) && (onlyConc)) {
			SbmlBase ref = coder.getSbmlNode();
			if (ref instanceof Species) {
				Species species = (Species) ref;
				if (species.getInitialAmount() != null) {
					String vol_id = species.getCompartment().getId();
					String vol = bindings.get(vol_id).getVarName();
					varname = "(" + varname + " / " + vol + ")";
				}
			}
		}
		return varname;
	}
	
	/**
	 * Returns the set of dependencies.
	 * @return set containing the names of the FortranCoder instances this
	 * instance depepends on
	 */
	final HashSet<String> getCodeDependencies() { return depends; }

	/**
	 * Translation table for substituting SBML function names for FORTRAN
	 * function names where corresponding functions are available.
	 */
	private static final String reserved[][] = {{"abs", "abs"},
		{"arccos", "acos"}, {"arcsin", "asin"}, {"arctan", "atan"},
		{"cos", "cos"}, {"exp", "exp"}, {"ln", "log"}, {"sin", "sin"},
		{"tan", "tan"}};

    static boolean isReserved(String name) {
		for (int i = 0; i < reserved.length; i ++) {
			if (reserved[i][0].equals(name)) {
				return true;
			}
		}
		return false;
    }
    
	/**
	 * Translates the mathematical expression represented by the AST <code>x
	 * </code> to a mathematical expression in FORTRAN syntax. This method
	 * should be used whenever dealing with ASTs. {@link SbmlCompiler
	 * .FortranCoder#findInnerDependencies}, passing this exact AST <code>x
	 * </code>, must be invoked during the {@link SbmlCompiler.FortranCoder
	 * #initialize} call. 
	 * @param x root of the abstract syntax tree
	 * @param b dependency model of the experiment
	 * @param outer must be <code>org.sbml.libsbml.AST_UNKNOWN</code>
	 * @return mathematical expression in FORTRAN syntax
	 * @throws SbmlCompiler.SbmlCompilerException
	 */
	protected final String getFormula(AstNode x, Bindings b, NodeType outer)
			throws SbmlCompilerException {
		boolean parenthesis = false;
		String token;
		int numChildren = x.getNumChildren();
		NodeType type = x.getType();
		if (x.isOperator()) {
			if ((type == NodeType.AST_MINUS) && (x.getNumChildren() == 1)) {
				token = "-" + getFormula(x.getChild(0), b, type);
			}
			else {
				String op;
				switch (type) {
				case AST_PLUS:
					op = " + ";
					if ((outer != NodeType.AST_PLUS) && (outer != NodeType
							.AST_UNKNOWN)) parenthesis = true;
					break;
				case AST_MINUS:
					op = " - ";
					if ((outer != NodeType.AST_PLUS) && (outer != NodeType
							.AST_UNKNOWN)) parenthesis = true;
					break;
				case AST_TIMES:
					op = " * ";
					if ((outer == NodeType.AST_DIVIDE) || (outer == NodeType
							.AST_POWER)) parenthesis = true;
					break;
				case AST_DIVIDE:
					op = " / ";
					if ((outer == NodeType.AST_DIVIDE) || (outer == NodeType
							.AST_POWER)) parenthesis = true;
					break;
				case AST_POWER:
					op = " ** ";
					if (outer == NodeType.AST_POWER) parenthesis = true;
					break;
				default:
					throw new SbmlCompilerException(SbmlCompilerException
							.UNSUPPORTED_AST_NODE, type);
				}
				token = getFormula(x.getChild(0), b, type) + op + getFormula(
						x.getChild(1), b, type);
			}
		}
		else if (type == NodeType.AST_CONSTANT) {
			token = getVarName(b, x.getName());
		}
		else if (x.isNumber()) {
			token = x.getNumber().toString();
		}
		else if (x.isFunction()) {
			String name = x.getName();
			if (type == NodeType.AST_FUNCTION_POWER) {
				token = getFormula(x.getChild(0), b, NodeType.AST_POWER) +
						" ** "+ getFormula(x.getChild(1), b, NodeType
								.AST_POWER);
				if (outer == NodeType.AST_POWER) parenthesis = true;
			}
			else if (type == NodeType.AST_FUNCTION_ROOT) {
				token = getFormula(x.getChild(1), b, NodeType.AST_POWER) +
						" ** (1 / "+ getFormula(x.getChild(0), b, NodeType
								.AST_DIVIDE) + ")";
				if (outer == NodeType.AST_POWER) parenthesis = true;
			}
			else {
				int k;
				for (k = 0; k < reserved.length; k ++) {
					if (reserved[k][0].equals(name)) {
						name = reserved[k][1];
						break;
					}
				}
				if (k == reserved.length) {
					// reference result from delegate function coder
					token = callQueue.get(queueIndex ++);
					if (queueIndex == callQueue.size()) queueIndex = 0;
				}
				else {
					// use native FORTRAN function
					token = name + "(";
					for (int i = 0; i < numChildren; i ++) {
						if (i > 0) {
							token += ", ";
						}
						token += getFormula(x.getChild(i), b, NodeType
								.AST_FUNCTION);
					}
					token += ")";
				}
			}
		}
		else if (x.isName()) {
			if (type == NodeType.AST_NAME_TIME) {
				token = "t";
			}
			else {
				token = getVarName(b, x.getName());
			}
		}
		else if (type == NodeType.AST_LAMBDA) {
			// function body is the last child of the lambda node
			token = getFormula(x.getChild(numChildren - 1), b, NodeType
					.AST_UNKNOWN);
		}
		else throw new SbmlCompilerException(SbmlCompilerException
				.UNSUPPORTED_AST_NODE, type);
		if (parenthesis) token = "(" + token + ")";
		return token;
	}
	
	/**
	 * Must return the <code>libSBML</code> object of the model entity this
	 * <code>FortranCoder</code> represents, or null if it does not
	 * represent any model entity. 
	 * @return object of the entity this <code>FortranCoder</code>
	 * represents or null
	 */
	public abstract SbmlBase getSbmlNode();
	
	/**
	 * Returns whether this <code>FunctionCoder</code> has implemented its
	 * task in the FORTRAN function indicated by <code>visitor</code>.
	 * @param visitor context of the visitor, must be either <code>FFCN
	 * </code> or <code>GFCN</code>.
	 * @return whether the node has been visited already in this function
	 * context
	 */
	final boolean isVisited(int visitor) { return visited[visitor]; }
	
	/**
	 * Sets the flag indicating that this <code>FunctionCoder</code> has
	 * implemented its task in the FORTRAN function named by <code>visitor
	 * </code>.
	 * @param visitor function context of the visitor, must be either <code>
	 * FFCN</code> or <code>GFCN</code>. 
	 */
	final void goodbye(int visitor) {
		visited[visitor] = true;
	}
	
	/**
	 * Walks the dependency model for marking each coder uninitialized.
	 * Importantly, the {@link SbmlCompiler.FortranCoder#visited} flags are
	 * set to false. Must not be overridden by other classes than <code>
	 * ModelStateFcn</code>.
	 * @param bindings dependency model of this experiment
	 * @throws SbmlCompilerException
	 */
	void unprepare(Bindings bindings) throws SbmlCompilerException {
		initialized = false;
		for (Iterator<String> k = depends.iterator(); k.hasNext();) {
			FortranCoder coder = bindings.get(k.next());
			if (coder.isInitialized()) {
				coder.unprepare(bindings);
			}
		}
	}
	
	void registerToFunction(ArrayList<FortranFunction> code) { };
	
	String fastPrepare(Bindings bindings) throws SbmlCompilerException {
		if (! isInitialized()) {
			init(bindings);
		}
		String varname = getVarName();
		bindings.put('#' + varname, this);
		return varname;
	}
}
