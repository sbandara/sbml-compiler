package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.sbml.EntityReader.ModelBuilder;
import de.dkfz.tbi.sbmlcompiler.sbml.MathReader.MathContainer;
import de.dkfz.tbi.sbmlcompiler.sbml.Reaction.*;

class ReactionReader extends StackedHandler implements MathContainer,
		ModelBuilder {

	private Reaction reaction = null;
	private ReferenceType ref_type = null;
	private SpeciesReference ref = null;
	
	ReactionReader(Context context) {
		super(context);
	}

	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		Context context = getContext();
		if (tag.equals("reaction")) {
			reaction = new Reaction(context.getModel(), atts);
		}
		else if (tag.equals("listOfReactants")) {
			ref_type = ReferenceType.REACTANT;
		}
		else if (tag.equals("listOfProducts")) {
			ref_type = ReferenceType.PRODUCT;
		}
		else if (tag.equals("listOfModifiers")) {
			ref_type = ReferenceType.MODIFIER;
		}
		else if (tag.equals("speciesReference") || tag.equals(
				"modifierSpeciesReference")) {
			String species = atts.getValue("species");
			String str_stoichiometry = atts.getValue("stoichiometry");
			if (str_stoichiometry == null) {
				ref = reaction.new SpeciesReference(ref_type, species, null);
			}
			else {
				ref = reaction.new SpeciesReference(ref_type, species, Integer
						.valueOf(str_stoichiometry));
			}
		}
		else if (tag.equals("math")) {
			context.push(new MathReader(context, this));
		}
		else if (tag.equals("listOfParameters")) {
			context.push(new EntityReader(context, 1, this));
		}
	}

	@Override
	void endElement(String tag, String str) throws SAXException {
		if (tag.equals("reaction")) {
			reaction.resolveLocalScope();
			getContext().getModel().entities.put(reaction.id, reaction);
		}
		else if (tag.equals("speciesReference") || tag.equals(
				"modifierSpeciesReference")) {
			ref = null;
		}
	}

	@Override
	public void setRootNode(AstNode node) {
		if (ref != null) {
			ref.setStoichiometryMath(node);
		}
		else if (reaction != null) {
			reaction.setKineticLaw(node);
		}
	}

	@Override
	public void addEntity(SbmlBase entity) {
		reaction.addParameter((Parameter) entity);
	}

	@Override
	public void addRule(Rule rule) { }
}
