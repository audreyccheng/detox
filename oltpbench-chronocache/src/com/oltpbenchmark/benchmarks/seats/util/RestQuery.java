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

package com.oltpbenchmark.benchmarks.seats.util;

import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.UniformInterfaceException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

import org.apache.log4j.Logger;

public class RestQuery
{
	private static final Logger LOG = Logger.getLogger(RestQuery.class);
	private static String hostname = "0.0.0.0";

	public static void setHostname(String host)
	{
		hostname = host;
	} 
	public static String quoteAndSanitize(String dirty)
	{
		return "'" + dirty.replace("'", "\\'") + "'";
	}

	public static List<Map<String, Object>> restReadQuery(String queryString, int clientId) throws SQLException {
		// Make a new client pointing at Apollo/the rest service
		Client client = new Client();
		client.setReadTimeout(1000 * 30);
		client.setConnectTimeout(1000 * 20);
		String target = "http://" + hostname + ":8080/chronocache/rest/query/" + clientId;
		WebResource resource = client.resource(target);

		// Log where we're going and what we're sending
		LOG.trace(String.format("restReadQuery targeted %s", target));
		LOG.info(String.format("restReadQuery Q = %s", queryString));

		// Make the post query

		long queryStart = System.nanoTime()/1000;
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

	public static int restOtherQuery(String queryString, int clientId) throws SQLException {
		// Make a new client pointing at Apollo/the rest service
		Client client = new Client();
		client.setReadTimeout(1000 * 30);
		client.setConnectTimeout(1000 * 20);

		String target = "http://" + hostname + ":8080/chronocache/rest/query/" + clientId;
		WebResource resource = client.resource(target);

		// Log where we're going and what we're sending
		LOG.trace(String.format("restOtherQuery targeted %s", target));
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
