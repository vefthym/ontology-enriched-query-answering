package com.ibm.nlq.ourqueryanswering.chase;

import com.ibm.nlq.ourqueryanswering.utils.Utilities;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyDomainAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyRangeAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubObjectPropertyOfAxiomImpl;

/**
 * Generates a set of tgds for a given normalized ELH^{fdr} TBox.
 * @deprecated use the version without normalization instead
 * @author Vasilis Efthymiou
 */
public class TgdsGeneratorAfterNormalization {

    private OWLOntology onto;

    public TgdsGeneratorAfterNormalization(OWLOntology onto) {
        this.onto = onto;
    }

    public void generateTgds() {
        generateConceptInclusionTgds(); //cases (1)-(4)
        generatedRoleHierarchyTgds();   // case (5)
        generateRoleRangeTgds();        // case (6)
        generatedRoleDomainTgds();      // case (7)
    }

    private void generateConceptInclusionTgds() {
        Stream<OWLSubClassOfAxiom> conceptDefinitionsConverted = onto.axioms(AxiomType.EQUIVALENT_CLASSES)
                .flatMap(axiom -> (((OWLEquivalentClassesAxiom) axiom).asOWLSubClassOfAxioms()).stream());

        Stream<OWLSubClassOfAxiom> conceptInclusions = onto.axioms(AxiomType.SUBCLASS_OF)
                .filter(axiom -> !((OWLSubClassOfAxiom) axiom).getSuperClass().isTopEntity()); //exclude subClass of owl:Thing

        List<OWLSubClassOfAxiom> allSubClassOfAxioms = Stream.concat(conceptDefinitionsConverted, conceptInclusions).collect(Collectors.toList());

        List<OWLSubClassOfAxiom> gcis = allSubClassOfAxioms.stream().filter(axiom -> axiom.isGCI()).collect(Collectors.toList());
        List<OWLSubClassOfAxiom> nonGcis = allSubClassOfAxioms.stream().filter(axiom -> !axiom.isGCI()).collect(Collectors.toList());

        //case (1): lhs is a single concept and rhs is an existentially quantified concept    
        nonGcis.stream()
                .filter(axiom -> !axiom.getSuperClass().isClassExpressionLiteral() && !(axiom.getSuperClass().asConjunctSet().size() > 1))
                .forEach(axiom -> {                    
                    String subClass = axiom.getSubClass().toString();
                    subClass = subClass.substring(subClass.lastIndexOf("/") + 1, subClass.length() - 1);
                    
                    OWLObjectSomeValuesFrom existential = (OWLObjectSomeValuesFrom) axiom.getSuperClass();
                    //System.out.println(axiom);
                    String role = existential.getProperty().toString();
                    role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);
                    
                    String filler = existential.getFiller().toString();
                    filler = filler.substring(filler.lastIndexOf("/") + 1, filler.length() - 1);
                    
                    String tgd = subClass + "(?X) -> "+ role +"(?X,?Y), "+filler+"(?Y) .";
                    System.out.println(tgd);
                }); 
        
