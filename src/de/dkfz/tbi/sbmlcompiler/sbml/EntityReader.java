package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class EntityReader extends StackedHandler {

	EntityReader(Context context) {
		super(context);
	}
	
	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		int nested = getNested();
		if (nested == 2) {
			Model model = getContext().getModel();
			if (tag.equalsIgnoreCase("Compartment")) {
				model.addEntity(new Compartment(model, atts));
			}
			else if (tag.equalsIgnoreCase("Species")) {
				model.addEntity(new Species(model, atts));
			}
			else if (tag.equalsIgnoreCase("Parameter")) {
				model.addEntity(new Parameter(atts));
			}
		}
	}
	
	@Override
	void endElement(String tag, String str) throws SAXException {
	}
}