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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import com.sciamlab.util.HTTPClient;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;



/**
 * Parse the list of available dataset on a generic CKAN instance
 * and classify the dataset using the Eurovoc Thesaurus
 * Here is used the alpha version of the RedLink Eurovoc Enhancer API
 * available at: https://api.redlink.io/1.0-ALPHA/analysis/eurovoc/enhance?key=<developer_api_key>
 * The text fields analyzed and passed as payload to the RedLink API are
 * the Dataset Title, Notes and Tags
 * 
 * @author linoder
 *
 */

public class EurovocClassifier {

	final static String REDLINK_KEY = ""; // ask for one at hello@redlink.co
	final static String REDLINK_API_URL = "https://api.redlink.io/1.0-ALPHA/analysis/eurovoc/enhance?key=";

	// www.dati.gov.it/catalog
	final static String CKAN_SRV_URL = "http://www.dati.gov.it/catalog"; 

	final static String DATASET_LIST_API_PATH = "/api/action/package_list";
	final static String DATASET_API_PATH = "/api/3/action/package_show?id=";	

	public static void main(String[] args) {

		HTTPClient httpClient = new HTTPClient();
		String datasetJson="";
		
		int count_datasets = 0;
		int count_datasets_classified = 0;
		
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
		JSONObject obj = (JSONObject)JSONSerializer.toJSON(datasetJson);
		JSONArray datasetJsonArray = obj.getJSONArray("result");

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
			JSONObject dsResponse = (JSONObject)JSONSerializer.toJSON(dsJson);
			JSONObject dsObj = (JSONObject)dsResponse.getJSONObject("result");

			// get the title, notes and tags fields
			//check the ops status
			if (dsResponse.getBoolean("success")){

				String search_payload = ""; 

				dsName = dsObj.getString("name");
				dsTitle = dsObj.getString("title");
				dsNotes = dsObj.getString("notes");
				JSONArray dsTagsArr = dsObj.getJSONArray("tags"); 
				for (int o=0;o<dsTagsArr.size();o++){
					JSONObject dsTagObj = dsTagsArr.getJSONObject(o);
					dsTags = dsTags + dsTagObj.getString("display_name");
				}
				//System.out.println(dsTagsArr.toString());

				// create the search payload text gluing notes, title and tags
				search_payload = dsTitle + "." +dsName + "." + dsTags;
				search_payload = search_payload.replace("\"", "'");

				StringBuffer sbout = new StringBuffer("\""+search_payload+"\",\""+CKAN_SRV_URL+DATASET_API_PATH+dsName+"\"");

				// NOW PERFORM THE EUROVOC CLASSIFICATION

				// call the service
				String eurovocEnhJsonLd = "";
				try {
					HashMap<String,String> headerParams = new HashMap<String,String>();
					headerParams.put("Content-Type", "text/plain"); //Content-Type
					headerParams.put("Accept", "application/ld+json"); //Accept

					eurovocEnhJsonLd = httpClient.doPOST(REDLINK_API_URL + REDLINK_KEY, search_payload , headerParams);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				
				JSONObject eurovocJsonResp = new JSONObject();
				boolean classificationOk = false;
				
				try {
					eurovocJsonResp = (JSONObject)JSONSerializer.toJSON(eurovocEnhJsonLd);

					// sanity check. Is the "@graph" element there ?
					if (eurovocJsonResp.containsKey("@graph")) {

						JSONArray eurovocGraphArray = (JSONArray) eurovocJsonResp.getJSONArray("@graph");
						
						for (int e=0;e<eurovocGraphArray.size();e++) {
							JSONObject enhancementItem = eurovocGraphArray.getJSONObject(e);
							if (enhancementItem.containsKey("entity-reference") || enhancementItem.containsKey("enhancer:entity-reference") ) {

								if (enhancementItem.getString("entity-reference").contains("eurovoc") || 
										enhancementItem.getString("enhancer:entity-reference").contains("eurovoc")) {
										System.err.println(enhancementItem);
									// ok we got some classification done
									// print usefull information
									
									String eurovocURI = null!=enhancementItem.getString("entity-reference") ? enhancementItem.getString("entity-reference") : enhancementItem.getString("enhancer:entity-reference");
									sbout = sbout.append(","+enhancementItem.getString("confidence")+" ,\""+enhancementItem.getString("entity-reference")+"\"");
									classificationOk=true;
								}
							}
						}
					}

				} catch (Exception ee) {
					System.err.println("EUROVOC ENHANCER RESPONSE: " + eurovocJsonResp);
					ee.printStackTrace();
				}

				if (classificationOk) {
					count_datasets_classified++;
				}
				System.out.println(sbout.toString());
				
			} // if dataset is good
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
		System.out.println("Classified: "+count_datasets_classified+" on "+count_datasets+" total datasets available.");
		System.out.println("Classification sucess rate: "+count_datasets_classified/count_datasets*100+"%");
		
		
		
		
	} // end main method

}