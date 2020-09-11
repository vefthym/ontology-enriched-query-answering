package com.ibm.nlq.ourqueryanswering.module;

import com.ibm.nlq.ourqueryanswering.utils.Utilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 *
 * @author Vasilis Efthymiou
 */
public class SyntacticLocalityBasedSModuleExtractor {

    private String signatureFile, mappingsFile, moduleOutputFile;
    OWLOntology onto;

    public SyntacticLocalityBasedSModuleExtractor(String mappingsFile, String signatureFile, OWLOntology onto, String moduleOutputFile) {
        this.mappingsFile = mappingsFile;
        this.signatureFile = signatureFile;
        this.onto = onto;
        this.moduleOutputFile = moduleOutputFile;
    }
    
    public SyntacticLocalityBasedSModuleExtractor(String mappingsFile, String signatureFile, String ontoFile, String moduleOutputFile) {
        this(mappingsFile, signatureFile, Utilities.loadOntologyFromFile(ontoFile), moduleOutputFile);
    }

    /**
     * Reads the mappings file and writes the second column (iris from target
     * ontology) to the signature file.
     */
    private void generateSignatureFromMappings() {
        try (BufferedReader mappingsReader = new BufferedReader(new FileReader(mappingsFile));
                PrintWriter out = new PrintWriter(signatureFile)) {
            String line;
            while ((line = mappingsReader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    out.println(parts[1]);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SyntacticLocalityBasedSModuleExtractor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SyntacticLocalityBasedSModuleExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public OWLOntology extractModule() {
        generateSignatureFromMappings();
        SyntacticLocalityModuleExtractor me = new SyntacticLocalityModuleExtractor(onto.getOWLOntologyManager(), onto.axioms(), ModuleType.STAR, false);       
        OWLOntology module;
        try {            
            module =  me.extractAsOntology(getSignatureFromFile(signatureFile), IRI.create("http://ibm.com/nlq/ontologies/module.owl"));                        
            module.saveOntology(IRI.create(new File(moduleOutputFile).toURI()));       
            return module;
        } catch (OWLOntologyCreationException | OWLOntologyStorageException ex) {    
            Logger.getLogger(SyntacticLocalityBasedSModuleExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Adapted from Ernesto's code. Reads the signature file and the ontology,
     * and returns the signature as a set of owl entities.
     *
     * @param fileSignature
     */
    private Set<OWLEntity> getSignatureFromFile(String fileSignature) {
        Set<OWLEntity> matchedSignature = new HashSet<>();        
        Map<String, OWLEntity> name2entity = new HashMap<>();

        onto.signature().forEach(entity -> name2entity.put(getEntityLabel(entity.getIRI().toString()), entity));

        Set<String> signatureNames = new HashSet<>();
        try (BufferedReader sigReader = new BufferedReader(new FileReader(signatureFile))) {
            String line;
            while ((line = sigReader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    signatureNames.add(line);
                }
            }

            //We should match the signature, that is, signature must have the same URI than the entity form the external onto
            Set<String> keys = name2entity.keySet();

            signatureNames.forEach(entSig -> {
                if (keys.contains(entSig)) {
                    matchedSignature.add(name2entity.get(entSig));
                } else {
                    System.err.println("\tThe entity '" + entSig + "' has not a correspondence in the external ontology.");
                }
            });
            return matchedSignature;

        } catch (Exception e) {
            System.err.println("Error reading file: " + fileSignature + "\n" + e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Adapted from Ernesto's code.
     *
     * @param uriStr
     * @return
     */
    private String getEntityLabel(String uriStr) {
        return uriStr.contains("#") ? uriStr.split("#")[1] : uriStr;
    }

}