        //case (2): lhs is existential and rhs is single concept
        gcis.stream().filter(axiom -> !axiom.getSubClass().isClassExpressionLiteral() && !(axiom.getSubClass().asConjunctSet().size() > 1))
                .forEach(axiom -> {                                                            
                    OWLObjectSomeValuesFrom existential = (OWLObjectSomeValuesFrom) axiom.getSubClass();
                    //System.out.println(axiom);
                    String role = existential.getProperty().toString();
                    role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);
                    
                    String filler = existential.getFiller().toString();
                    filler = filler.substring(filler.lastIndexOf("/") + 1, filler.length() - 1);
                    
                    String superClass = axiom.getSuperClass().toString();
                    superClass = superClass.substring(superClass.lastIndexOf("/") + 1, superClass.length() - 1);

                    System.out.println(role + "(?X,?X1), "+filler+"(?X1) -> " + superClass + "(?X) .");
                });
        
        //case (3)-part: lhs and rhs are both single concepts
        nonGcis.stream().filter(axiom -> axiom.getSuperClass().isOWLClass())
                .forEach(axiom -> {
                    String subClass = axiom.getSubClass().toString();
                    subClass = subClass.substring(subClass.lastIndexOf("/") + 1, subClass.length() - 1);

                    String superClass = axiom.getSuperClass().toString();
                    superClass = superClass.substring(superClass.lastIndexOf("/") + 1, superClass.length() - 1);

                    System.out.println(subClass + "(?X) -> " + superClass + "(?X) .");
                });

        //case (3)-part: lhs is a single concept and rhs is an intersection of concepts
        nonGcis.stream()
                .filter(axiom -> axiom.getSuperClass().asConjunctSet().size() > 1)
                .forEach(axiom -> {                    
                    String subClass = axiom.getSubClass().toString();
                    subClass = subClass.substring(subClass.lastIndexOf("/") + 1, subClass.length() - 1);
                    
                    Set<OWLClassExpression> superClasses = axiom.getSuperClass().asConjunctSet();
                    String tgd = subClass + "(?X) -> ";
                    for (OWLClassExpression superClassExp : superClasses) {
                        String superClass = superClassExp.toString();
                        superClass = superClass.substring(superClass.lastIndexOf("/") + 1, superClass.length() - 1);
                        tgd += superClass+"(?X), ";
                    }                    
                    tgd = tgd.substring(0, tgd.length()-2) + " .";
                    System.out.println(tgd);
                });        

        //case (4): lhs is an intersection of concepts and rhs is a single concept
        gcis.stream()
                .filter(axiom -> axiom.getSubClass().asConjunctSet().size() > 1)
                .forEach(axiom -> {                    
                    
                    
                    Set<OWLClassExpression> subClasses = axiom.getSubClass().asConjunctSet();
                    String tgd = "";
                    for (OWLClassExpression subClassExp : subClasses) {
                        String subClass = subClassExp.toString();
                        subClass = subClass.substring(subClass.lastIndexOf("/") + 1, subClass.length() - 1);
                        tgd += subClass+"(?X), ";
                    }               
                    
                    String superClass = axiom.getSuperClass().toString();
                    superClass = superClass.substring(superClass.lastIndexOf("/") + 1, superClass.length() - 1);
                    
                    tgd = tgd.substring(0, tgd.length()-2) + " -> "+superClass+"(?X) .";
                    System.out.println(tgd);
                });             
    }

    // case (5)
    protected void generatedRoleHierarchyTgds() {
        onto.axioms(AxiomType.SUB_OBJECT_PROPERTY)
                .forEach(axiom -> {
                    OWLSubObjectPropertyOfAxiomImpl subObjProp = (OWLSubObjectPropertyOfAxiomImpl) axiom;
                    String subProperty = subObjProp.getSubProperty().toString(); //TODO: better write labels intstead of IRIs
                    subProperty = subProperty.substring(subProperty.lastIndexOf("/") + 1, subProperty.length() - 1);

                    String superProperty = subObjProp.getSuperProperty().toString(); //TODO: better write labels intstead of IRIs
                    superProperty = superProperty.substring(superProperty.lastIndexOf("/") + 1, superProperty.length() - 1);

                    System.out.println(subProperty + "(?X,?Y) -> " + superProperty + "(?X,?Y) .");
                });
    }

    // case (6)
    private void generateRoleRangeTgds() {
        onto.axioms(AxiomType.OBJECT_PROPERTY_RANGE)
                .forEach(axiom -> {
                    OWLObjectPropertyRangeAxiomImpl rngAxiom = (OWLObjectPropertyRangeAxiomImpl) axiom;
                    String range = rngAxiom.getRange().toString(); //TODO: better write labels intstead of IRIs
                    range = range.substring(range.lastIndexOf("/") + 1, range.length() - 1);

                    String role = rngAxiom.getProperty().toString();
                    role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);

                    System.out.println(role + "(?X,?X1) -> " + range + "(?X1) .");
                });
    }

    // case (7)
    private void generatedRoleDomainTgds() {

        onto.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN)
                .forEach(axiom -> {
                    //System.out.println(axiom);
                    OWLObjectPropertyDomainAxiomImpl domAxiom = (OWLObjectPropertyDomainAxiomImpl) axiom;
                    String domain = domAxiom.getDomain().toString(); //TODO: better write labels intstead of IRIs
                    domain = domain.substring(domain.lastIndexOf("/") + 1, domain.length() - 1);

                    String role = domAxiom.getProperty().toString();
                    role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);

                    System.out.println(role + "(?X,?X1) -> " + domain + "(?X) .");
                });
    }

}
