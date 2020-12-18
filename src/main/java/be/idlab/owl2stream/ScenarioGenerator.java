/**
 * 
 */
package be.idlab.owl2stream;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author psbonte
 *
 */
public interface ScenarioGenerator {
	
	public OWLOntology getTBox();
	
	public OWLOntology getABox(int size);
	
	public String getInstantaneousABox(int timeIndex);

}
