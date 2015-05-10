package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.sbml.MathReader.MathContainer;

class ReactionReader extends StackedHandler implements MathContainer {

	private Reaction reaction = null;
	
	ReactionReader(Context context) {
		super(context);
	}

	@Override
	void startElement(String tag, Attributes atts) throws SAXException {
		
	}

	@Override
	void endElement(String tag, String str) throws SAXException {
	}

	@Override
	public void setRootNode(AstNode node) {
	}
}
