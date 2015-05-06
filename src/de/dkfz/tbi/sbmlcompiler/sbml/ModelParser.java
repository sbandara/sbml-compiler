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
import org.xml.sax.helpers.XMLReaderFactory;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class ModelParser {
	
	private final XMLReader xml_reader;
	private String id = null, name = null;
	
	private class ModelHandler extends StackedHandler {
		
		ModelHandler(XMLReader reader) {
			super(reader);
		}
		
		int level = -1, version = -1;
		
		@Override
		public void startElement(String tag, Attributes atts)
				throws SAXException {
			int nested = getNested();
			if ((nested == 1) && ("sbml".equalsIgnoreCase(tag))) {
				for (int k = 0; k < atts.getLength(); k ++) {
					if (atts.getLocalName(k).equalsIgnoreCase("version")) {
						version = Integer.parseInt(atts.getValue(k));
					}
					else if (atts.getLocalName(k).equalsIgnoreCase("level")) {
						level = Integer.parseInt(atts.getValue(k));
					}
				}
				if ((level > 2) || (version > 2)) {
					throw new SAXException("SBML version not supported",
							new SbmlCompilerException(SbmlCompilerException
									.CANNOT_READ_SBML, null));
				}
			}
			if ((nested == 2) && ("model".equalsIgnoreCase(tag))) {
				for (int k = 0; k < atts.getLength(); k ++) {
					if (atts.getLocalName(k).equalsIgnoreCase("name")) {
						name = atts.getValue(k);
					}
					else if (atts.getLocalName(k).equalsIgnoreCase("id")) {
						id = atts.getValue(k);
					}
				}
			}
		}
		
		@Override
		public void endElement(String tag) throws SAXException {
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
	
	public String getId() { return id; }
	
	public String getName() { return name; }
	
	private void parse(InputSource source) throws IOException, SAXException {
		xml_reader.setContentHandler(new ModelHandler(xml_reader));
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
