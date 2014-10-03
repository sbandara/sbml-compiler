package de.dkfz.tbi.sbmlcompiler;

import java.util.*;
import org.sbml.libsbml.*;

final public class SbmlCompiler {
	
	class PlotCoder extends FortranCoder {
		
		ArrayList<String> outputs = new ArrayList<String>();
		
		void putFortranCode(FortranFunction target, Map<String, FortranCoder>
				bindings) throws SbmlCompilerException
		{
			int n_var = 0;
        	String write_stmt = "WRITE(10,100) t";
        	for (Iterator<String> i = outputs.iterator(); i.hasNext();)
        	{
        		String name = i.next();
        		FortranCoder coder = bindings.get(name);
        		if (coder == null) {
        			throw new SbmlCompilerException(SbmlCompilerException
        					.UNKNOWN_MODEL_ENTITY, name);
        		}
    			write_stmt += ", " + coder.getVarName();
        	}
        	target.appendStatement(write_stmt);
        	target.appendStatement(100, "FORMAT(E20.10," + n_var +
        			"(1X,E20.10))");
		}
		
		void registerToFunction(ArrayList<FortranFunction> code) {
			code.get(PLOTFCN).outputs.add(this);
		}
		
		PlotCoder(ArrayList<String> outputs) {
			this.outputs = outputs;
		}

		String getPrefix() { return "plot"; }

		public SBase getSbmlNode() { return null; }

		protected void initialize(Map<String, FortranCoder> bindings)
				throws SbmlCompilerException {
			for (Iterator<String> k = outputs.iterator(); k.hasNext();) {
				addDepend(k.next());
			}
		}
	}
	
	/**
	 * Exception class of the <code>SbmlCompiler</code>. In case poorly written
	 * SBML is input to <code>SbmlCompiler</code>, the exception codes help
	 * spotting the problem. SbmlCompilerExceptions are also thrown if an
	 * experimental setup has been derived that cannot be reflected by the model.
	 * @author Samuel Bandara
	 */
	public static class SbmlCompilerException extends Exception {
		
		public int exceptionType;
		
		public Object exceptionParam;
		
		public SbmlCompilerException(int type, Object param) {
			exceptionType = type;
			exceptionParam = param;
		}
		
		/**
		 * An error occurred while loading the SBML file into the libSBML
		 * library. <code>exceptionParam</code> will be specified filename.
		 */
		public static final int CANNOT_READ_SBML = 1;
		
		/**
		 * The type of a node in an abstract syntax tree is not supported.
		 * <code>exceptionParam</code> will be the <code>Integer</code> code of
		 * libSBML for this type of node.
		 */
		public static final int UNSUPPORTED_AST_NODE = 2;
		
		/**
		 * The function called from a mathematical expression is an unsupported
		 * primitive function or not defined in the SBML function definitions.
		 * <code>exceptionParam</code> will be the name of this function. 
		 */
		public static final int FUNCTION_NOT_FOUND = 3;
		
		/**
		 * A variable used in an abstract syntax tree is not defined in the SBML
		 * file. <code>exceptionParam</code> will be the name of this variable. 
		 */
		public static final int UNKNOWN_MODEL_ENTITY = 4;
		
		/**
		 * A species, a compartment, or a parameter is declared non-constant, but
		 * its value is neither defined by a rate, assignment, or algebraic
		 * rule, nor by a reaction. <code>exceptionParam</code> will be a set of
		 * model entities without any defined value.
		 */
		public static final int UNDEFINED_VOLATILE = 5;
		
		/**
		 * An algebraic rule turned out to be an algebraic constraint. Algebraic
		 * constraints, however, are not supported yet. <code>exceptionParam
		 * </code> will be <code>null</code>.
		 */
		public static final int ALGEBRAIC_CONSTRAINT = 6;
		
		/**
		 * The name of a user-defined calibration parameter is badly formed. A
		 * valid name begins with an alpha character and must be alphanumeric.
		 * <code>exceptionParam</code> will be the malformed parameter name.
		 */
		public static final int INVALID_NAME = 7;

		/**
		 * The name of a user-defined calibration parameter is already used
		 * within the model. <code>exceptionParam</code> will be the allocated
		 * entity name.
		 */
		public static final int ALREADY_USED = 8;

		/**
		 * The user-defined mathematical expression is syntactically invalid.
		 * <code>libsbml.parseFormula</code> is used for parsing the expression,
		 * thus the limitations of that method have to be considered. <code>
		 * exceptionParam</code> will be the syntactically invalid expression. 
		 */
		public static final int INVALID_EXPRESSION = 9;
		
		public static final int UNSUPPORTED_CONTROL_TYPE = 10;

		public static final int NAMES_NOT_GENERATED_YET = 11;
		
		private static final long serialVersionUID = 1L;
		
