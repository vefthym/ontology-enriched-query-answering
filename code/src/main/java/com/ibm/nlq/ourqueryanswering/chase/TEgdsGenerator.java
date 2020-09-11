package com.ibm.nlq.ourqueryanswering.chase;

import com.ibm.nlq.ourqueryanswering.utils.Utilities;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author Vasilis Efthymiou
 */
public class TEgdsGenerator {

    private OWLOntology onto;
    private String outputFilePath;

    public TEgdsGenerator(OWLOntology onto, String outputFilePath) {
        this.onto = onto;
        this.outputFilePath = outputFilePath;
        new File(outputFilePath).delete();        
    }

    public void generateTEgds() {
        onto.axioms(AxiomType.FUNCTIONAL_DATA_PROPERTY)
                .forEach(axiom -> {
                    //System.out.println(axiom);
                    String property = axiom.getProperty().toString();
                    property = property.substring(property.lastIndexOf("/") + 1, property.length() - 1);
                    property = property.replace("#", "_"); //just for chase bench
                    property = property.toUpperCase();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
                        bw.write(property + "(?X,?Y1), " + property + "(?X,?Y2) -> " + "?Y1 = ?Y2 .");
                        bw.newLine();
                    } catch (IOException ex) {
                        Logger.getLogger(TEgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }                                                
                });
        
        onto.axioms(AxiomType.FUNCTIONAL_OBJECT_PROPERTY) //do the same for functional object properties
                .forEach(axiom -> {
                    //System.out.println(axiom);
                    String property = axiom.getProperty().toString();
                    property = property.substring(property.lastIndexOf("/") + 1, property.length() - 1);
                    property = property.replace("#", "_"); //just for chase bench
                    property = property.toUpperCase();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {
                        bw.write(property + "(?X,?Y1), " + property + "(?X,?Y2) -> " + "?Y1 = ?Y2 .");
                        bw.newLine();
                    } catch (IOException ex) {
                        Logger.getLogger(TEgdsGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }                                                
                });
        
    }          
}
