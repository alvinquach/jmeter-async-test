package com.alvinquach.jmeter.sampler.async;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvinquach.jmeter.sampler.util.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Generic sampler plug-in for sending HTTP requests to an asynchronous API
 * endpoint.
 * <p>
 * This differs from a standard HTTP Request sampler in that it will parse an
 * identifier value from the initial response and passes it in the returned
 * SampleResult object so that the AsyncHttpResponseSampler can use it to
 * track the asynchronous response.This requires a unique identifier to be
 * present in both the initial and asynchronous responses.
 * <p>
 * The test plan should be set up such that a AsyncHttpResponseSampler is placed
 * directly after this sampler in the same thread group.
 *
 * @author Alvin Quach
 */
public class AsyncHttpRequestSampler extends AbstractAsyncHttpRequestSampler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpRequestSampler.class);
	
	private static final String IDENTIFIER_KEY = "identifier";
	
	private static final String IDENTIFIER_PATH_KEY = "identifierPath";
	
	private String identifierPath;
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultArguments = super.getDefaultParameters();
		defaultArguments.addArgument(IDENTIFIER_PATH_KEY, "");
		return defaultArguments;
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
	protected void populateHeaders(HttpPost request) {
		return;
	}
	
	@Override
	protected HttpEntity createRequestEntityFromContext(JavaSamplerContext context) {
		String json = context.getParameter(REQUEST_BODY_KEY);	
		return new StringEntity(json, ContentType.APPLICATION_JSON);
	}
	
	@Override
	protected void populateResultFromResponseBody(JavaSamplerContext context, SampleResult result, String responseBody) {
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
		context.getJMeterVariables().put(IDENTIFIER_KEY, identifier);
		LOGGER.info("Receieved initial response with identifier '{}'", identifier);
	}
	
	protected String parseIdentifierFromResponseBody(JavaSamplerContext context, String responseBody) {
		JsonNode jsonNode;
		try {
			jsonNode = JsonNodeUtils.deserializeString(responseBody);
		} catch (JsonProcessingException e) {
			LOGGER.error("Exception encountered while deserializing the response body: {}", e.getClass().getSimpleName());
			return null;
		}
		String identifier = JsonNodeUtils.getNumberOrTextAsString(jsonNode, identifierPath);
		if (identifier == null) {
	    	LOGGER.error("Response body does not contain a valid identifier value at the specified path '{}'", identifierPath);
		}
		return identifier;
	}

	@Override
	protected Logger logger() {
		return LOGGER;
	}

}