		public String getMessage() {
			
			switch (this.exceptionType) {
			case CANNOT_READ_SBML: 
				return "An error occurred while loading the SBML file " 
					+ exceptionParam + " into the libSBML library";
			case UNSUPPORTED_AST_NODE:
				return "The type of a node with code " + exceptionParam + 
				" in an abstract syntax tree of libSBML is not supported.";
			case FUNCTION_NOT_FOUND:
				return "The function called from a mathematical " +
						"expression " + exceptionParam + " is an unsupported " +
						"primitive function or not defined in the SBML function " +
						"definitions";
			case UNKNOWN_MODEL_ENTITY:
				return "The variable "  + exceptionParam + " " +
						"is not defined in the SBML file";
			case UNDEFINED_VOLATILE:
				return "Undefined value for a non-constant entity " + exceptionParam;
			case ALGEBRAIC_CONSTRAINT:
				return  "The SBML contains an algebraic constraint. " +
						"Algebraic constraints are not supported yet.";
			case INVALID_NAME:
				return  "malformed parameter name: " + exceptionParam;
			case ALREADY_USED:
				return "Parameter already used: " + exceptionParam;
			default:
				return "Undefined exception.";
			}
		
		}
	}
	
	public final static int FFCN = 0, GFCN = 1, PLOTFCN = 2, MFCN1 = 3;
	
	/**
	 * Maximum number of characters in a line of FORTRAN code. If the number of
	 * characters exceeds <code>wrapLine</code>, the line is wrapped at the
	 * previous blank character. 
	 */
	public static int wrapLine = 72;
	
	/**
	 * Number of differential states to be used for implementing a time-delayer
	 * tube.
	 */
	public static int delaySteps = 5;
    
	/**
	 * Translation table for substituting SBML function names for FORTRAN
	 * function names where corresponding functions are available.
	 */
	private static final String reserved[][] = {{"abs", "abs"},
		{"arccos", "acos"}, {"arcsin", "asin"}, {"arctan", "atan"},
		{"cos", "cos"}, {"exp", "exp"}, {"ln", "log"}, {"sin", "sin"},
		{"tan", "tan"}};
	
