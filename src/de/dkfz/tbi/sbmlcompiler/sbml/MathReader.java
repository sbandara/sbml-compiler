package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class MathReader extends StackedHandler {

	MathReader(Context context, Object target) {
		super(context);
	}
	
	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
	}
	
	@Override
	void endElement(String tag) throws SAXException {
	}
}
