package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.ArrayList;

public class AstNode {
	
	public enum NodeType {
		
		AST_FUNCTION(16), AST_CONSTANT(32), AST_CONSTANT_E(33),
		AST_CONSTANT_PI(34), AST_OPERATOR(64), AST_DIVIDE(65), AST_NUMBER(1),
		AST_MINUS(66), AST_PLUS(67), AST_TIMES(68), AST_POWER(69),
		AST_UNKNOWN(0), AST_FUNCTION_DELAY(19), AST_LAMBDA(2), AST_NAME(128),
		AST_NAME_TIME(129), AST_FUNCTION_ROOT(17), AST_FUNCTION_POWER(18);
		
		private final int value;
		
		NodeType(int value) { this.value = value; }
	}
	
	private final NodeType type;
	private final ArrayList<AstNode> children = new ArrayList<AstNode>();
	private AstNode parent = null;
	private Number number = null;
	private String name = null;
	
	AstNode(NodeType type) {
		this.type = type;
	}
	
	AstNode(Number number) {
		type = NodeType.AST_NUMBER;
		this.number = number;
	}
	
	AstNode(NodeType type, String name) {
		this.name = name;
		if (type == NodeType.AST_FUNCTION) {
			this.type = resolveFunctionType(name);
		}
		else {
			this.type = type;
		}
	}
	
	private NodeType resolveFunctionType(String name) {
		switch (name) {
		case "root":
			return NodeType.AST_FUNCTION_ROOT;
		case "":
			return NodeType.AST_FUNCTION_POWER;
		default:
			return NodeType.AST_FUNCTION;
		}
	}
	
	public NodeType getType() { return type; }
	
	public boolean isNumber() { return number != null; }
	
	public Number getNumber() { return number; }
	
	private boolean isAstType(NodeType subtype, NodeType type) {
		return (subtype.value & type.value) == type.value;
	}
	
	public boolean isConstant() {
		return isAstType(type, NodeType.AST_CONSTANT);
	}
	
	public boolean isName() {
		return isAstType(type, NodeType.AST_NAME);
	}
	
	public String getName() { return name; }
	
	public boolean isFunction() {
		return isAstType(type, NodeType.AST_FUNCTION);
	}
	
	public boolean isOperator() {
		return isAstType(type, NodeType.AST_OPERATOR);
	}
	
	void appendNode(AstNode child) {
		child.parent = this;
		children.add(child);
	}
	
	public AstNode getParent() { return parent; }
	
	public AstNode getChild(int index) { return children.get(index); }
	
	public int getNumChildren() { return children.size(); }
}
