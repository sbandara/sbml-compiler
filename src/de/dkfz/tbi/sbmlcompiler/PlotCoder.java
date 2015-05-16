package de.dkfz.tbi.sbmlcompiler;

import java.util.ArrayList;
import java.util.Iterator;

import de.dkfz.tbi.sbmlcompiler.sbml.SbmlBase;

class PlotCoder extends FortranCoder {
	
	ArrayList<String> outputs = new ArrayList<String>();
	
	void putFortranCode(FortranFunction target, Bindings bindings)
			throws SbmlCompilerException {
		int n_var = 0;
    	String write_stmt = "WRITE(10,100) t";
    	for (Iterator<String> i = outputs.iterator(); i.hasNext();)
    	{
    		String name = i.next();
    		FortranCoder coder = bindings.get(name);
    		if (coder == null) {
    			throw new SbmlCompilerException(SbmlCompilerException
    					.UNKNOWN_MODEL_ENTITY, name);
    		}
			write_stmt += ", " + coder.getVarName();
    	}
    	target.appendStatement(write_stmt);
    	target.appendStatement(100, "FORMAT(E20.10," + n_var +
    			"(1X,E20.10))");
	}
	
	void registerToFunction(ArrayList<FortranFunction> code) {
		code.get(SbmlCompiler.PLOTFCN).outputs.add(this);
	}
	
	PlotCoder(ArrayList<String> outputs, SbmlCompiler compiler) {
		super(compiler);
		this.outputs = outputs;
	}

	String getPrefix() { return "plot"; }

	public SbmlBase getSbmlNode() { return null; }

	protected void initialize(Bindings bindings) throws SbmlCompilerException {
		for (Iterator<String> k = outputs.iterator(); k.hasNext();) {
			addDepend(k.next());
		}
	}
}
