package de.dkfz.tbi.sbmlcompiler.sbml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;
import de.dkfz.tbi.sbmlcompiler.sbml.Rule.RuleType;
import de.dkfz.tbi.sbmlcompiler.sbml.Reaction.*;

public class ModelReaderTest {
	
	private static final String TYSON_FILE = "/Tyson1991-L2V1.xml",
			MITCHELL_FILE = "/Mitchell2013-L2V1.xml";

	@Test
	public void testWithTyson1991() throws Exception {
		ModelParser parser = new ModelParser();
		InputStream is = this.getClass().getResourceAsStream(TYSON_FILE);
		try {
			Model model = parser.parse(is);
			Species m = (Species) model.getEntity("M");
			Compartment c = m.getCompartment();
			assertEquals(c.getId(), "cell");
			Reaction r1 = (Reaction) model.getEntity("Reaction1");
			ArrayList<SpeciesReference> r1s = r1.getSpecies();
			Iterator<SpeciesReference> ref_it = r1s.iterator();
			SpeciesReference c2_ref = null;
			while (ref_it.hasNext()) {
				SpeciesReference ref = ref_it.next();
				if (ref.getSpecies().getId().equals("C2")) {
					c2_ref = ref;
				}
			}
			assertEquals(c2_ref.getRefType(), ReferenceType.PRODUCT);
			AstNode kinetic_law = r1.getKineticLaw();
			assertEquals(kinetic_law.getType(), NodeType.AST_TIMES);
			Rule ct_rule = null; 
			for (int k = 0; k < model.getNumRules(); k ++) {
				Rule rule = model.getRule(k);
				if (rule.getVariable().getId().equals("CT")) {
					ct_rule = rule;
				}
			}
			assertEquals(ct_rule.getType(), RuleType.ASSIGNMENT);
			AstNode expression = ct_rule.getExpression();
			assertEquals(expression.getType(), NodeType.AST_PLUS);
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ignore) { }
		}
	}
	
	@Test
	public void testWithMitchell2013() throws Exception {
		ModelParser parser = new ModelParser();
		InputStream is = this.getClass().getResourceAsStream(MITCHELL_FILE);
		try {
			Model model = parser.parse(is);
			Function f3 = (Function) model.getEntity("function_3");
			assertEquals(f3.getNumArguments(), 4);
			assertEquals(f3.getArgument(3), "K");
			AstNode def = f3.getDefinition();
			assertEquals(def.getType(), NodeType.AST_TIMES);
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ignore) { }
		}
	}
}
