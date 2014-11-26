package de.dkfz.tbi.sbmlcompiler;

/**
 * Exception class of the <code>SbmlCompiler</code>. In case poorly written
 * SBML is input to <code>SbmlCompiler</code>, the exception codes help
 * spotting the problem. SbmlCompilerExceptions are also thrown if an
 * experimental setup has been derived that cannot be reflected by the model.
 * @author Samuel Bandara
 */
public class SbmlCompilerException extends Exception {
	
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
		switch (exceptionType) {
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
			return "Undefined value for a non-constant entity " +
					exceptionParam;
		case ALGEBRAIC_CONSTRAINT:
			return  "The SBML contains an algebraic constraint. " +
					"Algebraic constraints are not supported yet.";
		case INVALID_NAME:
			return  "malformed parameter name: " + exceptionParam;
		case ALREADY_USED:
			return "Parameter already used: " + exceptionParam;
		default:
			return "Undescribed exception with code " + exceptionType + ".";
		}
	}
}
