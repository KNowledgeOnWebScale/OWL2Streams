/**
 * 
 */
package be.idlab.owl2stream.citybenchplus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import be.idlab.owl2stream.ScenarioGenerator;
import be.idlab.owl2stream.utils.MappingFunction;
import be.idlab.owl2stream.utils.SimpleMapper;

/**
 * @author psbonte
 *
 */
public class CityBuilder implements ScenarioGenerator {

	private OWLOntology tbox;
	private OWLOntology abox;
	private int numSensors;
	private SimpleMapper mapper;
	private List<String> streamList;
	static final String mapping = 
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#>. \n" + 
			"@prefix : <http://massif/>. \n" +
			"@prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#>. \n" +
			"@prefix ct: <http://www.insight-centre.org/citytraffic#>.\n" + 
			"@prefix ses: <http://www.insight-centre.org/dataset/SampleEventService#>.\n"+
			"@prefix mas: <http://massif.streaming/ontologies/rsplab/officerepository.owl#>. \n"
			+ "ct:hasValue rdf:type owl:DatatypeProperty .\n" + 
			"ssn:observedProperty rdf:type owl:ObjectProperty .\n" +
			"ssn:observedBy rdf:type owl:ObjectProperty .\n" +
			"mas:hasDiscreteValue rdf:type owl:ObjectProperty .\n" +
			":?_id a ssn:Observation ; :eventTime \"?TIMESTAMP\"^^xsd:dateTime ; ct:hasValue \"?vehicleCount\"^^xsd:int; mas:hasDiscreteValue ?discreteVehicleCount ; ssn:observedProperty ses:vehicleCount ; ssn:observedBy ses:AarhusTrafficData186979 .\n"
			+ "ses:vehicleCount a ct:CongestionLevel. ";

	public CityBuilder(int numSensors) {
		this.numSensors = numSensors;		
		this.mapper = new SimpleMapper(mapping, true);
		this.mapper.registerFunction("discreteVehicleCount", "vehicleCount", new MappingFunction() {
		
			@Override
			public String apply(String input) {
				int parsedInt = Integer.parseInt(input);
				if (parsedInt < 5) {
					return "mas:lowValue";
				} else if (parsedInt < 15) {
					return "mas:mediumValue";
				} else if (parsedInt >= 15) {
					return "mas:highValue";
				}
				return null;
			}

		});
		String fileName = "resource/AarhusTrafficData182955.stream";

		this.streamList = new ArrayList<String>();
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {

			this.streamList = stream.limit(100).collect(Collectors.toList());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void generate(int numSensors) {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		try {
			final OWLOntology ont = manager
					.loadOntologyFromOntologyDocument(new File("resource/officerepository_all.owl"));

			OWLDataFactory fact = manager.getOWLDataFactory();
			OWLEntityRemover remover = new OWLEntityRemover(ont);
			List<String> streamNames = new ArrayList<String>();
			streamNames.add("AarhusTrafficData186979");
			Set<OWLIndividual> properties = new HashSet<OWLIndividual>();
			Set<OWLIndividual> profiles = new HashSet<OWLIndividual>();

			for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {

				for (OWLClassExpression ax : EntitySearcher.getTypes(ind, ont).collect(Collectors.toList())) {
					if (ax.toString().contains("Sensor")) {
						String name = ind.toString().split("#")[1];
						name = name.substring(0, name.length() - 1);

						if (!streamNames.contains(name) && numSensors <= 0) {
							Set<OWLIndividual> observes = EntitySearcher.getObjectPropertyValues(ind,
									fact.getOWLObjectProperty("http://purl.oclc.org/NET/ssnx/ssn#observes"), ont)
									.collect(Collectors.toSet());
							for (OWLIndividual observe : observes) {
								Set<OWLIndividual> fois = EntitySearcher
										.getObjectPropertyValues(observe,
												fact.getOWLObjectProperty(
														"http://purl.oclc.org/NET/ssnx/ssn#isPropertyOf"),
												ont)
										.collect(Collectors.toSet());
								// retrieve the offices
								for (OWLIndividual foi : fois) {
									Set<OWLIndividual> offices = EntitySearcher.getObjectPropertyValues(foi,
											fact.getOWLObjectProperty(
													"http://www.loa-cnr.it/ontologies/DUL.owl#isLocationOf"),
											ont).collect(Collectors.toSet());
									offices.stream().forEach(f -> f.asOWLNamedIndividual().accept(remover));
								}
								fois.stream().forEach(f -> f.asOWLNamedIndividual().accept(remover));
							}
							//
							properties.addAll(observes);
							Set<OWLIndividual> presents = EntitySearcher
									.getObjectPropertyValues(ind,
											fact.getOWLObjectProperty(
													"http://www.daml.org/services/owl-s/1.2/Service.owl#presents"),
											ont)
									.collect(Collectors.toSet());
							profiles.addAll(presents);

							ind.accept(remover);

						} else {
							numSensors--;
						}

					}

				}
			}
			properties.stream().forEach(p -> p.asOWLNamedIndividual().accept(remover));
			profiles.stream().forEach(p -> p.asOWLNamedIndividual().accept(remover));
			System.out.println(ont.getAxiomCount());
			manager.applyChanges(remover.getChanges());
			// remove blank nodes
			Stream<OWLAxiom> anonAxes = ont.axioms().filter(ax -> ax.toString().contains("_:genid"));
			manager.removeAxioms(ont, anonAxes);
			 
			this.abox = manager.createOntology();
			manager.addAxioms(this.abox, ont.getAxioms());
			System.out.println(ont.getAxiomCount());

			// save schema only version
			remover.reset();
			ont.individualsInSignature().forEach(p -> p.accept(remover));
			ont.axioms(AxiomType.ANNOTATION_ASSERTION).forEach(a -> manager.removeAxiom(ont, a));
			manager.applyChanges(remover.getChanges());
			this.tbox = ont;
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public OWLOntology getTBox() {
		if(this.tbox==null) {
			generate(numSensors);
		}
		return this.tbox;
	}

	@Override
	public OWLOntology getABox(int size) {
		if(this.abox==null) {
			generate(size);
		}
		return this.abox;
	}

	@Override
	public String getInstantaneousABox(int timeIndex) {
		return mapper.map(streamList.get(timeIndex));
	}
	

}
