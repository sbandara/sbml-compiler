package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class CompartmentReader extends StackedHandler {
	
	
	CompartmentReader(Context context) {
		super(context);
	}
	
	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		if ((getNested() != 1) || (! tag.equalsIgnoreCase("compartment"))) {
			return;
		}
		Model model = getContext().getModel();
		model.addCompartment(new Compartment(model, atts));
	}
	
	@Override
	void endElement(String tag) throws SAXException {	
	}
}
