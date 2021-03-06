package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.ArrayList;
import java.util.Iterator;

public class AstNode {
	
	public enum NodeType {
		
		AST_FUNCTION(16), AST_FUNCTION_ROOT(17), AST_FUNCTION_POWER(18),
		AST_FUNCTION_DELAY(19), AST_FUNCTION_LN(20), AST_FUNCTION_LOG(21),
		AST_OPERATOR(32), AST_DIVIDE(33),  AST_MINUS(34), AST_PLUS(35),
		AST_TIMES(36), AST_POWER(37), AST_NAME(64), AST_NAME_TIME(65),
		AST_UNKNOWN(0), AST_NUMBER(1), AST_LAMBDA(2), AST_CONSTANT(3),
		AST_MASK(240);
		
		private final int value;
		
		NodeType(int value) { this.value = value; }
		
		public boolean isKindOf(NodeType other) {
			return (value & AST_MASK.value) == other.value;
		}
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
		if (type == NodeType.AST_FUNCTION) {
			this.type = resolveFunctionType(name);
		}
		else {
			this.name = name;
			this.type = type;
		}
	}
	
	private NodeType resolveFunctionType(String name) {
		switch (name) {
		case "root":
			return NodeType.AST_FUNCTION_ROOT;
		case "power":
			return NodeType.AST_FUNCTION_POWER;
		case "ln":
			return NodeType.AST_FUNCTION_LN;
		case "log":
			return NodeType.AST_FUNCTION_LOG;
		default:
			this.name = name;
			return NodeType.AST_FUNCTION;
		}
	}
	
	public NodeType getType() { return type; }
	
	public boolean isNumber() { return number != null; }
	
	public Number getNumber() { return number; }
		
	public boolean isName() {
		return type.isKindOf(NodeType.AST_NAME);
	}
	
	public String getName() { return name; }
	
	public boolean isFunction() {
		return type.isKindOf(NodeType.AST_FUNCTION);
	}
	
	public boolean isOperator() {
		return type.isKindOf(NodeType.AST_OPERATOR);
	}
	
	void appendNode(AstNode child) {
		child.parent = this;
		children.add(child);
	}
	
	public AstNode getParent() { return parent; }
	
	public AstNode getChild(int index) { return children.get(index); }
	
	public int getNumChildren() { return children.size(); }
	
	void rescope(String local_id, String scoped_id) {
		Iterator<AstNode> it = children.iterator();
		while (it.hasNext()) {
			AstNode c = it.next();
			if ((c.type == NodeType.AST_NAME) && (c.name.equals(local_id))) {
				c.name = scoped_id;
			}
			else if (c.children.size() > 0) {
				c.rescope(local_id, scoped_id);
			}
		}
	}
	
	public void reduceToBinary() {
		final int n = children.size();
		if ((n > 2)  && ((type == NodeType.AST_PLUS) || (type == NodeType
				.AST_TIMES))) {
			final int h = n / 2;
			int k = 0;
			AstNode a;
			if (n == 3) {
				a = children.get(k ++);
			}
			else {
				a = new AstNode(type);
				while (k < h) {
					a.appendNode(children.get(k ++));
				}
			}
			AstNode b = new AstNode(type);
			while (k < n) {
				b.appendNode(children.get(k ++));
			}
			children.clear();
			children.add(a);
			children.add(b);
		}
		Iterator<AstNode> it = children.iterator();
		while (it.hasNext()) {
			it.next().reduceToBinary();
		}
	}
	
	public void flattenTree() {
		if ((type == NodeType.AST_PLUS) || (type == NodeType.AST_TIMES)) {
			int k = 0;
			while (k < children.size()) {
				final AstNode child = children.get(k);
				if (child.type == type) {
					final int n_grandchildren = child.children.size();
					for (int n = 0; n < n_grandchildren; n ++) {
						children.add(child.children.get(n));
					}
					children.remove(k);
				} else {
					child.flattenTree();
					k ++;
				}
			}
		} else {
			final int n_children = children.size();
			for (int k = 0; k < n_children; k ++) {
				children.get(k).flattenTree();
			}
		}
	}
}
