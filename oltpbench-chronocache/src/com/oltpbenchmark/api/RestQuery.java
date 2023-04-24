/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/

package com.oltpbenchmark.api;

import java.sql.SQLException;
import java.util.Map;
import java.util.List;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.UniformInterfaceException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class RestQuery
{
	private static final Logger LOG = Logger.getLogger(RestQuery.class);
	private static final String hostname = "localhost";

	private static Client client;

	public static Client makeClient() {
		if (client != null) {
			return client;
		}
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.getParams().setConnectionTimeout(5000);
		connectionManager.getParams().setSoTimeout(5000);
		connectionManager.getParams().setMaxTotalConnections(5000);
		connectionManager.getParams().setDefaultMaxConnectionsPerHost(5000);
		HttpClient httpClient = new HttpClient(connectionManager);
		ApacheHttpClientHandler clientHandler = new ApacheHttpClientHandler(httpClient);
		ClientHandler root = new ApacheHttpClient(clientHandler);
		ClientConfig config = new DefaultApacheHttpClientConfig();
		client = new Client(root, config);
		return client;
	}

	public static void restResetCacheStats() {
		String target = "http://" + hostname + ":8080/chronocache/rest/query/";
		WebResource wr = makeClient().resource(target);
		wr.post();
	}

	public static String restGetCacheStats() {
		String target = "http://" + hostname + ":8080/chronocache/rest/query/";
		WebResource wr = makeClient().resource(target);
		return wr.accept(MediaType.TEXT_PLAIN).type(MediaType.TEXT_PLAIN).get(String.class);
	}

	public static List<Map<String, Object>> restReadQuery(WebResource resource, String queryString, int clientId) throws SQLException {
		// Log where we're going and what we're sending
		LOG.info(String.format("restReadQuery Q = %s", queryString));

		// Make the post query
		long queryStart = System.nanoTime()/1000;
		String response;
		try {
			try {
				response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(String.class, String.format("{ \"query\":\"%s\" }", queryString));
			} catch (com.sun.jersey.api.client.ClientHandlerException e) {
				// TODO(jchan): this is a hack to catch timeouts
				throw new SQLException("HTTP error");
			}
		} catch (UniformInterfaceException e) {
			throw new SQLException("HTTP error");
		}

		long queryEnd = System.nanoTime()/1000;
		LOG.info(String.format( "Query Response time: %d", queryEnd - queryStart) );

		LOG.trace(String.format("restReadQuery result = %s", response));

		// Deparse the result
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, Object>> data = null;
		try
		{
			data = mapper.readValue(response, new TypeReference<List<Map<String, Object>>>(){});
		}
		catch (Exception e)
		{
			LOG.error(String.format( "IOException caught, message: {}", e.getMessage()));
		}

		assert data != null;

		return data;
	}

	public static int restOtherQuery(WebResource resource, String queryString, int clientId) throws SQLException {
		LOG.info(String.format("restOtherQuery Q = %s", queryString));

		long queryStart = System.nanoTime()/1000;
		// Make the post query
		String response;
		try {
			try {
				response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(String.class, String.format("{ \"query\":\"%s\" }", queryString));
			} catch (com.sun.jersey.api.client.ClientHandlerException e) {
				throw new SQLException("HTTP error");
			}
		} catch (UniformInterfaceException e) {
			throw new SQLException("HTTP error");
		}

		LOG.trace(String.format("restOtherQuery result = %s", response));
		long queryEnd = System.nanoTime()/1000;
		LOG.info(String.format( "Query Response time: %d", queryEnd - queryStart) );

		// Deparse the result
		ObjectMapper mapper = new ObjectMapper();
		int data = -1;
		try
		{
			data = mapper.readValue(response, Integer.class);
		}
		catch (Exception e)
		{
			LOG.error(String.format( "IOException caught, message: {}", e.getMessage()));
		}

		assert data != -1;

		return data;
	}
}
