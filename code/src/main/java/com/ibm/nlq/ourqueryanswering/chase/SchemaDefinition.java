package com.ibm.nlq.ourqueryanswering.chase;

import com.ibm.nlq.ourqueryanswering.utils.Utilities;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Generates a schema in the format readable by chase bench, from a given
 * ontology. See
 * https://github.com/dbunibas/chasebench/tree/master/utilities/parser
 *
 * Use to generate the target schema only. 
 * For source schema, manually edit the sql schema file. 
 * @author Vasilis Efthymiou
 */
public class SchemaDefinition {

    private OWLOntology onto;
    private String outputFile;

    public SchemaDefinition(OWLOntology onto, String outputFile) {
        this.onto = onto;
        this.outputFile = outputFile;
    }

    //like a simplified OWL2RDB, that writes in the format readable from chase bench
    public void generateTargetSchema() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            //adding unary relations for classes and their identifiers
            onto.classesInSignature()
                    .filter(owlClass -> !owlClass.isTopEntity())
                    .forEach(owlClass -> {
                        String classIRI = owlClass.getIRI().getShortForm().toUpperCase();
                        try {
                            bw.write(classIRI + " {");
                            bw.newLine();
                            bw.write("\t" + classIRI + "ID : INTEGER"); //assume that's always the identifier (primary key)
                            bw.newLine();
                            bw.write("}\n");
                            bw.newLine();
                        } catch (IOException ex) {
                            Logger.getLogger(SchemaDefinition.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });

            //adding functional data properties
            onto.axioms(AxiomType.DATA_PROPERTY_DOMAIN)
                    //.filter(domain -> domain.getDomain().equals(owlClass)) //get the properties that have this class as their domain
                    .forEach(domainAxiom -> {
                        String propertyName = domainAxiom.getProperty().toString();
                        String domainClassIRI = domainAxiom.getDomain().asOWLClass().getIRI().getShortForm();

                        try {
                            if (dataPropertyIsFunctional(domainAxiom.getProperty())) {
                                String shortenedPropertyName = shortenURI(propertyName).toUpperCase();
                                bw.write(shortenedPropertyName + " {");
                                bw.newLine();
                                bw.write("\t" + domainClassIRI.toUpperCase() + "ID : INTEGER"); //assume that's always the identifier (primary key)
                                onto.axioms(AxiomType.DATA_PROPERTY_RANGE)
                                        .filter(range -> range.getProperty().toString().equals(propertyName))
                                        .forEach(range -> {
                                            try {          
                                                String pos2Name = shortenURI(propertyName).toUpperCase();
                                                if (pos2Name.contains("_")) {
                                                    pos2Name = pos2Name.substring(pos2Name.indexOf("_")+1);
                                                }
                                                bw.write(",\n\t" + pos2Name + " : " + replaceDataType(range.getRange()));                                                
                                                bw.write("\n}\n");
                                                bw.newLine();
                                            } catch (IOException ex) {
                                                Logger.getLogger(SchemaDefinition.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        });
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(SchemaDefinition.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    });

            //adding functional object properties
            onto.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN)
                    .forEach(domainAxiom -> {
                        String propertyName = domainAxiom.getProperty().toString();
                        String domainClassIRI = domainAxiom.getDomain().asOWLClass().getIRI().getShortForm();

                        if (objectPropertyIsFunctional(domainAxiom.getProperty())) {
                            onto.axioms(AxiomType.OBJECT_PROPERTY_RANGE)
                                    .filter(rangeAxiom -> rangeAxiom.getProperty().toString().equals(propertyName))
                                    .forEach(rangeAxiom -> {
                                        String rangeClassIRI = rangeAxiom.getRange().asOWLClass().getIRI().getShortForm();
                                        try {

                                            bw.write(shortenURI(propertyName) + " {");
                                            bw.newLine();
                                            bw.write("\t" + domainClassIRI.toUpperCase() + "ID : INTEGER"); //assume that's always the identifier (primary key)
                                            bw.write(",\n\t" + rangeClassIRI.toUpperCase() + "ID : INTEGER");
                                            bw.write("\n}\n");
                                            bw.newLine();
                                        } catch (IOException ex) {
                                            Logger.getLogger(SchemaDefinition.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    });
                        }
                    });

            //adding binary relations for non-functional object properties 
            onto.objectPropertiesInSignature()
                    .filter(objectProp -> !objectPropertyIsFunctional(objectProp)) //exclude functionalOnes
                    .forEach(objectProp -> {
                        String propertyIRI = shortenURI(objectProp.toString());
                        try {
                            bw.write(propertyIRI.toUpperCase() + " {");
                            bw.newLine();                            
                            
                            IRI domainClassIRI = getDomainOfObjectProperty(objectProp).orElse(IRI.create("http://www.w3.org/2002/07/owl#pos1"));
                            IRI rangeClassIRI = getRangeOfObjectProperty(objectProp).orElse(IRI.create("http://www.w3.org/2002/07/owl#pos2"));

                            bw.write("\t" + domainClassIRI.getShortForm().toUpperCase() + " : INTEGER");
                            bw.write(",\n\t" + rangeClassIRI.getShortForm().toUpperCase() + " : INTEGER");

                            bw.write("\n}\n");
                            bw.newLine();
                        } catch (IOException ex) {
                            Logger.getLogger(SchemaDefinition.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
            //adding binary relations for non-functional data properties
            //TODO: not yet implemented (but not encountered in our data)

        } catch (IOException ex) {
            Logger.getLogger(SchemaDefinition.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

///////////////////////
// AUXILIARY METHODS //
///////////////////////
    private Optional<IRI> getDomainOfObjectProperty(OWLObjectProperty objectProp) {
        return onto.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN)
                .filter(domainAxiom -> domainAxiom.getProperty().toString().equals(objectProp.toString()))
                .map(domainAxiom -> domainAxiom.getDomain().asOWLClass().getIRI())
                .findFirst();
    }

    private Optional<IRI> getRangeOfObjectProperty(OWLObjectProperty objectProp) {
        return onto.axioms(AxiomType.OBJECT_PROPERTY_RANGE)
                .filter(rangeAxiom -> rangeAxiom.getProperty().toString().equals(objectProp.toString()))
                .map(rangeAxiom -> rangeAxiom.getRange().asOWLClass().getIRI())
                .findFirst();
    }

    private boolean dataPropertyIsFunctional(OWLDataPropertyExpression property) {
        return onto.axioms(AxiomType.FUNCTIONAL_DATA_PROPERTY)
                .filter(funcDataProp -> funcDataProp.getProperty().equals(property))
                .count() > 0;
    }

    private boolean objectPropertyIsFunctional(OWLObjectPropertyExpression property) {
        return onto.axioms(AxiomType.FUNCTIONAL_OBJECT_PROPERTY)
                .filter(funcObjectProp -> funcObjectProp.getProperty().equals(property))
                .count() > 0;
    }

    private String shortenURI(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1, uri.length() - 1)
                .replace("#", "_") //chaseBench recognizes '#' as comment delimiter
                .toUpperCase();  //chaseBench is case-sensitive
    }

    private String replaceDataType(OWLDataRange datatype) {
        switch (datatype.toString()) {
            case "xsd:int":
                return "INTEGER";
            case "xsd:string":
                return "STRING";
            default:
                return "STRING";
        }
    }

    public OWLOntology getOnto() {
        return onto;
    }

    public void setOnto(OWLOntology onto) {
        this.onto = onto;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

}
