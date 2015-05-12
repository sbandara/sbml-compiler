package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

abstract class StackedHandler extends DefaultHandler {
	
	final static class Context {

		private final ArrayList<ContentHandler> stack = new ArrayList
				<ContentHandler>();
		private final XMLReader reader;
		private Model model = null;
		
		Context(XMLReader reader, ContentHandler base) {
			this.reader = reader;
			reader.setContentHandler(base);
		}
		
		private ContentHandler pop() {
			ContentHandler prev = stack.remove(stack.size() - 1);
			reader.setContentHandler(prev);
			return prev;
		}
		
		void push(ContentHandler next) {
			stack.add(reader.getContentHandler());
			reader.setContentHandler(next);
		}

		void setModel(Model model) { this.model = model; }
		
		Model getModel() { return model; }
	}
	
	private int nested = 0;
	private final Context context;
	private StringBuilder str;
		
	StackedHandler(Context context) {
		this.context = context;
	}
	
	StackedHandler(XMLReader reader) {
		this.context = new Context(reader, this);
	}
	
	final int getNested() { return nested; }
	
	final Context getContext() { return context; }
	
	@Override
	public final void startElement(String uri, String local, String prefixed,
			Attributes atts) throws SAXException {
		nested ++;
		str = new StringBuilder();
		startElement(local, atts);
	}
	
	@Override
	final public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (str != null) {
			str.append(ch, start, length);
		}
	}
	
	@Override
	public final void endElement(String uri, String local, String prefixed)
			throws SAXException {
		if (nested > 0) {
			endElement(local, str.toString());
		}
		if (-- nested == -1) {
			ContentHandler prev = context.pop();
			prev.endElement(uri, local, prefixed);
		}
	}
	
	abstract void startElement(String tag, Attributes atts) throws SAXException;

	abstract void endElement(String tag, String str) throws SAXException;
}
