package com.alvinquach.jmeter.sampler;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomHttpSampler extends AbstractCustomHttpSampler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomHttpSampler.class);

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
	protected Logger logger() {
		return LOGGER;
	}
	
}
