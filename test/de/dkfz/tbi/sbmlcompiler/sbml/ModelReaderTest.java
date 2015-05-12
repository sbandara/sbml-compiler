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
	
	private static final String model_file = "/Tyson1991-L2V1.xml";

	@Test
	public void testWithTyson1991() {
		ModelParser parser = new ModelParser();
		InputStream is = this.getClass().getResourceAsStream(model_file);
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
			assertEquals(kinetic_law.getNumChildren(), 3);
			boolean did_rescope_k6 = false;
			String scoped_name = r1.getId() + ":k6";
			for (int k = 0; k < kinetic_law.getNumChildren(); k ++) {
				AstNode child = kinetic_law.getChild(k);
				if (child.getName().equals(scoped_name)) {
					did_rescope_k6 = true;
					break;
				}
			}
			assertTrue(did_rescope_k6);
			ArrayList<Rule> rules = model.getRules();
			Iterator<Rule> rule_it = rules.iterator();
			Rule ct_rule = null; 
			while (rule_it.hasNext()) {
				Rule rule = rule_it.next();
				if (rule.getVariable().getId().equals("CT")) {
					ct_rule = rule;
				}
			}
			assertEquals(ct_rule.getType(), RuleType.ASSIGNMENT);
			AstNode expression = ct_rule.getExpression();
			assertEquals(expression.getType(), NodeType.AST_PLUS);
			assertEquals(expression.getNumChildren(), 4);
		}
		catch (Exception e) {
			e.printStackTrace();
			try {
				is.close();
			}
			catch (IOException ignore) { }
			fail();
		}
	}
}