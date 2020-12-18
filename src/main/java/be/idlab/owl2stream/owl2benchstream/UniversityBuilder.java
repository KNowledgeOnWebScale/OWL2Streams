/**
 * 
 */
package be.idlab.owl2stream.owl2benchstream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import be.idlab.owl2stream.ScenarioGenerator;
import be.idlab.owl2stream.Utils;
import be.idlab.owl2stream.utils.SimpleMapper;

/**
 * @author psbonte
 *
 */
public class UniversityBuilder implements ScenarioGenerator {
	private OWLOntology tbox;
	private OWLOntology abox;
	private int numDepartments;
	private SimpleMapper mapper;
	private List<String> streamList;

	public UniversityBuilder(int numDepartments) {
		this.numDepartments = numDepartments;
		streamList = new ArrayList<String>();
	}

	@Override
	public OWLOntology getTBox() {
		if(this.tbox==null) {
			generate(numDepartments);
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
		// TODO Auto-generated method stub
		return  streamList.get(timeIndex);
	}

	public void generate(int numDepartments) {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		OWLOntology ont;
		try {
			ont = manager.loadOntologyFromOntologyDocument(new File("resource/OWL2DL-1.owl"));

			OWLDataFactory fact = manager.getOWLDataFactory();
			OWLEntityRemover remover = new OWLEntityRemover(ont);
			Set<OWLIndividual> students = new HashSet<OWLIndividual>();
			// remove all students
			for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {

				for (OWLObjectPropertyExpression prop : EntitySearcher.getObjectPropertyValues(ind, ont).keySet()) {
					if (prop.toString().contains("enrollIn")) {
						students.add(ind);
						ind.accept(remover);
					}
				}

			}

			int departmentCounter = 0;
			Set<OWLIndividual> courses = new HashSet<OWLIndividual>();
			Set<OWLIndividual> teachers = new HashSet<OWLIndividual>();
			// remove departments
			for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {
				for (OWLClassExpression ax : EntitySearcher.getTypes(ind, ont).collect(Collectors.toList())) {
					if (ax.toString().contains("Department")) {
						if (departmentCounter < numDepartments) {
							departmentCounter++;
							courses = EntitySearcher
									.getObjectPropertyValues(ind,
											fact.getOWLObjectProperty("http://benchmark/OWL2Bench#offerCourse"), ont)
									.collect(Collectors.toSet());
							for (OWLIndividual course : courses) {
								teachers.addAll(
										EntitySearcher
												.getObjectPropertyValues(course,
														fact.getOWLObjectProperty(
																"http://benchmark/OWL2Bench#isTaughtBy"),
														ont)
												.collect(Collectors.toList()));

							}
						} else {
							ind.accept(remover);
							for (OWLIndividual course : EntitySearcher
									.getObjectPropertyValues(ind,
											fact.getOWLObjectProperty("http://benchmark/OWL2Bench#offerCourse"), ont)
									.collect(Collectors.toList())) {
								((OWLNamedIndividual) course).accept(remover);

								for (OWLIndividual teacher : EntitySearcher
										.getObjectPropertyValues(course,
												fact.getOWLObjectProperty("http://benchmark/OWL2Bench#isTaughtBy"), ont)
										.collect(Collectors.toList())) {
									((OWLNamedIndividual) teacher).accept(remover);
								}
							}
						}
					}
				}
			}
			Map<OWLIndividual, Set<OWLAxiom>> studentAxioms = new HashMap<OWLIndividual, Set<OWLAxiom>>();
			// remove students that follow courses that are not taught in the department
			for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {

				for (OWLObjectPropertyExpression prop : EntitySearcher.getObjectPropertyValues(ind, ont).keySet()) {
					if (prop.toString().contains("enrollIn")) {
						Set<OWLIndividual> studentCourses = EntitySearcher
								.getObjectPropertyValues(ind,
										fact.getOWLObjectProperty("http://benchmark/OWL2Bench#takesCourse"), ont)
								.collect(Collectors.toSet());
						Set<OWLIndividual> ignoreCourses = new HashSet<OWLIndividual>();
						boolean departementStudent = false;
						for (OWLIndividual studentCourse : studentCourses) {
							if (courses.contains(studentCourse)) {
								departementStudent = true;

							} else {
								ignoreCourses.add(studentCourse);
							}
						}
						if (departementStudent) {
							students.add(ind);
							// ind.accept(remover);
							// extract student info
							Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
							for (Entry<OWLObjectPropertyExpression, OWLIndividual> ent : EntitySearcher
									.getObjectPropertyValues(ind, ont).entries()) {
								if (!ent.getKey()
										.equals(fact.getOWLObjectProperty("http://benchmark/OWL2Bench#takesCourse"))
										|| !ignoreCourses.contains(ent.getValue())) {
									axioms.add(
											fact.getOWLObjectPropertyAssertionAxiom(ent.getKey(), ind, ent.getValue()));
								}

							}
							for (Entry<OWLDataPropertyExpression, OWLLiteral> ent : EntitySearcher
									.getDataPropertyValues(ind, ont).entries()) {
								axioms.add(fact.getOWLDataPropertyAssertionAxiom(ent.getKey(), ind, ent.getValue()));
							}
							for (OWLClassExpression ax : EntitySearcher.getTypes(ind, ont)
									.collect(Collectors.toList())) {
								axioms.add(fact.getOWLClassAssertionAxiom(ax, ind));
							}
							studentAxioms.put(ind, axioms);
						} else {

							ind.accept(remover);
						}
					}
				}

			}
			// remove all people that are not teaching courses
			for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {

				for (OWLClassExpression ax : EntitySearcher.getTypes(ind, ont).collect(Collectors.toList())) {
					if (ax.toString().contains("Woman") || ax.toString().contains("Man")) {
						if (!teachers.contains(ind) && !students.contains(ind)) {
							ind.accept(remover);
						}

					}
				}
			}
			manager.applyChanges(remover.getChanges());
			remover.reset();
			// remove all publications that are not made by people in the departmennt
			for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {
				if (EntitySearcher
						.getDataPropertyValues(ind,
								fact.getOWLDataProperty("http://benchmark/OWL2Bench#hasPublicationDate"), ont)
						.count() > 0) {
					if (EntitySearcher
							.getObjectPropertyValues(ind,
									fact.getOWLObjectProperty("http://benchmark/OWL2Bench#hasAuthor"), ont)
							.count() == 0) {
						ind.accept(remover);
					}

				}

			}

			manager.applyChanges(remover.getChanges());
			this.tbox = ont;
			this.abox = ont;
			int counter = 0;
			for (Entry<OWLIndividual, Set<OWLAxiom>> ent : studentAxioms.entrySet()) {
				OWLOntology studentOnt;
				try {
					studentOnt = manager.createOntology(substituteStudent(ent.getValue()));
					streamList.add(Utils.getOntologyString(studentOnt));
				} catch (OWLOntologyCreationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private static Set<OWLAxiom> substituteStudent(Set<OWLAxiom> studentAxs) {
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLOntology o;
		try {
			o = m.createOntology(studentAxs);

			OWLDataFactory fact = m.getOWLDataFactory();
			final OWLEntityRenamer renamer = new OWLEntityRenamer(m, Collections.singleton(o));
			final Map<OWLEntity, IRI> entity2IRIMap = new HashMap<>();
			// find class assertion
			UUID uuid = UUID.randomUUID();

			for (OWLAxiom ax : studentAxs) {
				if (ax instanceof OWLClassAssertionAxiom) {
					OWLClassAssertionAxiom clsAx = (OWLClassAssertionAxiom) ax;
					IRI iri = clsAx.getIndividual().asOWLNamedIndividual().getIRI();
					entity2IRIMap.put(clsAx.getIndividual().asOWLNamedIndividual(),
							IRI.create(iri.toString() + "_" + uuid.toString()));
				}
			}
			o.applyChanges(renamer.changeIRI(entity2IRIMap));
			return o.getAxioms();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Collections.EMPTY_SET;
	}

}
