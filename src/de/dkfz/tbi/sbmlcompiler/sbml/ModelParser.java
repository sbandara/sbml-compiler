package de.dkfz.tbi.sbmlcompiler.sbml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ModelParser {
	
	private final XMLReader xml_reader;
	
	private class ModelHandler extends DefaultHandler {
		
		private int sbml_nested = -1;
		
		@Override
		public void startElement(String uri, String local, String prefixed,
				Attributes atts) throws SAXException {
			if (sbml_nested > -1) {
				sbml_nested ++;
			}
			else if ("sbml".equalsIgnoreCase(local)) {
				sbml_nested = 0;
			}
			if (("model".equalsIgnoreCase(local)) && (sbml_nested == 1)) {
				// TODO: wire in model handler
			}
		}
		
		@Override
		public void endElement(String uri, String local, String prefixed)
				throws SAXException {
			if (sbml_nested > -1) {
				sbml_nested --;
			}
		}
	}
	
	public ModelParser() {
		try {
			xml_reader = XMLReaderFactory.createXMLReader();
		}
		catch (SAXException e) {
			throw new RuntimeException("Unable to instantiate XML reader.");
		}
	}
	
	private void parse(InputSource source) throws IOException, SAXException {
		xml_reader.setContentHandler(new ModelHandler());
		xml_reader.parse(source);
	}
	
	public void parse(String sbml) throws IOException, SAXException {
		parse(new InputSource(new StringReader(sbml)));
	}
	
	public void parse(File sbml) throws IOException, SAXException {
		InputStream is = new FileInputStream(sbml);
		try {
			parse(new InputSource(is));		
		}
		finally {
			is.close();
		}
	}
}
