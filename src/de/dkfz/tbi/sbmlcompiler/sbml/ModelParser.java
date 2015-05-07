package de.dkfz.tbi.sbmlcompiler.sbml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class ModelParser {
	
	private class SbmlHandler extends StackedHandler {
		
		SbmlHandler(XMLReader reader) {
			super(reader);
		}
		
		@Override
		public void startElement(String tag, Attributes atts)
				throws SAXException {
			int nested = getNested();
			if ((nested == 1) && ("sbml".equalsIgnoreCase(tag))) {
				int version = 1, level = -1;
				for (int k = 0; k < atts.getLength(); k ++) {
					if (atts.getLocalName(k).equalsIgnoreCase("version")) {
						version = Integer.parseInt(atts.getValue(k));
					}
					else if (atts.getLocalName(k).equalsIgnoreCase("level")) {
						level = Integer.parseInt(atts.getValue(k));
					}
				}
				if ((level == -1) || (version == -1)) {
					throw new SAXException("Unable to determine SBML version.",
							new SbmlCompilerException(SbmlCompilerException
									.CANNOT_READ_SBML, null));
				}
				if ((level != 2) || (version > 2)) {
					throw new SAXException("SBML version not supported",
							new SbmlCompilerException(SbmlCompilerException
									.CANNOT_READ_SBML, null));
				}
			}
			else if ((nested == 2) && ("model".equalsIgnoreCase(tag))) {				
				getContext().setModel(new Model(atts));
			}
			else if (nested == 3) {
				switch (tag) {
				case "listOfCompartments":
					context.push(new CompartmentReader(context));
					break;
				case "listOfSpecies":
					break;
				case "listOfParameters":
					break;
				case "listOfRules":
					break;
				case "listOfReactions":
					break;
				}
			}
		}
		
		@Override
		public void endElement(String tag) throws SAXException {
		}
	}
	
	private final XMLReader xml_reader;
	private String id = null, name = null;
	
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
	
	public void parse(File sbml) throws IOException, SAXException {
		InputStream is = new FileInputStream(sbml);
		xml_reader.setContentHandler(new SbmlHandler(xml_reader));
		try {
			xml_reader.parse(new InputSource(is));
		}
		finally {
			is.close();
		}
	}
}