	/**
	 * Element of a FORTRAN function. A <code>FortranCoder</code> generates a
	 * piece of FORTRAN code that implements the task this coder is responsible
	 * for. It must specify the names of all other coders that must have put
	 * down their piece of code this <code>FortranCoder</code> relys on. The
	 * <code>SbmlCompiler</code> then guarantees that all its dependencies are
	 * satisfied when {@link de.dkfz.tbi.sbmlcompiler.SbmlCompiler.FortranCoder#putFortranCode
	 * } is invoked on this coder. Often, a <code>FortranCoder</code> represents
	 * an entity of the model.
	 * @author Samuel Bandara
	 */
	public abstract class FortranCoder {
    	
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
		protected final void addDepend(String dependency) {
			((HashSet<String>)depends).add(dependency);
			if (onlyConc) {
				Species species = model.getSpecies(dependency);
				if ((species != null) && (species.isSetInitialAmount())) {
					depends.add(species.getCompartment());
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
    	abstract void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings)
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
		final void init(Map<String, FortranCoder> bindings) throws SbmlCompilerException {
			String prefix = getPrefix();
			id = id_generator.makeId(prefix);
			varName = prefix + id;
			depends.clear();
			callQueue.clear();
			queueIndex = 0;
			visited = new boolean[n_visit_flags];
			initialized = true;
			initialize(bindings);
			for (Iterator<String> k = depends.iterator(); k.hasNext();) {
				String name = k.next();
				FortranCoder coder = bindings.get(name);
				if (coder == null) {
					throw new InternalError("Did not find coder object for id " + name);
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
		protected abstract void initialize(Map<String, FortranCoder> bindings) throws SbmlCompilerException;
		
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
		protected final void findInnerDepends(ASTNode root, Map<String, FortranCoder> bindings, int j)
				throws SbmlCompilerException {
			int type = root.getType();
			if ((root.isFunction()) && (type != libsbml.AST_FUNCTION_ROOT) &&
					(type != libsbml.AST_FUNCTION_POWER)) {
				String fn_name = root.getName();
				FortranCoder fn_coder = null;
				if (fn_name.equals("delay")) {
					ASTNode reference = root.getChild(0);
					ASTNode delay = root.getChild(1);
					fn_coder = new TimeDelayCoder(reference, delay, onlyConc);
				}
				else {
					FunctionDefinition fn_def = model.getFunctionDefinition(
							fn_name);
					if (fn_def != null) {
						fn_coder = new FunctionCoder(fn_def.getMath(), root,
								onlyConc);
					}
					else {
						boolean isFortranPredef = false;
						for (int i = 0; i < reserved.length; i ++) {
							if (reserved[i][0].equals(fn_name)) {
								isFortranPredef = true;
								break;
							}
						}
						if (! isFortranPredef) throw new SbmlCompilerException(
								SbmlCompilerException.FUNCTION_NOT_FOUND,
								fn_name);
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
		protected String getVarName(Map<String, FortranCoder> bindings, String name) throws
    			SbmlCompilerException {
			FortranCoder coder = bindings.get(name);
			String varname = coder.getVarName();
			if ((! (coder instanceof ControlCoder)) && (onlyConc)) {
				SBase ref = coder.getSbmlNode();
				if ((ref != null) && (ref.getTypeCode() == libsbml.SBML_SPECIES)
						&& (((Species)ref).isSetInitialAmount())) {
					String compartment = ((Species)ref).getCompartment();
					String vol = bindings.get(compartment).getVarName();
					varname = "(" + varname + " / " + vol + ")";
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
		protected final String getFormula(ASTNode x, Map<String, FortranCoder> b, int outer)
				throws SbmlCompilerException
		{
			boolean paranth = false;
			String token;
			long numChildren = x.getNumChildren();
			int type = x.getType();
			if (x.isOperator()) {
				if (x.isUMinus()) {
					token = "-" + getFormula(x.getChild(0), b, type);
				}
				else {
					String op;
					switch(type) {
					case libsbml.AST_PLUS:
						op = " + ";
						if ((outer != libsbml.AST_PLUS) && (outer !=
							libsbml.AST_UNKNOWN)) paranth = true;
						break;
					case libsbml.AST_MINUS:
						op = " - ";
						if ((outer != libsbml.AST_PLUS) && (outer !=
							libsbml.AST_UNKNOWN)) paranth = true;
						break;
					case libsbml.AST_TIMES:
						op = " * ";
						if ((outer == libsbml.AST_DIVIDE) || (outer ==
							libsbml.AST_POWER))	paranth = true;
						break;
					case libsbml.AST_DIVIDE:
						op = " / ";
						if ((outer == libsbml.AST_DIVIDE) || (outer ==
							libsbml.AST_POWER))	paranth = true;
						break;
					case libsbml.AST_POWER:
						op = " ** ";
						if (outer == libsbml.AST_POWER) paranth = true;
						break;
					default:
						throw new SbmlCompilerException(SbmlCompilerException
								.UNSUPPORTED_AST_NODE, new Integer(type));
					}
					token = getFormula(x.getChild(0), b, type) + op +
						getFormula(x.getChild(1), b, type);
				}
			}
			else if (x.isConstant()) {
				token = getVarName(b, x.getName());
			}
			else if (x.isNumber()) {
				if (x.isInteger()) {
					token = Integer.toString(x.getInteger());
				}
				else {
					token = Double.toString(x.getReal());
				}
			}
			else if (x.isFunction()) {
				String name = x.getName();
				if (type == libsbml.AST_FUNCTION_POWER) {
					token = getFormula(x.getChild(0), b, libsbml
						.AST_POWER) + " ** "+ getFormula(x.getChild(1),
						b, libsbml.AST_POWER);
					if (outer == libsbml.AST_POWER) paranth = true;
				}
				else if (type == libsbml.AST_FUNCTION_ROOT) {
					token = getFormula(x.getChild(1), b, libsbml
						.AST_POWER) + " ** (1 / "+ getFormula(x.getChild
						(0), b, libsbml.AST_DIVIDE) + ")";
					if (outer == libsbml.AST_POWER) paranth = true;
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
						token = callQueue.get(queueIndex ++);
						if (queueIndex == callQueue.size()) queueIndex = 0;
					}
					else {
						token = name + "(";
						for (long i = 0; i < numChildren; i ++) {
							if (i > 0) {
								token += ", ";
							}
							token +=  getFormula(x.getChild(i), b, libsbml
									.AST_FUNCTION);
						}
						token += ")";
					}
				}
			}
			else if (x.isName()) {
				if (type == libsbml.AST_NAME_TIME) {
					token = "t";
				}
				else {
					token = getVarName(b, x.getName());
				}
			}
			else if (x.isLambda()) {
				token = getFormula(x.getChild(numChildren - 1), b, libsbml
						.AST_UNKNOWN);
			}
			else throw new SbmlCompilerException(SbmlCompilerException
					.UNSUPPORTED_AST_NODE, new Integer(type));
			if (paranth) token = "(" + token + ")";
			return token;
		}
		
		/**
		 * Must return the <code>libSBML</code> object of the model entity this
		 * <code>FortranCoder</code> represents, or null if it does not
		 * represent any model entity. 
		 * @return object of the entity this <code>FortranCoder</code>
		 * represents or null
		 */
		public abstract SBase getSbmlNode();
		
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
		void unprepare(Map<String, FortranCoder> bindings) throws SbmlCompilerException {
			initialized = false;
			for (Iterator<String> k = depends.iterator(); k.hasNext();) {
				FortranCoder coder = bindings.get(k.next());
				if (coder.isInitialized()) {
					coder.unprepare(bindings);
				}
			}
		}
		
		void registerToFunction(ArrayList<FortranFunction> code) { };
		
		String fastPrepare(Map<String, FortranCoder> bindings) throws SbmlCompilerException {
    		if (! isInitialized()) {
    			init(bindings);
    		}
    		String varname = getVarName();
    		bindings.put('#' + varname, this);
    		return varname;
		}
    }
    
	/**
	 * Evaluates a function call. The use of ADIFOR for differentiation of
	 * FORTRAN code forbids the translation of SBML function definitions to
	 * user-defined FORTRAN functions. Instead, for each function call, <code>
	 * FunctionCoder</code> substitutes each argument name for the argument
	 * passed to the function and implements the evaluation of this expression.
	 * @author Samuel Bandara
	 */
    final class FunctionCoder extends FortranCoder {
    	
    	/**
    	 * Holds the abstract syntax tree of the function definition.
    	 */
    	private ASTNode root;
    	
    	/**
    	 * Holds the arguments of the function call. An argument of a function
    	 * call can be a composite.
    	 */
    	private ASTNode args[];
    	
    	/**
    	 * Knows the mathematical FORTRAN expression that must replace the name
    	 * of an argument.
    	 */
    	private HashMap<String, String> argSymbols;
    	
    	/**
    	 * Whether the quantity of species contained in function arguments must
    	 * be transformed to concentrations if amounts are given. Note that in
    	 * the context of a function <i>definition</i>, quantities of species
    	 * are <i>never</i> transformed.
    	 */
    	private boolean argOnlyConc;
    	
    	/**
    	 * @param lambda abstract syntax tree of the function definition
    	 * @param call abstract syntax tree of the function call
    	 * @param onlyconc whether the quantity of species contained in function
    	 * arguments must be transformed to concentrations if amounts are given  
    	 */
    	FunctionCoder(ASTNode lambda, ASTNode call, boolean onlyconc) {
    		args = new ASTNode[(int)call.getNumChildren()];
    		for (int i = 0; i < args.length; i ++) {
    			args[i] = call.getChild(i);
    		}
    		root = lambda;
    		argOnlyConc = onlyconc;
    	}
    	
    	/**
    	 * Prepares the substitution of argument names by mathematical FORTRAN
    	 * expressions. Composite arguments are delegated to <code>EvalCoders
    	 * </code>. Simple variables and constants are passed directly.
    	 */
		protected void initialize(Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
			argSymbols = new HashMap<String, String>();
			for (int i = 0; i < args.length; i ++) {
				String arg_symbol;
				if (args[i].getNumChildren() > 0) {
					EvalCoder eval_coder = new EvalCoder(null, args[i], "arg",
							argOnlyConc);
					arg_symbol = eval_coder.fastPrepare(bindings);
					addDepend('#' + arg_symbol);
				}
				else if (args[i].isName()) {
					arg_symbol = '~' + args[i].getName();
					onlyConc = argOnlyConc;
					addDepend(args[i].getName());
					onlyConc = false;
				}
				else if (args[i].isNumber()) {
					arg_symbol = Double.toString(args[i].getReal());
				}
				else throw new InternalError();
				argSymbols.put(root.getChild(i).getName(), arg_symbol);
			}
		}
		
		/**
		 * Replaces argument names by the variable names of delegated tasks or
		 * simple variables.
		 */
		protected String getVarName(Map<String, FortranCoder> bindings, String name) throws
				SbmlCompilerException {
			if (argSymbols.containsKey(name)) {
				String symbol = argSymbols.get(name);
				if (symbol.startsWith("~")) {
					onlyConc = argOnlyConc;
					symbol = super.getVarName(bindings, symbol.substring(1));
					onlyConc = false;
				}
				return symbol;
			}
			return super.getVarName(bindings, name);
		}

		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings)
				throws SbmlCompilerException {
			String varname = this.getVarName();
			target.declareVar(varname);
			ASTNode fn_body = root.getChild(root.getNumChildren() - 1);
			target.appendStatement(varname + " = " + getFormula(fn_body,
					bindings, libsbml.AST_UNKNOWN));
		}

		String getPrefix() { return "fn"; }

		public SBase getSbmlNode() { return null; }
    }
    
    /**
     * Class of coders that are responsible for a state variable of the FFCN or
     * the GFCN. These coders are not only involved in the calculation of the
     * new state, i.e. by evaluating the differential equation or calculating
     * the algebraic residual, but also need to make the current value of the
     * state available within one or more FORTRAN functions. By convention, a
     * <code>StateVariable</code> generates the output of a model function when
     * {@link SbmlCompiler.FortranCoder#putFortranCode} is called, which is
     * referred to as the entry of dependencies, and rescales the input current
     * value of the state upon invocation of {@link SbmlCompiler.StateVariable
     * #putHeader}. Thus, a <code>StateVariable</code> closes the loop of
     * dependencies by interfacing to the integrator. Note that <code>
     * putFortranCode</code> is executed only once during compilation, whereas
     * <code>putHeader</code> may be called up to once for each model function.
     * @author Samuel Bandara
     */
    abstract class StateVariable extends FortranCoder {
    	
    	/**
    	 * Wether this <code>StateVariable</code> has implemented the reference
    	 * to its value in the FORTRAN function indicated by the array index.
    	 */
    	private boolean looped[];
    	
    	/**
    	 * Prepares visitor flags for the phase of code generation. This method
    	 * must be invoked explicitly by overriding methods. 
    	 */
    	protected void initialize(Map<String, FortranCoder> bindings)
    			throws SbmlCompilerException {
    		looped = new boolean[n_visit_flags];
    	}
    	
    	/**
    	 * Implements a reference to the current value of the state variable by
    	 * making {@link SbmlCompiler.StateVariable#putHeader} assign it to a
    	 * local FORTRAN variable. 
    	 * @param target target function
    	 * @param bindings dependency model of the experiment
    	 * @param visitor type of target function, must be either FFCN or GFCN
    	 */
    	void closeLoop(FortranFunction target, Map<String, FortranCoder> bindings,
    			int visitor) {
    		if (! looped[visitor]) {
    			putHeader(target, bindings);
    		}
    		looped[visitor] = true;
    	}
    	
    	/**
    	 * Puts the header assignment of this state variable to the target
    	 * function <code>target</code>. <code>SbmlCompiler</code> guarantees
    	 * that this method is invoked only once per target function.
    	 * @param target target function
    	 * @param bindings dependency model of the experiment
    	 */
    	abstract void putHeader(FortranFunction target, Map<String, FortranCoder> bindings);
    	
    	/**
    	 * Returns wether this state variable is differential or algebraic.
    	 * @return FFCN if it is differential, or GFCN if it is algebraic
    	 */
    	abstract int getTarget();
    }

    /**
     * Evaluates simple mathematical expressions. <code>EvalCoder</code> only
     * minimally implements the abstract methods of <code>FortranCoder</code>,
     * but allows for free choice of the prefix of the variable name. It mainly
     * serves for implementing the evaluation of kinetic laws and assignment
     * rules, and for evaluation tasks delegated from other coders. 
     * @author Samuel Bandara
     */
    public class EvalCoder extends FortranCoder {
    	
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
    			onlyconc) {
    		ref_obj = obj;
    		funcDef = function;
    		my_prefix = prefix;
    		onlyConc = onlyconc;
    	}
    	
		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
				SbmlCompilerException
		{
			String name = getVarName();
			target.declareVar(name);
			String formula = getFormula(funcDef, bindings, libsbml.AST_UNKNOWN);
			String assignment = name + " = " + formula;
			target.appendStatement(assignment);
		}

		protected void initialize(Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
			findInnerDepends(funcDef, bindings, 0);
		}
		
		public SBase getSbmlNode() { return ref_obj; }

		String getPrefix() { return my_prefix; }
    }

    /**
     * Implements a FORTRAN constant. Offering the keyword PARAMETER, FORTRAN
     * allows for the definition of constants. <code>ConstantCoder</code> is
     * mainly used for defining the constants pi and e, which <code>SbmlCompiler
     * </code> includes on demand, and for encoding constant values of model
     * entities. 
     * @author Samuel Bandara
     */
    public class ConstantCoder extends FortranCoder {

    	/**
    	 * The libSBML object of the model entity this coder represents. 
    	 */
    	private SBase ref_obj = null;
    	
    	/**
    	 * Value of the constant.
    	 */
    	private double my_value = 0;
    	
    	/**
    	 * @param obj libSBML object of the model entity this coder represents
    	 */
    	public ConstantCoder(SBase obj) {
    		ref_obj = obj;
    		if (ref_obj instanceof Compartment) {
    			Compartment compartment = (Compartment)ref_obj;
    			if (compartment.isSetVolume()) {
    				my_value = compartment.getVolume();
    			}
    			else my_value = 1;
    		}
    		else if (ref_obj instanceof Species) {
    			Species species = (Species)ref_obj;
    			if (species.isSetInitialConcentration()) {
    				my_value = species.getInitialConcentration();
    			}
    			else if (species.isSetInitialAmount()) {
    				my_value = species.getInitialAmount();
    			}
    		}
    		else if (ref_obj instanceof Parameter) {
    			Parameter param = (Parameter)ref_obj;
    			if (param.isSetValue()) {
    				my_value = param.getValue();
    			}
    		}
    	}
    	
    	/**
    	 * @param value value of the constant
    	 */
       	ConstantCoder(double value) {
    		my_value = value;
    	}
       	
       	/**
       	 * Returns the value of the constant.
       	 * @return value of the constant
       	 */
    	double getValue() { return my_value; }
    	
		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
				SbmlCompilerException
		{
			String name = getVarName();
			target.declareVar(name);
			target.defineConst(name + " = " + Double.toString(my_value));
		}
		
		protected void initialize(Map<String, FortranCoder> bindings) { }
		
		public SBase getSbmlNode() { return ref_obj; }

		String getPrefix() { return "const"; }
    }

    /**
     * Implements a reference to an estimated parameter by assigning its value
     * to a local FORTRAN variable. Instead of a scaled item of the array of
     * parameters, a mark consisting of a dollar sign enclosing the name of the
     * parameter on each side, is put. 
     * @author Samuel Bandara
     */
    class ParameterCoder extends FortranCoder {
    	
    	/**
    	 * The libSBML object of the model entity this coder implements. 
    	 */
    	private SBase refObj;
    	
    	/**
    	 * The id of the model entity or the name of a calibration parameter.
    	 */
    	private String paramName;
    	
    	/**
    	 * @param param libSBML object of the model entity this coder implements
    	 * @param name id of the model entity or the name of a calibration
    	 * parameter, which will be decorated, and used as mark
    	 */
    	ParameterCoder(SBase param, String name) {
    		refObj = param;
    		paramName = name;
    	}
    	
		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
			String stmt = getVarName();
			target.declareVar(stmt);
			stmt += " = $" + paramName + "$";
			target.appendStatement(stmt);
		}
		
		protected void initialize(Map<String, FortranCoder> bindings) { }
		
		public SBase getSbmlNode() { return refObj; }

		String getPrefix() { return "par"; }
    }

    /**
     * Implements a reference to an optimal control by assigning its value to a
     * local FORTRAN variable. For indexing the array of controls, the <code>id
     * </code> of this <code>FortranCoder</code> with prefix "q" is used.
     * @author Samuel Bandara
     */
    public class ControlCoder extends FortranCoder {
    	
    	/**
    	 * The libSBML object of the model entity this coder implements.
    	 */
    	private SBase refObj;
    	
    	public final static byte CONSTANT = 1, PW_CONSTANT = 2;
    	
    	private byte discretize;
    	
    	/**
    	 * @param param libSBML object of the model entity this coder implements
    	 * @param scale scaling factor to be applied to this item of the array
    	 * of controls
    	 */
    	public ControlCoder(SBase param, byte discretize) {
    		refObj = param;
    		this.discretize = discretize;
    	}
    	
		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
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
		
		public SBase getSbmlNode() { return refObj; }

		String getPrefix() {
			if (discretize == CONSTANT) { 
				return "q";
			}
			else return "u";
		}
    }
    
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
    	DiffStateCoder(SBase obj, String reaction[]) {
    		ref_obj = obj;
    		my_reaction = reaction;
    	}
    	
    	/**
    	 * @param obj libSBML object of the model entity this coder represents
    	 * @param diffeq abstract syntax tree of the rate rule
    	 */
    	DiffStateCoder(SBase obj, ASTNode diffeq) {
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
		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
				SbmlCompilerException
		{
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
		
		protected void initialize(Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
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

		int getTarget() { return FFCN; }
		
		void registerToFunction(ArrayList<FortranFunction> code) {
			code.get(FFCN).outputs.add(this);
		}
    }

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
    	private ASTNode my_algeq;
    	
    	/**
    	 * Set of variable names found in this algebraic equation. {@link
    	 * SbmlCompiler#assignAlgStates} rules out one after another until one
    	 * variable takes the role of the corresponding algebraic state.
    	 */
    	private HashSet<String> candidates = new HashSet<String>();
    	
    	/**
    	 * Returns the set of variable names found in this algebraic equation.
    	 * After algebraic states have been assigned, this set contains the
    	 * algebraic state variable as its sole element.
    	 * @return set containing the variable names found in this algebraic
    	 * equation, or, if other candidates have been ruled out, the name of
    	 * the algebraic state variable 
    	 */
    	protected HashSet<String> getCandidates() { return candidates; }
    	
    	/**
    	 * Identifies candidates for the role of the algebraic state variable,
    	 * recursively.
    	 */
    	private void findStateCandidates(ASTNode root) {
    		if ((root.isName()) && (root.getType() != libsbml.AST_NAME_TIME)) {
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
    	AlgStateCoder(ASTNode algeq) {
    		my_algeq = algeq;
    		findStateCandidates(my_algeq);
    	}

    	void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
    			SbmlCompilerException {
			String code = "g(" + getId() + ") = ";
			code += getFormula(my_algeq, bindings, libsbml.AST_UNKNOWN);
			target.appendStatement(code);
		}
		
		protected void initialize(Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
			super.initialize(bindings);
			findInnerDepends(my_algeq, bindings, 0);
		}
		
		public SBase getSbmlNode() { return null; }

		String getPrefix() { return "xa"; }

		void putHeader(FortranFunction target, Map<String, FortranCoder>bindings) {
    		String name = getVarName();
			target.declareVar(name);
			String code = name + " = x(" + (getId() +
					id_generator.getGreatestId("xd")) + ")";
    		target.appendStatement(code);
		}

		int getTarget() { return GFCN; }

		void registerToFunction(ArrayList<FortranFunction> code) {
			code.get(GFCN).outputs.add(this);
		}
    }

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
    	TimeDelayCoder(ASTNode entity, ASTNode delay, boolean onlyconc) {
    		my_entity = entity;
    		my_delay = delay;
    		onlyConc = onlyconc;
    	}

		void putFortranCode(FortranFunction target, Map<String, FortranCoder> bindings) throws
				SbmlCompilerException {
			String varname = getVarName();
			String nByDt = varname + "v";
			target.declareVar(nByDt);
			target.appendStatement(nByDt + " = " + delaySteps + " / (" +
					getFormula(my_delay, bindings, libsbml.AST_UNKNOWN) + ")");
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
			first_id = id_generator.makeId("xd");
			for (int i = 1; i < delaySteps - 1; i ++) {
				id_generator.makeId("xd");
			}
			last_id = id_generator.makeId("xd");
		}

		String getPrefix() { return "dly"; }

		public SBase getSbmlNode() { return null; }

		void putHeader(FortranFunction target, Map<String, FortranCoder> bindings) {
			target.appendStatement(getVarName() + " = x(" + last_id + ")");
		}

		int getTarget() { return FFCN; }
    }

    /**
     * Dependency model of the <i>in vivo</i> situation. This dependency model
     * is created by the constructor of <code>SbmlCompiler</coder> while reading
     * an SBML file.
     */
    private Map<String, FortranCoder> inVivoBindings = new HashMap<String, FortranCoder>();
    
    /**
     * The libSBML representation of the SBML model.
     */
    Model model;
    
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
    private HashMap<String, ArrayList<String>> reactome = new HashMap<String, ArrayList<String>>();
    
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
		ArrayList<AlgStateCoder> algebraicEquations = new ArrayList<AlgStateCoder>();
		for (int i = 0; i < listOfRules.size(); i ++) {
			Rule rule = (Rule)listOfRules.get(i);
			if (rule instanceof AlgebraicRule) {
				algebraicEquations.add(new AlgStateCoder(rule.getMath()));
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
					coder = new EvalCoder(ref_obj, expr, "asgn", false);
				}
				else {
					coder = new DiffStateCoder(ref_obj, expr);
				}
				inVivoBindings.put(v_name, coder);
			}
		}
		
		/*
		 * Walks the list of reactions for finding local parameters and for
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
						param.getName());
				inVivoBindings.put(name, param_coder);
			}
	    	String rxn_name = reaction.getId();
	    	addToReactome(reaction.getListOfReactants(), rxn_name);
	    	addToReactome(reaction.getListOfProducts(), rxn_name);
			EvalCoder rxn_coder = new EvalCoder(reaction, reaction
					.getKineticLaw().getMath(), "rxn", true);
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
			DiffStateCoder diff_coder = new DiffStateCoder(species, a_rxns);
			inVivoBindings.put(spec_name, diff_coder);
		}
		for (int i = 0; i < listOfReactions.size(); i ++) {
			Reaction reaction = (Reaction)listOfReactions.get(i);
			addToReactome(reaction.getListOfModifiers(), reaction.getId());
		}
		
		HashSet<String> algebraicVariable = new HashSet<String>();
		/* Walks the list of global parameters. If the constant field is set to
		 * true, the parameter cannot be controlled by a rule, but is identifiable.
		 * If it is not constant and not put to the dependency model, already,
		 * it is a candidate for an algebraic state.
		 */
		ListOf listOfParams = model.getListOfParameters();
    	for (int i = 0; i < listOfParams.size(); i ++) {
    		Parameter param = (Parameter)listOfParams.get(i);
    		String param_name = param.getId();
    		idFromNameMap.put(param.getName(), param_name);
    		if (param.getConstant()) {
				ParameterCoder param_coder = new ParameterCoder(param,
						param.getName());
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
				ConstantCoder const_coder = new ConstantCoder(volume);
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
				ConstantCoder const_coder = new ConstantCoder(species);
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
    		inVivoBindings.put("pi", new ConstantCoder(3.14159));
    	}
    	if (! inVivoBindings.containsKey("e")) {
    		inVivoBindings.put("e", new ConstantCoder(2.71828));
    	}
    }

	/**
	 * Increments a number for each prefix of FORTRAN variable names, starting
	 * from 1. That way, unique FORTRAN variable names are generated. Further,
	 * FORTRAN arrays, which are at the interface to numerical software, are
	 * indexed by these numbers, using a different prefix for each array.
	 * @author Samuel Bandara
	 */
    private static class IdGenerator {
		
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
	}
    
    public Map<String, FortranCoder> getInVivoBindings() {
    	Map<String, FortranCoder> clone = new HashMap<String, FortranCoder>();
    	clone.putAll(inVivoBindings);
    	return clone;
    }

    /**
     * FORTRAN function which accepts the output of <code>FortranCoders</code>.
     * <code>FortranFunction</code> produces a FORTRAN function complying with
     * the interface definition of VPLAN, but can easily adapted to other pieces
     * of numerical software. <code>FortranCoders</code> can declare variables,
     * using {@link #declareVar}, define constants, using {@link #defineConst},
     * and append ordinary FORTRAN statements to the body of the function, using
     * {@link #appendStatement}. Code is beautified by wrapping lines in FORTRAN
     * style. The code is returned as the string representation of this object,
     * that is, by {@link #toString}. The output variables a <code>
     * FortranFunction</code> calculates can be queried via {@link #getOutputs}.
     * @author Samuel Bandara
     */
	public final class FortranFunction {
    	
    	/**
    	 * Body block of code containing ordinary FORTRAN statements.
    	 */
    	private String bodyBlock = new String();
    	
    	/**
    	 * Header block containing the function header and the fixed declaration
    	 * of this function's arguments.
    	 */
    	private String headerBlock;
    	
    	/**
    	 * Name of this FORTRAN function.
    	 */
    	private String fcnName;
    	
    	/**
    	 * @return name of this FORTRAN function
    	 */
    	String getName() { return fcnName; }
    	
    	/**
    	 * Two lists of code items, one for variables to be declared, indexed
    	 * {@link #DECL}, and one for the definition of constants, indexed
    	 * {@link #CONST}.
    	 */
    	private ArrayList<String> declBlock = new ArrayList<String>();

    	private ArrayList<String> constBlock = new ArrayList<String>();

    	final static private String CONT_LINEFEED = "     &    ";
    	
    	final public static int DECL = 0, CONST = 1;
    	
    	final static String PLOT = "plot";
    	
    	/**
    	 * Creates an empty FORTRAN function.
    	 * @param name name of this FORTRAN function
    	 * @param var output variable, for example "f" or "g", or <code>PLOT
    	 * </code> for generating a plot function
    	 */
     	FortranFunction(String name, String var) {
     		declBlock = new ArrayList<String>();
     		if (var.equals(PLOT)) {
     			headerBlock = "      SUBROUTINE " + name + "(t, x, p, q, nga," +
     				" nt, wron,\n     &   ngq, gq, ngaq1, ngaq2, gaq, rwh, iwh)"
     				+ "\n        IMPLICIT NONE\n        INTEGER*4 i, iwh(*), " +
     				"nga, nt, ngq, ngaq1, ngaq2\n        REAL*8 x(*),t, rwh(*),"
     				+ " p(*), q(*), wron(nga,nt),\n     &       gq(ngq,*), gaq"
     				+ "(ngaq1,ngaq2,*)\n";
     		}
     		else {
	 			headerBlock = "      SUBROUTINE " + name + "(t, x, " + var +
					", p, q, rwh, iwh, iflag)\n        IMPLICIT NONE\n" +
					"        REAL*8 t, x(*), " + var + "(*), p(*), q(*)," +
					" rwh(*)\n        INTEGER*4 iwh(*), iflag\n";
     		}
	 		constBlock = new ArrayList<String>();
	 		fcnName = name;
    	}
     	
     	/**
     	 * Assembles a block of FORTRAN code, either a declaration block for
     	 * variables or a definition block for constants.
     	 * @param block identifier of the block to be assembled, either <code>
     	 * DECL</code> or <code>CONST</code>
     	 * @return block of FORTRAN code
     	 */
     	private String assembleBlock(int block) {
     		ArrayList<String> items;
     		String code;
     		if (block == DECL) {
     			items = declBlock;
     			code = "        REAL*8 ";
     		}
     		else {
     			items = constBlock;
     			code = "        PARAMETER (";
     		}
     		int colPos = code.length(); 
     		for (int i = 0; i < items.size(); i ++) {
     			String item = (String)items.get(i);
     			int len_item = item.length();
         		if (colPos + len_item + 3 > wrapLine) {
         			if (i > 0) {
         				code += ',';
         			}
         			code += '\n' + CONT_LINEFEED;
         			colPos = CONT_LINEFEED.length();
         		}
         		else if (i > 0) {
         			code += ", ";
         			colPos += 2;
         		}
         		code += item;
         		colPos += len_item;
     		}
     		return code + ((block == CONST)? ")\n": "\n");
     	}

     	/**
     	 * Adds the definition of a constant to this function.
     	 * @param def assignment expression defining the constant, for example
     	 * "pi = 3.14159".
     	 */
     	void defineConst(String def) { constBlock.add(def); }
     	
     	/**
     	 * Adds the declaration of a REAL*8-typed variable to this function.
     	 * @param decl name of the FROTRAN variable
     	 */
     	void declareVar(String decl) { declBlock.add(decl); }
     	
     	private void appendStmt(String statement, String margin) {
     		String line = margin + statement;
     		while (line.length() > wrapLine) {
     			int p = line.lastIndexOf(' ', wrapLine);
     			if (p == 9) {
     				p = line.indexOf(' ', wrapLine + 1);
     				if (p == -1) {
     					bodyBlock += line + '\n';
     					return;
     				}
     			}
 				bodyBlock += line.substring(0, p) + '\n';
 				line = CONT_LINEFEED + line.substring(p + 1);
     		}
     		bodyBlock += line + '\n';
     	}
     	
    	/**
     	 * Adds an ordinary FORTRAN statement to this function.
     	 * @param statement FORTRAN statement
     	 */
      	void appendStatement(String statement) {
     		appendStmt(statement, "        ");
     	}
      	
      	void appendComment(String comment) {
     		appendStmt(comment, "C       ");
      	}
 
      	/**
     	 * Adds an ordinary FORTRAN statement with a line number to this
     	 * function.
     	 * @param line number to be prepended t o the statement
     	 * @param statement FORTRAN statement
     	 */
     	void appendStatement(int lineNumber, String statement) {
     		String margin = Integer.toString(lineNumber);
     		for (int i = margin.length(); i < 8; i ++) {
     			margin += ' ';
     		}
     		appendStmt(statement, margin);
     	}
     	
     	/**
     	 * @return entire code of this FORTRAN function
     	 */
     	public String toString() {
     		String function = headerBlock;
     		if (! declBlock.isEmpty()) {
     			function += assembleBlock(DECL);
     		}
     		if (! constBlock.isEmpty()) {
     			function += assembleBlock(CONST);
     		}
     		function += bodyBlock + "      END\n";
    		return function;
    	}
     	
     	ArrayList<FortranCoder> outputs = new ArrayList<FortranCoder>();
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
    private void visitDependents(FortranCoder parent, Map<String, FortranCoder> bindings,
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
    
    IdGenerator id_generator = null;
    
    int n_visit_flags;
    
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
    public ArrayList<FortranFunction> compile(Map<String, FortranCoder> bindings,
    		String prefix, Set<String> mfcns)
    throws SbmlCompilerException {
    	
    	ArrayList<FortranFunction> fn = new ArrayList<FortranFunction>();
    	
    	fn.add(FFCN, new FortranFunction(prefix + "ffcn", "f"));
    	
    	fn.add(GFCN, new FortranFunction(prefix + "gfcn", "g"));
    	
    	fn.add(PLOTFCN, new  FortranFunction(prefix + "plot", FortranFunction.PLOT));
    	
    	for (Iterator<FortranCoder> k = bindings.values().iterator(); k.hasNext();) {
    		k.next().registerToFunction(fn);
    	}

    	id_generator = new IdGenerator();
		n_visit_flags = 3 + mfcns.size();

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
 			
 			for (Iterator<FortranCoder> k = target.outputs.iterator(); k.hasNext();) {
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
 		id_generator = null;
    	
    	return fn;
    }
    
    private Map<String, String> idFromNameMap = new HashMap<String, String>();
    
    public String getIdFromName(String name) { return idFromNameMap.get(name); }
    
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
			
			ArrayList<FortranFunction> fn = cmplr.compile(bnds, "", null);
			System.out.println(fn.get(0).toString());
		}
		catch (SbmlCompilerException e) {
			System.out.println(e.getMessage());
		}
	}
}
