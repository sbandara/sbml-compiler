package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class Rule {

	public enum RuleType { ASSIGNMENT, RATE_RULE, ALGEBRAIC_RULE }

	private final String variable_ref;
	private final Model model;
	private SbmlBase variable = null;
	private final RuleType type;
	private AstNode expression = null;
	
	Rule(Model model, Attributes atts, RuleType type) throws SAXException {
		this.model = model;
		variable_ref = atts.getValue("variable");
		if ((type == RuleType.ALGEBRAIC_RULE) && (variable_ref != null)) {
			throw new SAXException("Attribute 'variable' in algebraic rule.");
		}
		this.type = type;
	}
	
	public SbmlBase getVariable() throws SbmlCompilerException {
		if (type == RuleType.ALGEBRAIC_RULE) {
			throw new IllegalStateException("No variable in algebraic rule.");
		}
		if (variable == null) {
			variable = model.getEntity(variable_ref);
			if (variable == null) {
				throw new SbmlCompilerException(SbmlCompilerException
						.UNKNOWN_MODEL_ENTITY, variable_ref);
			}
		}
		return variable;
	}
	
	public RuleType getType() { return type; }
	
	public AstNode getExpression() { return expression; }
	
	void setExpression(AstNode root_node) { expression = root_node; }
}
