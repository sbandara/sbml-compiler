package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class Reaction extends SbmlBase {
	
	public enum ReferenceType { REACTANT, MODIFIER, PRODUCT }
	
	public class SpeciesReference {
		
		private Species species = null;
		private final String species_key;
		private final int stoichiometry;
		private AstNode stoichiometry_math;
		private final ReferenceType ref_type;
		
		SpeciesReference(ReferenceType ref_type, String species_key,
				int stoichiometry) {
			this.stoichiometry = (ref_type == ReferenceType.MODIFIER) ? -1 :
				stoichiometry;
			this.stoichiometry_math = null;
			this.species_key = species_key;
			this.ref_type = ref_type;
			Reaction.this.species.add(this);
		}
		
		SpeciesReference(ReferenceType ref_type, String species_key,
				AstNode stoichiometry_math) {
			this(ref_type, species_key, -1);
			this.stoichiometry_math = (ref_type == ReferenceType.MODIFIER) ?
					null : stoichiometry_math;
		}
		
		public Species getSpecies() throws SbmlCompilerException {
			if (species == null) {
				try {
					species = (Species) model.getEntity(species_key);
				}
				catch (ClassCastException e) {
					throw new SbmlCompilerException(SbmlCompilerException
							.UNKNOWN_MODEL_ENTITY, e);
				}
			}
			return species;
		}
		
		
		public int getStoichiometry() {
			if (ref_type != ReferenceType.MODIFIER) {
				return stoichiometry;
			}
			throw new IllegalStateException("Modifiers have no stoichiometry.");
		}
		
		public AstNode getStochiometryMath() { return stoichiometry_math; }
	}
	
	private final boolean is_reversible;
	private final Model model;
	private AstNode kinetic_law = null;
	private final ArrayList<SpeciesReference> species =
			new ArrayList<SpeciesReference>();

	Reaction(Model model, Attributes atts) throws SAXException {
		super(atts);
		this.model = model;
		is_reversible = getBoolAttOpt(atts, "reversible", true);
	}
	
	public boolean isReversible() { return is_reversible; }

	void setKineticLaw(AstNode root_node) { kinetic_law = root_node; }
	
	public AstNode getKineticLaw() { return kinetic_law; }
	
	public ArrayList<SpeciesReference> getSpecies() {
		return new ArrayList<SpeciesReference>(species);
	}
}
