package com.ibm.nlq.ourqueryanswering.chase;

import com.ibm.nlq.ourqueryanswering.utils.Utilities;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyDomainAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyRangeAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubObjectPropertyOfAxiomImpl;

/**
 *
 * @author Vasilis Efthymiou
 */
public class TTgdsGenerator {

    private OWLOntology onto;
    private String outputFilePath;
    private int globalExistenialsUsed;

    public TTgdsGenerator(OWLOntology onto, String outputFilePath) {
        this.onto = onto;
        this.outputFilePath = outputFilePath;
        new File(outputFilePath).delete();
        globalExistenialsUsed = 0;
    }

    public void generateTgds() {
        generateConceptInclusionTgds();                         //cases (1)-(4) 34 tgds of 1&3, 11 tgds of 2&4
        generatedRoleHierarchyTgds();                           // case (5)     1 tgd
        generateRoleRangeTgds();                                //case (6)      54 tgds
        generatedRoleDomainTgds();                              //case (7)      54 tgds
    }

    private void generateConceptInclusionTgds() {
        Stream<OWLSubClassOfAxiom> conceptDefinitionsConverted = onto.axioms(AxiomType.EQUIVALENT_CLASSES)
                .flatMap(axiom -> (((OWLEquivalentClassesAxiom) axiom).asOWLSubClassOfAxioms()).stream());

        Stream<OWLSubClassOfAxiom> conceptInclusions = onto.axioms(AxiomType.SUBCLASS_OF)
                .filter(axiom -> !((OWLSubClassOfAxiom) axiom).getSuperClass().isTopEntity()); //exclude subClass of owl:Thing

        List<OWLSubClassOfAxiom> allSubClassOfAxioms = Stream.concat(conceptDefinitionsConverted, conceptInclusions).collect(Collectors.toList());

        List<OWLSubClassOfAxiom> gcis = allSubClassOfAxioms.stream().filter(axiom -> axiom.isGCI()).collect(Collectors.toList());
        List<OWLSubClassOfAxiom> nonGcis = allSubClassOfAxioms.stream().filter(axiom -> !axiom.isGCI()).collect(Collectors.toList());

        //cases (1) & (3): lhs is a single concept and rhs is an existentially quantified expression (case 1) or intersection (case 3)    
        nonGcis.stream()
              .forEach(axiom -> convertOWLAxiomToTGD(axiom));
        
        //cases (2) & (4): lhs is a class expression and rhs is a single concept
        gcis.stream()
                .forEach(axiom -> convertGCIAxiomToTGD(axiom));
    }
    
    // case (5)
    protected void generatedRoleHierarchyTgds() {
        onto.axioms(AxiomType.SUB_OBJECT_PROPERTY)
                .forEach(axiom -> {
                    OWLSubObjectPropertyOfAxiomImpl subObjProp = (OWLSubObjectPropertyOfAxiomImpl) axiom;
                    String subProperty = subObjProp.getSubProperty().toString(); //TODO: better write labels intstead of IRIs
                    subProperty = subProperty.substring(subProperty.lastIndexOf("/") + 1, subProperty.length() - 1);
                    subProperty = subProperty.replace("#", "_"); //just for chase bench

                    String superProperty = subObjProp.getSuperProperty().toString(); //TODO: better write labels intstead of IRIs
                    superProperty = superProperty.substring(superProperty.lastIndexOf("/") + 1, superProperty.length() - 1);
                    superProperty = superProperty.replace("#", "_"); //just for chase bench
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
                        bw.write(subProperty + "(?X,?Y) -> " + superProperty + "(?X,?Y) .");
                        bw.newLine();
                    } catch (IOException ex) {
                        Logger.getLogger(TTgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }                    
                });
    }

