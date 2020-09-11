package com.ibm.nlq.ourqueryanswering.chase;

import com.ibm.nlq.ourqueryanswering.utils.Table;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StTgdsGenerator {

    private static List<Table> tables = new ArrayList<Table>();
    private static List<String> unchangedRelations = new ArrayList<>();
    private static Map<String, String> mapping = new HashMap<String, String>();
    private final String mappingFilePath, schemaFilePath,outputFilePath;

    public StTgdsGenerator(String mappingFilePath, String schemaFilePath, String outputFilePath) {
        this.mappingFilePath = mappingFilePath;
        this.schemaFilePath = schemaFilePath;
        this.outputFilePath = outputFilePath;        
    }
    
    public void run() {
        loadMapping();
        loadSchema();
        generation();
    }

    public void loadSchema() {
        ArrayList<String> strArray = new ArrayList<>();
        try (BufferedReader csvReader = new BufferedReader(new FileReader(schemaFilePath))) {
            String row;
            while ((row = csvReader.readLine()) != null) {
                row = row.replaceAll("\\s", "");
                if (row.equals("")) {
                    continue;
                }
                if (row.endsWith(",")) {
                    row = row.substring(0, row.length() - 1);
                }
                strArray.add(row);
                if (row.contains("}")) {
                    //System.out.println(strArray);
                    tableInfo(strArray);
                    strArray.clear();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tableInfo(ArrayList<String> strArr) {
        String tableName = strArr.get(0).substring(0, strArr.get(0).length() - 1);
        ArrayList<String> attrNames = new ArrayList<>();
        ArrayList<String> attrTypes = new ArrayList<>();
        for (int i = 1; i < strArr.size() - 1; i++) {
            String[] attStrSplit = strArr.get(i).split(":");
            attrNames.add(attStrSplit[0]);
            attrTypes.add(attStrSplit[1]);
        }
        if (attrNames.size() == 2) {
            if (attrNames.get(0).endsWith("ID") && attrNames.get(1).endsWith("ID")) {
                if (attrTypes.get(0).equals("INTEGER") && attrTypes.get(1).equals("INTEGER")) {
                    System.out.println("Skipping the binary relation "+tableName);
                    unchangedRelations.add(tableName);
                    return;
                }
            }
        }
        Table table = new Table(tableName, attrNames, attrTypes);
        tables.add(table);
        // System.out.println(table.toString());
    }

    public void loadMapping() {
        try (BufferedReader csvReader = new BufferedReader(new FileReader(mappingFilePath))) {
            String row;
            while ((row = csvReader.readLine()) != null) {
                String[] rowSplit = row.split("\t");
                String src = rowSplit[0];
                String dst = rowSplit[1];
                src = src.substring(src.lastIndexOf("/") + 1, src.length());
                dst = dst.substring(dst.lastIndexOf("/") + 1, dst.length());                
                mapping.put(src.toUpperCase(), dst.toUpperCase());                
            }
            System.out.println("Loaded "+mapping.size()+" mappings.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generation() {
        ArrayList<String> strArr = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            Table t = tables.get(i);
            String tableName = t.getName().toUpperCase();            
            tableName = tableName.replaceFirst("SRC", "src");

            // LHS
            String str = tableName + "(";
            for (int j = 0; j < t.getAttrNames().size(); j++) {
                str += "?X" + (j + 1) + ", ";
            }
            str = str.substring(0, str.length() - 2) + ") -> ";

            // RHS
            String tableNamePrime = "";
            tableName = tableName.substring("src_".length(), tableName.length());            
            if (mapping.containsKey(tableName)) {
                tableNamePrime = mapping.get(tableName);                
            }

            if (!tableNamePrime.equals("")) {
                str += tableNamePrime;
            } else {
                str += tableName;
            }
            
            int pkColumn = 1; //by default, assume that the first column is the (non-composite) primary key
            
            if (tableName.equals("ADMISSIONS")) {
                pkColumn = 3;
            }
            
            str += "(?X"+pkColumn+"), ";            

            for (int j = 1; j < t.getAttrNames().size(); j++) {
                str += tableName + "_" + t.getAttrNames().get(j) + "(?X"+pkColumn+", ?X" + (j + 1) + "), ";
            }
            str = str.substring(0, str.length() - 2) + " .";
            strArr.add(str);
        }
        
        //keep binary relations; just remove the "src_" prefix to match the target schema.
        for (String unchangedRelation : unchangedRelations) {            
            strArr.add(unchangedRelation+"(?X1, ?X2) -> "+
                    unchangedRelation.replaceFirst("src_", "")
                    .replaceFirst("(?s)HAS(?!.*?HAS)", "_HAS") //replace last
                    .replaceFirst("(?s)FOR(?!.*?FOR)", "_FOR") //replace last
                    .replace("_FORM", "FORM")
                    +"(?X1,?X2) .");
        }
        
        writeFile(strArr);
        // System.out.println(strArr);
    }

    private void writeFile(ArrayList<String> strArr) {

        try (FileWriter fw = new FileWriter(outputFilePath)) {
            for (String str : strArr) {
                fw.write(str + "\n");
            }

        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("Success...");
    }
}
