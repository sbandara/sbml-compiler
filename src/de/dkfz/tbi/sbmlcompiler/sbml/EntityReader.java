package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class EntityReader extends StackedHandler {

	interface EntityContainer {
		void addEntity(SbmlBase entity); 
	}
	
	private final int expected_nesting;
	private final EntityContainer target;

	EntityReader(Context context, int nesting, EntityContainer target) {
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
			if (tag.equals("compartment")) {
				target.addEntity(new Compartment(model, atts));
			}
			else if (tag.equals("species")) {
				target.addEntity(new Species(model, atts));
			}
			else if (tag.equals("parameter")) {
				target.addEntity(new Parameter(atts));
			}
		}
		else if (tag.equals("listOfReactions")) {
			context.push(new ReactionReader(context));
		}
	}
	
	@Override
	void endElement(String tag, String str) throws SAXException {
	}
}
