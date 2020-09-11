package com.ibm.nlq.ourqueryanswering.chasePipeline;

import com.ibm.nlq.ourqueryanswering.chase.ChaseExecution;
import com.ibm.nlq.ourqueryanswering.chase.SchemaDefinition;
import com.ibm.nlq.ourqueryanswering.chase.StTgdsGenerator;
import com.ibm.nlq.ourqueryanswering.chase.TEgdsGenerator;
import com.ibm.nlq.ourqueryanswering.chase.TTgdsGenerator;
import com.ibm.nlq.ourqueryanswering.merging.OntologyMerger;
import com.ibm.nlq.ourqueryanswering.module.SyntacticLocalityBasedSModuleExtractor;
import com.ibm.nlq.ourqueryanswering.utils.Utilities;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Implementation of the chase approach. 
 *
 * @author Vasilis Efthymiou
 */
public class ChasePipeline {

    public static String BASE_PATH;
    public static String ontologyFile1, ontologyFile2, ontologiesFolder, 
            mappingsFile, sourceSchemaPath, dataFolder, queryFolder, 
            signatureFile, ontology2ModulePath, targetSchemaPath, 
            dependenciesFolder, stTgdsFilePath, tTgdsFilePath, tEgdsFilePath, 
            outputFolder;

    private static void setInputParameters(boolean useLogMap, boolean useMimic3) {
        BASE_PATH = "src/main/resources"; //the base path where the input (output) files are (will be created)
        if (useMimic3) {
            BASE_PATH += "_mimic";
        }                

        //input files (required)
        ontologiesFolder = BASE_PATH + "/ontologies";
        ontologyFile1 = ontologiesFolder + "/MDX_DrugInteraction-nolabels.owl";
        ontologyFile2 = ontologiesFolder + "/SNOMED-2019-05-06_16-16-24-functionalSyntax.owl";
        mappingsFile = BASE_PATH + "/mappings.txt";

        sourceSchemaPath = BASE_PATH + "/schema/sourceSchema.txt";
        dataFolder = BASE_PATH + "/data";
        queryFolder = BASE_PATH + "/queries";

        //output files (will be generated) - folders must exist
        signatureFile = BASE_PATH + "/ontologies/SignatureMDXFull.txt"; //for module extraction        
        ontology2ModulePath = BASE_PATH + "/ontologies/LUM_SNOMED-2019-05-06_16-16-24-functionalSyntax_module.owl"; //TODO: replace with original onto and then extract module

        targetSchemaPath = BASE_PATH + "/schema/targetSchema.txt";

        dependenciesFolder = BASE_PATH + "/dependencies";
        stTgdsFilePath = dependenciesFolder + "/st-tgds.txt";
        tTgdsFilePath = dependenciesFolder + "/t-tgds.txt";
        tEgdsFilePath = dependenciesFolder + "/t-egds.txt";

        outputFolder = BASE_PATH + "/output";
        
        if (useMimic3) {            
            ontologyFile1 = ontologiesFolder + "/MIMIC3.owl"; 
            signatureFile = BASE_PATH + "/signatureMIMICIII.txt"; //for module extraction        
            ontology2ModulePath = ontologiesFolder + "/LUM_SNOMED-2019-05-06_16-16-24-functionalSyntax_module_MIMICIII.owl"; //TODO: replace with original onto and then extract module
        }
                

        if (useLogMap) {
            mappingsFile = BASE_PATH + "/mappingsLogMap.txt";
            signatureFile = BASE_PATH + "/signatureLogMap.txt"; //for module extraction  
            ontology2ModulePath = ontologiesFolder + "/LUM_SNOMED-2019-05-06_16-16-24-functionalSyntax_module_LogMap.owl"; //TODO: replace with original onto and then extract module
            targetSchemaPath = BASE_PATH + "/schema/targetSchemaLogMap.txt";
            stTgdsFilePath = dependenciesFolder + "/st-tgdsLogMap.txt";
            tTgdsFilePath = dependenciesFolder + "/t-tgdsLogMap.txt";
            tEgdsFilePath = dependenciesFolder + "/t-egdsLogMap.txt";
            queryFolder = BASE_PATH + "/queriesLogMap";
            outputFolder = BASE_PATH + "/outputUsingLogMap";
        }
        
    }

    public static void run() {

        boolean useLogMap = false; //true for LogMap, false, for all matches
        boolean useMimic3 = false; //true for Mimic-III, false for MDX as source data
        setInputParameters(useLogMap, useMimic3); 

        //extract S-module in SNOMED
        //commented out to save some time, using the already extracted module from the same (faster) local library: https://github.com/ernestojimenezruiz/locality-module-extractor
        //SyntacticLocalityBasedSModuleExtractor moduleExtractor = new SyntacticLocalityBasedSModuleExtractor(mappingsFile, signatureFile, ontologyFile2, ontology2ModulePath);
        //moduleExtractor.extractModule();                
        //merge ontologies
        System.out.println("Merging ontologies...");
        OWLOntology onto1 = Utilities.loadOntologyFromFile(ontologyFile1);
        System.out.println("onto1 axioms: " + onto1.axioms().count());
        OWLOntology onto2 = Utilities.loadOntologyFromFile(ontology2ModulePath);
        System.out.println("onto2 axioms: " + onto2.axioms().count());

        OntologyMerger merger = new OntologyMerger(onto1, onto2);
        merger.loadMappings(mappingsFile);
        System.out.println("mappings: " + merger.getMappings().size());

        OWLOntology mergedOntology = merger.merge();

        // uncomment to save Ontology to file (not required)
        
        try {
            String mergedOntologyPath = ontologiesFolder + "/mergedOntology.owl";
            mergedOntology.saveOntology(IRI.create(new File(mergedOntologyPath).toURI()));
        } catch (OWLOntologyStorageException ex) {
            Logger.getLogger(ChasePipeline.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         
        //generate target schema        
        SchemaDefinition sd = new SchemaDefinition(mergedOntology, targetSchemaPath);
        sd.generateTargetSchema();

        //generate tgds
        //first delete existing dependency files          
        for (File file : new File(dependenciesFolder).listFiles()) {
            file.delete();
        }

        //st-tgds                                
        StTgdsGenerator stTgdsG = new StTgdsGenerator(mappingsFile, sourceSchemaPath, stTgdsFilePath);
        stTgdsG.run();

        //t-tgds        
        TTgdsGenerator tTgds = new TTgdsGenerator(mergedOntology, tTgdsFilePath);
        tTgds.generateTgds();

        //t-egds        
        TEgdsGenerator tEgds = new TEgdsGenerator(mergedOntology, tEgdsFilePath);
        tEgds.generateTEgds();

        //run the chase
        ChaseExecution chase = new ChaseExecution(sourceSchemaPath, targetSchemaPath, stTgdsFilePath, tTgdsFilePath, tEgdsFilePath, dataFolder, queryFolder, outputFolder);
        //chase.runChase();
        chase.runChaseWithQA();
    }

    public static void main(String[] args) {
        run();
    }

}
