package de.dkfz.tbi.sbmlcompiler.sbml;

import java.util.ArrayList;
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dkfz.tbi.sbmlcompiler.SbmlCompilerException;

public class Reaction extends SbmlBase {
	
	public enum ReferenceType { REACTANT, MODIFIER, PRODUCT }
	
	public class SpeciesReference {
		
		private Species species = null;
		private final String species_key;
		private final Integer stoichiometry;
		private AstNode stoichiometry_math;
		private final ReferenceType ref_type;
		
		SpeciesReference(ReferenceType ref_type, String species_key,
				Integer stoichiometry) {
			this.stoichiometry = stoichiometry;
			this.stoichiometry_math = null;
			this.species_key = species_key;
			this.ref_type = ref_type;
			Reaction.this.species.add(this);
		}
		
		void setStoichiometryMath(AstNode stoichiometry_math) {
			if (stoichiometry != null) {
				new IllegalStateException("Stoichiometry already defined.");
			}
			this.stoichiometry_math = stoichiometry_math;
		}
		
		public Species getSpecies() throws SbmlCompilerException {
			if (species == null) {
				try {
					species = (Species) model.getEntity(species_key);
				}
				catch (ClassCastException e) { }
				if (species == null) {
					throw new SbmlCompilerException(SbmlCompilerException
							.UNKNOWN_MODEL_ENTITY, species_key);
				}
			}
			return species;
		}
		
		public Integer getStoichiometry() {
			if (ref_type != ReferenceType.MODIFIER) {
				if (stoichiometry_math != null) {
					return null;
				}
				if (stoichiometry == null) {
					return 1;
				}
				return stoichiometry;
			}
			throw new IllegalStateException("Modifiers have no stoichiometry.");
		}
		
		public AstNode getStochiometryMath() { return stoichiometry_math; }
		
		public ReferenceType getRefType() { return ref_type; }
	}
	
	private final boolean is_reversible;
	private final Model model;
	private AstNode kinetic_law = null;
	private final ArrayList<SpeciesReference> species =
			new ArrayList<SpeciesReference>();
	private final ArrayList<Parameter> parameters = new ArrayList<Parameter>();

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

	void addParameter(Parameter parameter) { parameters.add(parameter); }
	
	void resolveLocalScope() {
		Iterator<Parameter> it = parameters.iterator();
		while (it.hasNext()) {
			Parameter local_parameter = it.next();
			String local_id = local_parameter.id;
			local_parameter.id = id + ':' + local_parameter.id;
			kinetic_law.rescope(local_id, local_parameter.id);
			model.entities.put(local_parameter.id, local_parameter);
		}
	}
}
