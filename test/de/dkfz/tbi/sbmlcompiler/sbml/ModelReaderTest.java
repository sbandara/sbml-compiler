package de.dkfz.tbi.sbmlcompiler.sbml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;
import de.dkfz.tbi.sbmlcompiler.sbml.Reaction.*;

public class ModelReaderTest {
	
	private static final String model_file = "/Tyson1991-L2V1.xml";

	@Test
	public void test() {
		ModelParser parser = new ModelParser();
		InputStream is = this.getClass().getResourceAsStream(model_file);
		try {
			Model model = parser.parse(is);
			Species m = (Species) model.getEntity("M");
			Compartment c = m.getCompartment();
			assertEquals(c.getId(), "cell");
			Reaction r1 = (Reaction) model.getEntity("Reaction1");
			ArrayList<SpeciesReference> r1s = r1.getSpecies();
			Iterator<SpeciesReference> it = r1s.iterator();
			SpeciesReference c2_ref = null;
			while (it.hasNext()) {
				SpeciesReference ref = it.next();
				if (ref.getSpecies().getId().equals("C2")) {
					c2_ref = ref;
				}
			}
			if (c2_ref == null) {
				fail();
			}
			assertEquals(c2_ref.getRefType(), ReferenceType.PRODUCT);
			AstNode root = r1.getKineticLaw();
			assertEquals(root.getType(), NodeType.AST_TIMES);
			assertEquals(root.getNumChildren(), 3);
			boolean did_rescope_k6 = false;
			String scoped_name = r1.getId() + ":k6";
			for (int k = 0; k < root.getNumChildren(); k ++) {
				AstNode child = root.getChild(k);
				if (child.getName().equals(scoped_name)) {
					did_rescope_k6 = true;
					break;
				}
			}
			assertTrue(did_rescope_k6);
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
