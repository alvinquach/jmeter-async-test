package com.alvinquach.jmeter.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;

public abstract class AbstractCustomHttpSampler<T extends SampleResult> extends AbstractJavaSamplerClient {

	@Override
	public final T runTest(JavaSamplerContext context) {
		T result = sampleResult();
		result.sampleStart();
		
		runTest(context, result);
		
		result.sampleEnd();
		return result;
	}
	
	protected void runTest(JavaSamplerContext context, T result) {
		URI uri = popuateUrlFromContext(context, result);
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost request = new HttpPost(uri);
			request.setEntity(createRequestEntityFromContext(context));
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				populateResultFromResponse(context, result, response);
			}
		} catch (IOException e) {
			logger().error("Exection encountered while sending request: {}", e.getClass().getSimpleName());
			result.setSuccessful(false);
		}
	}
	
	protected abstract HttpEntity createRequestEntityFromContext(JavaSamplerContext context);
	
	/**
	 * Sets the request URL to be used for this sample into the result and returns
	 * it as a URI.
	 */
	protected abstract URI popuateUrlFromContext(JavaSamplerContext context, T result);
	
	protected void populateResultFromResponse(JavaSamplerContext context, T result, HttpResponse response) {
		/*
		 * Response status
		 */
		populateResultFromStatusLine(context, result, response.getStatusLine());

		/*
		 * Response body
		 */
		populateResultFromResponseEntity(context, result, response.getEntity());
	}
	
	protected void populateResultFromStatusLine(JavaSamplerContext context, T result, StatusLine statusLine) {
		int statusCode = statusLine.getStatusCode();
		// TODO Allow user to specify expected status code, and set successful flag based on whether the code matches.
		if (statusCode == 200) {
			result.setResponseCodeOK();
			result.setSuccessful(true);
		} else {
			result.setResponseCode(String.valueOf(statusCode));
			result.setSuccessful(false);
		}
		result.setResponseMessage(statusLine.getReasonPhrase());
	}
	
	protected void populateResultFromResponseEntity(JavaSamplerContext context, T result, HttpEntity responseEntity) {
		if (responseEntity == null) {
			return;
		}
		result.setBodySize(responseEntity.getContentLength());
		Header contentType = responseEntity.getContentType();
		if (contentType != null) {
			result.setContentType(contentType.getValue());
		}
		try {
			InputStream content = responseEntity.getContent();
			String responseBody = IOUtils.toString(content, "UTF-8");
			populateResultFromResponseBody(context, result, responseBody);
		} catch (Exception e) {
			logger().warn("Could not parse body from response.");
		}
	}
	
	protected void populateResultFromResponseBody(JavaSamplerContext context, T result, String responseBody) {
		result.setResponseData(responseBody, "UTF-8");
	}

	/**
	 * Creates a container for the sample result.
	 */
	protected abstract T sampleResult();
	
	/**
	 * Get the logger for this class.
	 */
	protected abstract Logger logger();
	
}
