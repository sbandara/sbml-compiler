package de.dkfz.tbi.sbmlcompiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class Bindings implements Cloneable {
	
	private final HashMap<String, FortranCoder> bindings;
	final SbmlCompiler compiler;
	boolean did_compile = false;
	
	private Bindings(SbmlCompiler compiler, HashMap<String, FortranCoder> clone) {
		this.compiler = compiler;
		bindings = clone;
	}
	
	Bindings(SbmlCompiler compiler) {
		this(compiler, new HashMap<String, FortranCoder>());
	}
	
	public FortranCoder get(String id) {
		return bindings.get(id);
	}
	
	public Set<String> getIds() { return bindings.keySet(); }
	
	public Collection<FortranCoder> getCoders() { return bindings.values(); }
	
	public void put(String id, FortranCoder coder) {
		bindings.put(id, coder);
	}
	
	void assertNotCompiled() {
		if (did_compile) {
			throw new IllegalStateException("Bindings already compiled.");
		}
	}
	
	public Bindings clone() {
		assertNotCompiled();
		HashMap<String, FortranCoder> clone = new HashMap<String,
 				FortranCoder>();
 		clone.putAll(bindings);
 		return new Bindings(compiler, clone);
	}
}
