package de.dkfz.tbi.sbmlcompiler;

import java.util.HashMap;
import java.util.Map;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode;
import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;
import de.dkfz.tbi.sbmlcompiler.sbml.SbmlBase;

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
	private AstNode root;
	
	/**
	 * Holds the arguments of the function call. An argument of a function
	 * call can be a composite.
	 */
	private AstNode args[];
	
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
	FunctionCoder(AstNode lambda, AstNode call, boolean onlyconc, SbmlCompiler
			compiler) {
		super(compiler);
		args = new AstNode[(int)call.getNumChildren()];
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
						argOnlyConc, compiler);
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
				arg_symbol = args[i].getNumber().toString();
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
		AstNode fn_body = root.getChild(root.getNumChildren() - 1);
		target.appendStatement(varname + " = " + getFormula(fn_body,
				bindings, NodeType.AST_UNKNOWN));
	}

	String getPrefix() { return "fn"; }

	public SbmlBase getSbmlNode() { return null; }
}
