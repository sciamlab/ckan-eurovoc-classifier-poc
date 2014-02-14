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

package com.sciamlab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * An HTTPClient helper class to perform REST calls
 */
public class HTTPClient {
	
	private HttpClient client;
	
	public HTTPClient() {
		this.client = new DefaultHttpClient();
	}

	public String doGET(URL url) throws IOException {

		return this.doGET(url, null);
	}
	
	public String doGET(URL url, String user_agent) throws IOException {

		URLConnection yc = url.openConnection();
		if(user_agent!=null && !"".equals(user_agent))
			yc.setRequestProperty("User-Agent", user_agent);
		String output = "";
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String inputLine;
			output = "";
			while ((inputLine = in.readLine()) != null)
				output += inputLine;

		} finally {
			if (in != null) {
				in.close();
			}
		}
		return output;
	}

	public StringBuffer doGETBig(URL url) throws IOException {

		URLConnection yc = url.openConnection();
		BufferedReader in = null;
		StringBuffer output = new StringBuffer();
		try {
			in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String line = in.readLine();
			while (line != null) {
				output.append(line);
				line = in.readLine();
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}

		return output;
	}

	public String doPOST(String url, String body, Map<String, String> headerParams) throws ClientProtocolException,IOException {
		
		HttpPost post = new HttpPost(url);
		StringEntity input = new StringEntity(body);
		post.setEntity(input);
		for (String param : headerParams.keySet()) {
			String value = headerParams.get(param);
			post.setHeader(param, value);
		}

		HttpResponse response = client.execute(post);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String line = "";
		String out = "";
		while ((line = rd.readLine()) != null) {
			out += line;
		}
		return out;
	}
	
	public String doDELETE(String url, Map<String, String> headerParams) throws ClientProtocolException,IOException {
		
		HttpDelete delete = new HttpDelete(url);
		for (String param : headerParams.keySet()) {
			String value = headerParams.get(param);
			delete.setHeader(param, value);
		}

		HttpResponse response = client.execute(delete);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String line = "";
		String out = "";
		while ((line = rd.readLine()) != null) {
			out += line;
		}
		return out;
	}
	
}
