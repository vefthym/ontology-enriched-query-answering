# ontology-enriched-query-answering

This is a framework for query answering on relational databases by adopting methods and techniques from the Semantic Web community and the data exchange community. 
It first deploys module-extraction techniques to derive a concise and relevant sub-ontology from an external reference ontology. Then, it uses the chase procedure from the data exchange community to materialize a universal solution that can be subsequently used to answer queries on a database. 

In short, it can enrich the query answers over a relational database by exploiting an external referenence ontology (e.g., SNOMED CT). After having an ontology generated from the input database, you can either manually or (semi-)automatically (e.g., by using https://github.com/ernestojimenezruiz/logmap-matcher) generate matches between this generated ontology and the external ontology. Then, our framework will extract a syntactic-locality-based module from the external ontology, merge the generated ontology with the extracted module into a single ontology (used as the target schema) and generate the necessary st-tgds, t-tgds, and t-egds required to run the chase. Depending on which chase implementation you choose, our framework can then return the (certain) answers to queries posed over the target schema, or just returned the materialized data after the chase termination. 
The details of this framework, including necessary chase termination conditions, will be uploaded soon. 

The main file to run is ChasePipeline.java. 
Due to licensing issues, however, in the file ChaseExecution.java, we have commented out the calls to the chase becnhmark library (https://github.com/dbunibas/chasebench) that was introduced in the paper: 
Michael Benedikt, George Konstantinidis, Giansalvatore Mecca, Boris Motik, Paolo Papotti, Donatello Santoro, Efthymia Tsamoura:
Benchmarking the Chase. PODS 2017: 37-52

We have contacted the authors and they are in the process of attaching a license to their library. Once this is done, we will reveal the missing piece in our code as well. 

# Reference

Shqiponja Ahmetaj, Vasilis Efthymiou, Ronald Fagin, Phokion Kolaitis, Chuan Lei, Fatma Özcan and Lucian Popa. Ontology-Enriched Query Answering on Relational Databases. IAAI 2021
