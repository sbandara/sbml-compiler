package de.dkfz.tbi.sbmlcompiler;

import java.util.ArrayList;

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
     		if (colPos + len_item + 3 > SbmlCompiler.WRAP_LINE) {
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
 		while (line.length() > SbmlCompiler.WRAP_LINE) {
 			int p = line.lastIndexOf(' ', SbmlCompiler.WRAP_LINE);
 			if (p == 9) {
 				p = line.indexOf(' ', SbmlCompiler.WRAP_LINE + 1);
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
  	
	/**
 	 * Adds a FORTRAN comment to this function.
 	 * @param comment any comment string
 	 */
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
