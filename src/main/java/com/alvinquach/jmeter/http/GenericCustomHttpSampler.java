package com.alvinquach.jmeter.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericCustomHttpSampler extends AbstractCustomHttpSampler<SampleResult> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericCustomHttpSampler.class);

	private static final String REQUEST_URI_KEY = "requestUri";
	
	private static final String REQUEST_BODY_KEY = "requestBody";
	
	// TODO Make use of this. Currently, only application/json is supported.
	private static final String REQUEST_BODY_CONTENT_TYPE_KEY = "contentType";
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultPArguments = new Arguments();
		defaultPArguments.addArgument(REQUEST_URI_KEY, "http://localhost:3000/rest/test/hello");
		defaultPArguments.addArgument(REQUEST_BODY_KEY, "{}");
		return defaultPArguments;
	}

	@Override
	protected HttpEntity createRequestEntityFromContext(JavaSamplerContext context) {
		String json = context.getParameter(REQUEST_BODY_KEY);	
		return new StringEntity(json, ContentType.APPLICATION_JSON);
	}
	
	@Override
	protected URI popuateUrlFromContext(JavaSamplerContext context, SampleResult result) {
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
	protected SampleResult sampleResult() {
		return new SampleResult();
	}

	@Override
	protected Logger logger() {
		return LOGGER;
	}
	
}
