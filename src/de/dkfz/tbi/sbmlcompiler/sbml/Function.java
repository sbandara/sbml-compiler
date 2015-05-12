package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;

public class Function extends SbmlBase {
	
	private AstNode lambda = null;
	private String[] arguments = null;
	private AstNode body = null;
	
	Function(Attributes atts) throws SAXException {
		super(atts);
	}
	
	void setLambda(AstNode lambda) {
		if ((lambda.getType() != NodeType.AST_LAMBDA) || (lambda
				.getNumChildren() < 1)) {
			throw new IllegalArgumentException("Root node must be a lambda.");
		}
		this.lambda = lambda;
	}
	
	private void readArguments() {
		assertLambda();
		int n_args = lambda.getNumChildren() - 1;
		arguments = new String[n_args];
		for (int k = 0; k < arguments.length; k ++) {
			arguments[k] = lambda.getChild(k).getName();
		}
	}
	
	public String getArgument(int index) {
		if (arguments == null) {
			readArguments();
		}
		return arguments[index];
	}
	
	public int getNumArguments() {
		assertLambda();
		return lambda.getNumChildren() - 1;
	}
	
	public boolean isArgument(AstNode node) {
		if (node.getType() == NodeType.AST_NAME) {
			if (arguments == null) {
				readArguments();
			}
			String name = node.getName();
			for (int k = 0; k < arguments.length; k ++)
				if (name.equals(arguments[k])) return true;
		}
		return false;
	}
	
	public AstNode getDefinition() {
		assertLambda();
		if (body == null) {
			body = lambda.getChild(lambda.getNumChildren() - 1);
		}
		return body;
	}
	
	private void assertLambda() {
		if (lambda == null) {
			throw new IllegalStateException("Incomplete function definition.");
		}
	}
}