    // case (6)
    private void generateRoleRangeTgds() {
        onto.axioms(AxiomType.OBJECT_PROPERTY_RANGE)
                .forEach(axiom -> {
                    OWLObjectPropertyRangeAxiomImpl rngAxiom = (OWLObjectPropertyRangeAxiomImpl) axiom;
                    String range = convertClassExpressionToCQ(axiom.getRange(), "", 1);

                    String role = rngAxiom.getProperty().toString();
                    role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);
                    role = role.replace("#", "_"); //just for chase bench
                    role = role.toUpperCase();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
                        bw.write(role + "(?X,?X1) -> " + range + " .");
                        bw.newLine();
                    } catch (IOException ex) {
                        Logger.getLogger(TTgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
    }

    // case (7)
    private void generatedRoleDomainTgds() {

        onto.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN)
                .forEach(axiom -> {
                    //System.out.println(axiom);
                    OWLObjectPropertyDomainAxiomImpl domAxiom = (OWLObjectPropertyDomainAxiomImpl) axiom;
                    String domain = convertClassExpressionToCQ(axiom.getDomain(), "", 0);

                    String role = domAxiom.getProperty().toString();
                    role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);
                    role = role.replace("#", "_"); //just for chase bench
                    role = role.toUpperCase();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
                        bw.write(role + "(?X,?X1) -> " + domain + " .");
                        bw.newLine();
                    } catch (IOException ex) {
                        Logger.getLogger(TTgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
    }

    private void convertOWLAxiomToTGD(OWLAxiom axiom) {
        //System.out.println(axiom);
        OWLClassExpression subClass = ((OWLSubClassOfAxiom) axiom).getSubClass();
        OWLClassExpression superClass = ((OWLSubClassOfAxiom) axiom).getSuperClass();

        String subClassName = subClass.toString();
        subClassName = subClassName.substring(subClassName.lastIndexOf("/") + 1, subClassName.length() - 1);
        String tgd = subClassName + "(?X) -> ";

        tgd += convertClassExpressionToCQ(superClass, "", 0);
        tgd += " .";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            bw.write(tgd.toUpperCase());
            bw.newLine();
        } catch (IOException ex) {
            Logger.getLogger(TTgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    //lhs is a class expression, rhs is a single concept
    private void convertGCIAxiomToTGD(OWLAxiom axiom) {
        //System.out.println(axiom);
        OWLClassExpression superClass = ((OWLSubClassOfAxiom) axiom).getSuperClass();
        OWLClassExpression subClass = ((OWLSubClassOfAxiom) axiom).getSubClass();

        String superClassName = superClass.toString();
        superClassName = superClassName.substring(superClassName.lastIndexOf("/") + 1, superClassName.length() - 1);
        String tgd = "";

        tgd += convertClassExpressionToCQ(subClass, "", 0);
        tgd += " -> " + superClassName + "(?X) .";
        //axiom.components().forEach(component -> System.out.print(" "+component));
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            bw.write(tgd);
            bw.newLine();
        } catch (IOException ex) {
            Logger.getLogger(TTgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Recursively converts an OWLClassExpression to a Conjunctive Query.
     *
     * @param cExp
     * @param currentResult
     * @param currDepth
     * @return
     */
    private String convertClassExpressionToCQ(OWLClassExpression cExp, String currentResult, int currDepth) {
        if (currDepth == 0) {
            globalExistenialsUsed = 0;
        }
        if (cExp.isOWLClass()) {
            String conceptName = cExp.toString();
            conceptName = conceptName.substring(conceptName.lastIndexOf("/") + 1, conceptName.length() - 1);
            return currentResult + (currentResult.isEmpty() ? "" : ", ") + conceptName.toUpperCase() + "(?X" + (currDepth == 0 ? "" : currDepth) + ")";
        } else {
            //two possible cases here: cExp is an existentialy qualified expr, or an intersection
            if (cExp.asConjunctSet().size() > 1) {      //in that case, we have intesection
                String nestedResult = "";
                for (OWLClassExpression nestedExp : cExp.asConjunctSet()) {
                    //System.out.println("Recursive calling with params:" + nestedExp + ", " + currentResult + ", " + numUsedExistentials);
                    nestedResult += convertClassExpressionToCQ(nestedExp, currentResult, currDepth) + ", ";
                }
                return nestedResult.substring(0, nestedResult.length() - 2);
            } else {                                   //in that case, we have existenailly qualified expr (role)
                OWLObjectSomeValuesFrom existential = (OWLObjectSomeValuesFrom) cExp;
                String role = existential.getProperty().toString();
                role = role.substring(role.lastIndexOf("/") + 1, role.length() - 1);

                OWLClassExpression fillerExp = existential.getFiller();
                //System.out.println("Recursive calling with params:" + fillerExp + ", " + currentResult + ", " + numUsedExistentials);
                return role + "(?X" + (currDepth == 0 ? "" : currDepth)
                        + ", ?X" + (++globalExistenialsUsed) + "), " + convertClassExpressionToCQ(fillerExp, currentResult, globalExistenialsUsed);
            }

            //return currentResult + (currentResult.isEmpty() ? "" : ", ") + cExp + "(?X" + (++numUsedExistentials) + ")";
        }
    }

}
