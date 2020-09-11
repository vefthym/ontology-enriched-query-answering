package com.ibm.nlq.ourqueryanswering.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 *
 * @author Vasilis Efthymiou
 */
public class Utilities {
    
    public static OWLOntology replaceOWLEntityInAxiom(OWLOntology ontology, OWLEntity toRemove, OWLEntity toAdd, OWLAxiom axiom) {
        AxiomType type = axiom.getAxiomType();
        ontology.removeAxiom(axiom);
        
        OWLAxiom newAxiom = null;
        AddAxiom addAxiom = new AddAxiom(ontology, newAxiom);
        ontology.getOWLOntologyManager()
                .applyChange(addAxiom);
        return ontology;
    }
    
    public static OWLOntology loadOntologyFromFile(String ontologyFile) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            return manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static OWLOntology loadOntologyFromFile(String ontologyFile, String inputFormat) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            return manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }        
    
    
    public static void parseSingleCsvDataFileIntoMultiple(String inputFilePath, String outputFileFolder) {
        String filename = "AAtest-ignore";
        try (BufferedReader csvReader = new BufferedReader(new FileReader(inputFilePath))) {
            String row;            
            
            PrintWriter out = new PrintWriter(outputFileFolder+"/src_"+filename+".csv"); //never used
            while ((row = csvReader.readLine()) != null) {
                if (row.length() > 1 && Character.isAlphabetic(row.charAt(0))) {
                    System.out.println(row);
                    String[] split = row.split(",");
                    if (split.length == 2 && split[0].endsWith("ID") && split[1].endsWith("ID")) {
                        filename = split[0].substring(0, split[0].lastIndexOf("ID"))+ "HAS" +split[1].substring(0, split[1].lastIndexOf("ID"));
                    } else {
                        filename = row.substring(0, row.indexOf("ID,"));
                    }                    
                    System.out.println(filename);
                    out.close();
                    out = new PrintWriter(outputFileFolder+"/src_"+filename+".csv");                    
                } else if (row.length() > 1) {
                    row = row.replace("(null)", "-1"); //this is only for RDFox, until we know how to represent null values there
                    out.println(row);            
                }
            }
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
