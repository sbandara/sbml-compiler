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
			"minus", TIMES = "times", DIVIDE = "divide", APPLY = "apply",
			POWER = "power", CSYMBOL = "csymbol", EXP_E = "exponentiale",
			PI = "pi", CI = "ci", CN = "cn";
	
	private final static String SYM_URL = "http://www.sbml.org/sbml/symbols/",
			SYM_DELAY = "delay", SYM_TIME = "time";
	
	private AstNode node = null;
	private final MathContainer target;
	private boolean is_apply = false;
	
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
		tag = tag.toLowerCase();
		switch (tag) {
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
		case POWER:
			child = new AstNode(NodeType.AST_POWER);
			break;
		case CSYMBOL:
			String def_url = atts.getValue("definitionURL");
			if (def_url == null) {
				throw new SAXException("Unspecified csymbol found.");
			}
			if (def_url.equals(SYM_URL + SYM_DELAY)) {
				child = new AstNode(NodeType.AST_FUNCTION_DELAY);
			}
			else if (def_url.equals(SYM_URL + SYM_TIME)) {
				child = new AstNode(NodeType.AST_NAME_TIME);
			}
			else {
				throw new SAXException("Unknown csymbol found.");
			}
		case PI:
		case EXP_E:
			child = new AstNode(NodeType.AST_CONSTANT, tag);
			break;
		}
		if (child != null) {
			if (node != null) {
				node.appendNode(child);
			}
			node = child;
		}
		if (tag.equals(APPLY)) {
			is_apply = true;
		}
		else if (! tag.equals(CI)) {
			is_apply = false;
		}
	}
	
	@Override
	void endElement(String tag, String str) throws SAXException {
		str = str.trim();
		switch (tag.toLowerCase()) {
		case CI:
			if (is_apply) {
				if (node == null) {
					node = new AstNode(NodeType.AST_FUNCTION, str);
				}
				else {
					node.appendNode(new AstNode(NodeType.AST_FUNCTION, str));
				}
				is_apply = false;
			}
			else {
				node.appendNode(new AstNode(NodeType.AST_NAME, str));
			}
			break;
		case CN:
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
