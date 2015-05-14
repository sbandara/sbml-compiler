package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Model extends SbmlBase {
	
	final HashMap<String, SbmlBase> entities = new HashMap<String, SbmlBase>();
	final ArrayList<Rule> rules = new ArrayList<Rule>();
	
	Model(Attributes atts) throws SAXException {
		super(atts);
	}
	
	public SbmlBase getEntity(String id) { return entities.get(id); }
	
	public Set<String> getEntityIds() { return entities.keySet(); }
	
	public Rule getRule(int index) { return rules.get(index); }
	
	public int getNumRules() { return rules.size(); }
}
