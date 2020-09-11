package com.ibm.nlq.ourqueryanswering.utils;

import java.util.ArrayList;

public class Table {
	private String name;
	private ArrayList<String> attrNames, attrTypes;

	public Table(String name, ArrayList<String> attrNames, ArrayList<String> attrTypes) {
		this.name = name;
		this.attrNames = attrNames;
		this.attrTypes = attrTypes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<String> getAttrNames() {
		return attrNames;
	}

	public void setAttrNames(ArrayList<String> attrNames) {
		this.attrNames = attrNames;
	}
	
	public ArrayList<String> getAttrTypes() {
		return attrTypes;
	}

	public void setAttrTypes(ArrayList<String> attrTypes) {
		this.attrTypes = attrTypes;
	}
	
        @Override
	public String toString() {
		String rst = name + ": ";
		for (int i = 0; i < attrNames.size(); i++)
			rst += attrNames.get(i) + "-" + attrTypes.get(i) + "; ";
		return rst;
	}
}
