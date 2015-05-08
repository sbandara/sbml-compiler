package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;

class MathReader extends StackedHandler {
	
	interface MathContainer {
		void setRootNode(AstNode node); 
	}
	
	private final static String LAMBDA = "lambda", PLUS = "plus", MINUS =
			"minus", TIMES = "times", DIVIDE = "divide", APPLY = "apply";
	
	private AstNode node = null;
	private final MathContainer target;
	
	MathReader(Context context, MathContainer target) {
		super(context);
		this.target = target;
	}
	
	MathReader(XMLReader reader, MathContainer target) {
		super(reader);
		this.target = target;
	}
	
	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		AstNode child = null;
		switch (tag.toLowerCase()) {
		case LAMBDA:
			child = new AstNode(NodeType.AST_LAMBDA);
			break;
		case PLUS:
			child = new AstNode(NodeType.AST_PLUS);
			break;
		case MINUS:
			child = new AstNode(NodeType.AST_MINUS);
			break;
		case TIMES:
			child = new AstNode(NodeType.AST_TIMES);
			break;
		case DIVIDE:
			child = new AstNode(NodeType.AST_DIVIDE);
			break;
		}
		if (child != null) {
			if (node != null) {
				node.appendNode(child);
			}
			node = child;
		}
	}
	
	@Override
	void endElement(String tag, String str) throws SAXException {
		switch (tag.toLowerCase()) {
		case "ci":
			node.appendNode(new AstNode(NodeType.AST_NAME, str.trim()));
			break;
		case "cn":
			str = str.trim();
			if ((str.indexOf('.') != -1) || (str.indexOf('e') != -1) ||
					(str.indexOf('E') != -1)) {
				node.appendNode(new AstNode(Double.parseDouble(str)));
			}
			else {
				node.appendNode(new AstNode(Integer.parseInt(str)));
			}
			break;
		}
		if ((tag.equalsIgnoreCase(LAMBDA)) || (tag.equalsIgnoreCase(APPLY))) {
			AstNode parent = node.getParent();
			if (parent == null) {
				target.setRootNode(node);
			}
			node = parent;
		}
	}
}
