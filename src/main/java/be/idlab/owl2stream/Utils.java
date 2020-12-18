/**
 * 
 */
package be.idlab.owl2stream;

import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author psbonte
 *
 */
public class Utils {
	public static String getOntologyString(OWLOntology ont) {
		String ontStr="";
		try {
			OWLOntologyManager manager = ont.getOWLOntologyManager();
			StringDocumentTarget target = new StringDocumentTarget();
			TurtleDocumentFormat turtleFormat = new TurtleDocumentFormat();
			manager.saveOntology(ont,turtleFormat, target);
			ontStr = target.toString();
		} catch ( OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ontStr;
	}

}
