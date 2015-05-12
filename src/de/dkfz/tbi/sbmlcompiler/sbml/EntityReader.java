package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.sbml.MathReader.MathContainer;
import de.dkfz.tbi.sbmlcompiler.sbml.Rule.RuleType;

class EntityReader extends StackedHandler implements MathContainer {

	interface ModelBuilder {
		void addEntity(SbmlBase entity);
		void addRule(Rule rule);
	}
	
	private final int expected_nesting;
	private final ModelBuilder target;
	private Rule rule = null;

	EntityReader(Context context, int nesting, ModelBuilder target) {
		super(context);
		this.expected_nesting = nesting;
		this.target = target;
	}
	
	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		int nested = getNested();
		Context context = getContext();
		if (nested == expected_nesting) {
			Model model = context.getModel();
			switch (tag) {
			case "compartment":
				target.addEntity(new Compartment(model, atts));
				break;
			case "species":
				target.addEntity(new Species(model, atts));
				break;
			case "parameter":
				target.addEntity(new Parameter(atts));
				break;
			case "event":
				throw new SAXException("Discontinuous models not supported.");
			case "assignmentRule":
				rule = new Rule(model, atts, RuleType.ASSIGNMENT);
				context.push(new MathReader(context, this));
				break;
			case "rateRule":
				rule = new Rule(model, atts, RuleType.RATE_RULE);
				context.push(new MathReader(context, this));
				break;
			case "algebraicRule":
				rule = new Rule(model, atts, RuleType.ALGEBRAIC_RULE);
				context.push(new MathReader(context, this));
				break;
			}
		}
		else if (tag.equals("listOfReactions")) {
			context.push(new ReactionReader(context));
		}
	}
	
	@Override
	void endElement(String tag, String str) throws SAXException {
		if ((tag.equals("assignmentRule")) || (tag.equals("rateRule")) ||
				(tag.equals("algebraicRule"))) {
			rule = null;
		}
	}

	@Override
	public void setRootNode(AstNode node) {
		if (rule != null) {
			rule.setExpression(node);
			target.addRule(rule);
		}
	}
}
