package de.dkfz.tbi.sbmlcompiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class Bindings implements Cloneable {
	
	private final HashMap<String, FortranCoder> bindings;
	
	private Bindings(HashMap<String, FortranCoder> clone) {
		bindings = clone;
	}
	
	Bindings() {
		bindings = new HashMap<String, FortranCoder>();
	}
	
	public FortranCoder get(String id) {
		return bindings.get(id);
	}
	
	public Set<String> getIds() { return bindings.keySet(); }
	
	public Collection<FortranCoder> getCoders() { return bindings.values(); }
	
	public void put(String id, FortranCoder coder) {
		bindings.put(id, coder);
	}
	
	public Bindings clone() {
 		HashMap<String, FortranCoder> clone = new HashMap<String,
 				FortranCoder>();
 		clone.putAll(bindings);
 		return new Bindings(clone);
	}
}
