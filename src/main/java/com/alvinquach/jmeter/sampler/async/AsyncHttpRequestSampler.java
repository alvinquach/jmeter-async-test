package com.alvinquach.jmeter.sampler.async;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvinquach.jmeter.sampler.http.AbstractCustomHttpSampler;
import com.alvinquach.jmeter.sampler.util.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncHttpRequestSampler extends AbstractCustomHttpSampler<AsyncRequestResult<String>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpRequestSampler.class);
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final String IDENTIFIER_PATH_KEY = "identifierPath";
	
	private String identifierPath;
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultPArguments = super.getDefaultParameters();
		defaultPArguments.addArgument(IDENTIFIER_PATH_KEY, "");
		return defaultPArguments;
	}
	
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		
		identifierPath = context.getParameter(IDENTIFIER_PATH_KEY);
		if (StringUtils.isEmpty(identifierPath)) {
			throw new IllegalArgumentException("Setup error: identifier path must not be blank");
		}
	}
	
	@Override
	public AsyncRequestResult<String> runTest(JavaSamplerContext context) {
		AsyncRequestResult<String> result = super.runTest(context);
		result.setOriginalRequestStartTime(result.getStartTime());
		return result;
	}

	@Override
	protected HttpEntity createRequestEntityFromContext(JavaSamplerContext context) {
		String json = context.getParameter(REQUEST_BODY_KEY);	
		return new StringEntity(json, ContentType.APPLICATION_JSON);
	}
	
	@Override
	protected URI popuateUrlFromContext(JavaSamplerContext context, AsyncRequestResult<String> result) {
		String requestUri = context.getParameter(REQUEST_URI_KEY);
		try {
			URI uri = new URI(requestUri);
			result.setURL(uri.toURL());
			return uri;
		} catch (URISyntaxException | MalformedURLException e) {
			LOGGER.error("Could not parse URI '{}'", requestUri);
		}
		return null;
	}

	@Override
	protected void populateResultFromResponseBody(JavaSamplerContext context, AsyncRequestResult<String> result, String responseBody) {
		super.populateResultFromResponseBody(context, result, responseBody);
		/*
		 * Get a unique identifier for the result. This will be used by the response
		 * sampler to track the corresponding asynchronous response for this request.
		 */
		String identifier = parseIdentifierFromResponseBody(context, responseBody);
		if (identifier == null) {
			result.setSuccessful(false);
			return;
		}
		LOGGER.info("Receieved initial response with identifier '{}'", identifier);
		result.setIdentifier(identifier);
	}
	
	protected String parseIdentifierFromResponseBody(JavaSamplerContext context, String responseBody) {
		JsonNode jsonNode = deserializeResponseBody(responseBody);
	    if (jsonNode == null) {
	    	return null;
	    }
		String identifier = JsonNodeUtils.getNumberOrTextAsString(jsonNode, identifierPath);
		if (identifier == null) {
	    	LOGGER.error("Response body does not contain a valid identifier value at the specified path '{}'", identifierPath);
		}
		return identifier;
	}
	
	private JsonNode deserializeResponseBody(String body) {
		try {
			return MAPPER.readTree(body);
		} catch (JsonProcessingException e) {
			LOGGER.error("Exception encountered while deserializing the response body: {}", e.getClass().getSimpleName());
			return null;
		}
	}
	
	@Override
	protected AsyncRequestResult<String> sampleResult() {
		return new AsyncRequestResult<>();
	}

	@Override
	protected Logger logger() {
		return LOGGER;
	}

}
