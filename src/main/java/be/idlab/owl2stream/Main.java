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
import be.idlab.owl2stream.owl2benchstream.UniversityBuilder;

import static spark.Spark.*;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author psbonte
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length==0) {
			System.out.println("USAGE: <type>[University|City|Building] <size>[int]");
		}else {
		
		port(9000);
		String scenarioType = args[0];
		int size = Integer.parseInt(args[1]);
		System.out.println("Get the Ontology TBox at: <url>/tbox\n"
				+ "Get the Ontology ABox at: <url>/abox\n"
				+ "Pull the stream at: <url>/event");
		if(scenarioType.toLowerCase().equals("university")) {
			System.out.println(String.format("Staring University Stream Generator with %s departments", size));
			//ScenarioGenerator builder = new CityBuilder(1);
			ScenarioGenerator builder = new UniversityBuilder(size);
			OWLOntology tbx = builder.getTBox();
			OWLOntology abox = builder.getABox(size);
			 get("/abox", (req, res) -> Utils.getOntologyString(abox));
			 get("/tbox", (req, res) -> Utils.getOntologyString(tbx));
			AtomicInteger counter = new AtomicInteger(0);
			get("/event", (req, res) -> builder.getInstantaneousABox(counter.getAndIncrement()));
			
		}else {
			System.out.println(String.format("Staring City Stream Generator with %s squares", size));

			ScenarioGenerator builder = new CityBuilder(size);
			OWLOntology tbx = builder.getTBox();
			OWLOntology abox = builder.getABox(size);
			 get("/abox", (req, res) -> Utils.getOntologyString(abox));
			 get("/tbox", (req, res) -> Utils.getOntologyString(tbx));
			 builder.getInstantaneousABox(0);
			AtomicInteger counter = new AtomicInteger(1);
			get("/event", (req, res) -> builder.getInstantaneousABox(counter.getAndIncrement()));
		
		}
		}

	}
	
	

}
