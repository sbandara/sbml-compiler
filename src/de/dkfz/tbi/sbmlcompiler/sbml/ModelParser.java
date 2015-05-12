package de.dkfz.tbi.sbmlcompiler.sbml;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;
import de.dkfz.tbi.sbmlcompiler.sbml.EntityReader.ModelBuilder;

public class ModelParser {
	
	private class SbmlHandler extends StackedHandler implements ModelBuilder {
		
		SbmlHandler(XMLReader reader) {
			super(reader);
		}
		
		@Override
		public void startElement(String tag, Attributes atts)
				throws SAXException {
			int nested = getNested();
			if ((nested == 1) && ("sbml".equals(tag))) {
				int version = 1, level = -1;
				for (int k = 0; k < atts.getLength(); k ++) {
					if (atts.getLocalName(k).equals("version")) {
						version = Integer.parseInt(atts.getValue(k));
					}
					else if (atts.getLocalName(k).equals("level")) {
						level = Integer.parseInt(atts.getValue(k));
					}
				}
				if ((level == -1) || (version == -1)) {
					throw new SAXException("Unable to determine SBML version.",
							new SbmlCompilerException(SbmlCompilerException
									.CANNOT_READ_SBML, null));
				}
				if ((level != 2) || (version > 1)) {
					throw new SAXException("SBML version not supported",
							new SbmlCompilerException(SbmlCompilerException
									.CANNOT_READ_SBML, null));
				}
			}
			else if ((nested == 2) && ("model".equals(tag))) {	
				Context context = getContext();
				context.setModel(new Model(atts));
				context.push(new EntityReader(context, 2, this));
			}
		}
		
		@Override
		public void endElement(String tag) throws SAXException {
		}

		@Override
		public void addEntity(SbmlBase entity) {
			getContext().getModel().entities.put(entity.id, entity);
		}

		@Override
		public void addRule(Rule rule) {
			getContext().getModel().rules.add(rule);
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
	
	public Model parse(InputStream is) throws IOException, SAXException {
		StackedHandler handler = new SbmlHandler(xml_reader);
		xml_reader.setContentHandler(handler);
		try {
			xml_reader.parse(new InputSource(is));
		}
		finally {
			is.close();
		}
		return handler.getContext().getModel();
	}
}
