/**
 * ============================================================================
 *  Copyright (c) 2013,2014 SciamLab s.r.l.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ============================================================================
 */
package com.sciamlab.sandbox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import com.sciamlab.util.HTTPClient;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;



/**
 * Extract the Dataset Title, Notes and Tags from a CKAN Catalog
 * into a txt document for each dataset.
 * The extraction is made using the legacy version fo the CKAN API for
 * a wider compatibility.
 * The resulting list of document is formatted to be processed using the
 * JEX the JRC EuroVoc indeXer
 * @see http://ipsc.jrc.ec.europa.eu/index.php/Traineeships/60/0/
 * 
 * @author alessio
 *
 */

public class CKANDatasetTextExtractor {

	
	// www.dati.gov.it/catalog
	final static String CKAN_SRV_URL = "http://www.opendatahub.it"; 

	final static String DATASET_LIST_API_PATH = "/api/rest/package";
	final static String DATASET_API_PATH = "/api/rest/package/";	
	final static String DESTINATION_FOLDER = "/home/alessio/apps/jex/documents/";

	String[] params = {};
	public CKANDatasetTextExtractor(String[] args){
		params=args;
	}
	
	public static void main(String[] args) {

		Date start = new Date();
		CKANDatasetTextExtractor te = new CKANDatasetTextExtractor(args);
		te.run();
		Date end = new Date();

		System.out.println("\n\n-----------------------------------------------------");
		System.out.println("FINAL SUMMARY");
		System.out.println("Start date:\t"+start);
		System.out.println("End date:\t"+end);
		long total = end.getTime()-start.getTime();
		long hrs = total/1000/60/60;
		long min = (total/1000/60)-(hrs*60);
		long sec = (total/1000)-(hrs*60*60)-(min*60);
		System.out.println("Total processing time: "+(hrs<10?"0":"")+hrs+":"+(min<10?"0":"")+min+":"+(sec<10?"0":"")+sec);
		System.exit(0);
	}
	

	public void run() {

		HTTPClient httpClient = new HTTPClient();
		String datasetJson="[]";

		int count_datasets = 0;

		Date start = new Date();

		try {
			datasetJson = httpClient.doGET(new URL(CKAN_SRV_URL + DATASET_LIST_API_PATH));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONArray datasetJsonArray = (JSONArray) JSONSerializer.toJSON(datasetJson.trim());

		for(Object ds: datasetJsonArray){

			String dsJson = "";

			String dsName = "";
			String dsTitle = "";
			String dsNotes = "";
			String dsTags = "";

			try {
				dsJson = httpClient.doGET(new URL(CKAN_SRV_URL + DATASET_API_PATH + ds));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			count_datasets++;
			JSONObject dsObj = (JSONObject)JSONSerializer.toJSON(dsJson);

			// get the title, notes and tags fields


			String search_payload = ""; 

			dsName = dsObj.getString("name");
			dsTitle = dsObj.getString("title");
			dsNotes = dsObj.getString("notes");
			JSONArray dsTagsArr = dsObj.getJSONArray("tags"); 
			
			for (int o=0;o<dsTagsArr.size();o++){
				dsTags = dsTags + ", "+ dsTagsArr.getString(o);
			}
			//System.out.println(dsTagsArr.toString());

			// create the search payload text gluing notes, title and tags
			search_payload = dsTitle + ". " +dsNotes + ". " + dsTags;
			search_payload = search_payload.replace("\"", "'");

			// store into a file
			PrintWriter pw;
			try {
				pw = new PrintWriter(DESTINATION_FOLDER + dsName + ".txt");
				pw.write(search_payload);
				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.print(".");
			if ( (count_datasets % 100) == 0) {
				System.out.println(""+count_datasets);
			}
		} // end main for

		Date end = new Date();
		System.out.println("\n\n-----------------------------------------------------");
		System.out.println("FINAL SUMMARY");
		System.out.println("Start date:\t"+start);
		System.out.println("End date:\t"+end);
		long total = end.getTime()-start.getTime();
		long hrs = total/1000/60/60;
		long min = (total/1000/60)-(hrs*60);
		long sec = (total/1000)-(hrs*60*60)-(min*60);
		System.out.println("Total processing time: "+(hrs<10?"0":"")+hrs+":"+(min<10?"0":"")+min+":"+(sec<10?"0":"")+sec);
		System.out.println();
		System.out.println(count_datasets+" total datasets available.");

	} // end run method

}