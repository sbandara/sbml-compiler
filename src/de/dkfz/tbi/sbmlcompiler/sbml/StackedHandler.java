package de.dkfz.tbi.sbmlcompiler.sbml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public abstract class StackedHandler extends DefaultHandler {
	
	private final DefaultHandler prev;
	private final XMLReader reader;
	
	private int nested = 0;
	
	public StackedHandler(XMLReader reader, DefaultHandler prev) {
		this.prev = prev;
		this.reader = reader;
	}
	
	@Override
	public void startElement(String uri, String local, String prefixed,
			Attributes atts) throws SAXException {
		nested ++;
		startElement(local, atts);
	}
	
	@Override
	public void endElement(String uri, String local, String prefixed)
			throws SAXException {
		if (nested > 0) {
			endElement(local);
		}
		else if (-- nested == -1) {
			reader.setContentHandler(prev);
			prev.endElement(uri, local, prefixed);
		}
	}
	
	abstract void startElement(String tag, Attributes atts) throws SAXException;

	abstract void endElement(String tag) throws SAXException;
}
