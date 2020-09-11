package com.ibm.nlq.ourqueryanswering.merging;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

/**
 *
 * @author Vasilis Efthymiou
 */
public class OntologyMerger {

    private OWLOntology onto1, onto2;
    private Map<String, String> mappings;
    private OWLOntology mergedOnto;

    /**
     * Initializes a merging instance with two ontologies to be merged and mappings between them. 
     * @param onto1 the first ontology (some of whose entities are mapped to entities in onto2) - e.g., MDX
     * @param onto2 the second ontology - e.g., SNOMED module normalized or not
     * @param mappings key: iri of onto1 entity, value: iri of equivalent entity in onto2 (onto1 entity will be renamed to use the value iri)
     */
    public OntologyMerger(OWLOntology onto1, OWLOntology onto2, Map<String, String> mappings) {
        this.onto1 = onto1;
        this.onto2 = onto2;
        this.mappings = mappings;
        mergedOnto = null;
    }
    
    public OntologyMerger(OWLOntology onto1, OWLOntology onto2) {
        this(onto1, onto2, new HashMap<>());
    }

    /**
     * Merges the two ontologies and renames the mapped names of ontology1 to use 
     * the mapped names from ontology2. 
     * @return 
     */
    public OWLOntology merge() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();        

        try {
            mergedOnto = manager.createOntology(IRI.create("http://www.ibm.com/mergedOntology"));
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(OntologyMerger.class.getName()).log(Level.SEVERE, null, ex);
        }
        mergedOnto.addAxioms(onto1.axioms().collect(Collectors.toList()));                        
        mergedOnto.addAxioms(onto2.axioms().collect(Collectors.toList()));
        
        //merge the mapped entities from onto1 using their mapped names in onto2
        OWLEntityRenamer renamer = new OWLEntityRenamer(manager, Collections.singleton(mergedOnto));
        Map<OWLEntity, IRI> entity2IRIMap = new HashMap<>();
               
        mappings.entrySet().forEach(entry -> { 
            OWLEntity toBeReplaced = mergedOnto.entitiesInSignature(IRI.create(entry.getKey()), Imports.EXCLUDED).findFirst().get();            
            entity2IRIMap.put(toBeReplaced, IRI.create(entry.getValue()));
        });
        
        mergedOnto.applyChanges(renamer.changeIRI(entity2IRIMap));
        
        return mergedOnto;
    }

    public OWLOntology getOnto1() {
        return onto1;
    }

    public void setOnto1(OWLOntology onto1) {
        this.onto1 = onto1;
    }

    public OWLOntology getOnto2() {
        return onto2;
    }

    public void setOnto2(OWLOntology onto2) {
        this.onto2 = onto2;
    }

    public OWLOntology getMergedOnto() {
        return (mergedOnto != null) ? mergedOnto : merge();
    }
    
    public void loadMappings(String mappingsFile) {
        mappings = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(mappingsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                mappings.put(parts[0], parts[1]); //+"m" is used only for snomed, because of issue in rewriting approach (only numeric ids are not supported by GRIND)
                //mappings.put(parts[0], parts[1].replace("snomed.info", "ourOntologyModule")+"m");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OntologyMerger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OntologyMerger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }

}
