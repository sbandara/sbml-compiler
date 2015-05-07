package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class EntityReader extends StackedHandler {

	private String list = null;
	
	EntityReader(Context context) {
		super(context);
	}
	
	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		int nested = getNested();
		if ((nested == 1) && (tag.toLowerCase().startsWith("listof"))) {
			list = tag.substring(6);
		}
		else if (nested == 2) {
			Model model = getContext().getModel();
			if (tag.equalsIgnoreCase("compartment")) {
				model.addCompartment(new Compartment(model, atts));
			}
			else if (tag.equalsIgnoreCase("Species")) {
				model.addSpecies(new Species(model, atts));
			}
		}
	}
	
	@Override
	void endElement(String tag) throws SAXException {	
	}
}
