package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Model extends SbmlBase {
	
	final HashMap<String, SbmlBase> entities = new HashMap<String, SbmlBase>();
	
	Model(Attributes atts) throws SAXException {
		super(atts);
	}
	
	void addEntity(SbmlBase entity) {
		entities.put(entity.getId(), entity);
	}
	
	public SbmlBase getEntity(String id) {
		return entities.get(id);
	}
}
