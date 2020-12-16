/**
 * 
 */
package be.idlab.owl2stream;

import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import be.idlab.owl2stream.citybenchplus.CityBuilder;
import static spark.Spark.*;


/**
 * @author psbonte
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		port(9000);
       
		CityBuilder builder = new CityBuilder(1);
		
		OWLOntology tbx = builder.getTBox();
		OWLOntology abox = builder.getABox(1);
		 get("/abox", (req, res) -> getOntologyString(abox));
		 get("/tbox", (req, res) -> getOntologyString(tbx));
		String mapped = builder.getInstantaneousABox(0);
		mapped = builder.getInstantaneousABox(1);

	}
	
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
