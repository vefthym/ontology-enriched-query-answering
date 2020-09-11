package com.ibm.nlq.ourqueryanswering.chase;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author Vasilis Efthymiou
 */
public class ChaseExecution {

	private final String sourceSchemaPath, targetSchemaPath, stTgdsFilePath, tTgdsFilePath, tEgdsFilePath;
	private final String dataFolder, outputFolder, queryFolder;

	public ChaseExecution(String sourceSchemaPath, String targetSchemaPath, String stTgdsFilePath, String tTgdsFilePath,
			String tEgdsFilePath, String dataFolder, String queryFolder, String outputFolder) {
		this.sourceSchemaPath = sourceSchemaPath;
		this.targetSchemaPath = targetSchemaPath;
		this.stTgdsFilePath = stTgdsFilePath;
		this.tTgdsFilePath = tTgdsFilePath;
		this.tEgdsFilePath = tEgdsFilePath;
		this.dataFolder = dataFolder;
		this.outputFolder = outputFolder;
		this.queryFolder = queryFolder; // can be null
	}

	protected void loadCommonFile(Object databaseSchema, File file, Set<Object> rules, Set<Object> facts)
			throws Exception {
		// TODO: due to licensing issues, we cannot post the code for this part.
	}

	/**
	 * Runs a chase implementation. Pick your favorite from
	 * https://github.com/dbunibas/chasebench, or write your own. Code removed for
	 * licensing issues.
	 */
	public void runChase() {
		// TODO: due to licensing issues, we cannot post the code for this part.
		// You can pick your preferred chase implementation from the
		// chase benchmark github repo and run it here:
		// https://github.com/dbunibas/chasebench
	}

	/**
	 * Runs a chase implementation that also answers queries. Pick your favorite
	 * from https://github.com/dbunibas/chasebench, or write your own. Code removed
	 * for licensing issues.
	 */
	public void runChaseWithQA() {
		// TODO: due to licensing issues, we cannot post the code for this part.
		// You can pick your preferred chase implementation from the
		// chase benchmark github repo and run it here:
		// https://github.com/dbunibas/chasebench
	}

	public void run() {

		// TODO: due to licensing issues, we cannot post the code for this part.
	}

	public static void main(String[] args) throws IOException, Exception {

		boolean useLogMap = false;
		boolean useMimic = true;

		String BASE_PATH = "src/main/resources"; // the base path where the input (output) files are (will be created)
		if (useMimic) {
			BASE_PATH = BASE_PATH + "_mimic";
		}

		String sourceSchemaPath = BASE_PATH + "/schema/sourceSchema.txt";
		String targetSchemaPath = BASE_PATH + "/schema/targetSchema.txt";
		String stTgdsFilePath = BASE_PATH + "/dependencies/st-tgds.txt";
		String tTgdsFilePath = BASE_PATH + "/dependencies/t-tgds.txt";
		String tEgdsFilePath = BASE_PATH + "/dependencies/t-egds.txt";
		String dataFolder = BASE_PATH + "/data";
		String queryFile = BASE_PATH + "/queries";
		String outputFolder = BASE_PATH + "/output";

		if (useLogMap) {
			targetSchemaPath = BASE_PATH + "/schema/targetSchemaLogMap.txt";
			stTgdsFilePath = BASE_PATH + "/dependencies/st-tgdsLogMap.txt";
			tTgdsFilePath = BASE_PATH + "/dependencies/t-tgdsLogMap.txt";
			tEgdsFilePath = BASE_PATH + "/dependencies/t-egdsLogMap.txt";
			queryFile = BASE_PATH + "/queriesLogMap"; // some queries need to change as well to capture less renamings
														// due to missing matches
			outputFolder = BASE_PATH + "/outputUsingLogMap";
		}

		ChaseExecution chase = new ChaseExecution(sourceSchemaPath, targetSchemaPath, stTgdsFilePath, tTgdsFilePath,
				tEgdsFilePath, dataFolder, queryFile, outputFolder);
		chase.runChase();
		// chase.runChaseWithQA();

	}

}
