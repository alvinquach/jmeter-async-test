package com.alvinquach.jmeter.sampler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;

public abstract class AbstractCustomHttpSampler extends AbstractJavaSamplerClient {

	protected static final String REQUEST_URI_KEY = "requestUri";
	
	protected static final String REQUEST_BODY_KEY = "requestBody";
	
	protected URI requestUri;
	
	protected URL requestUrl;

	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultArguments = new Arguments();
		defaultArguments.addArgument(REQUEST_URI_KEY, "http://localhost:3000/rest/test/hello");
		defaultArguments.addArgument(REQUEST_BODY_KEY, "{}");
		return defaultArguments;
	}
	
	@Override
	public void setupTest(JavaSamplerContext context) {
		try {
			requestUri = parseUrlFromContext(context);
			requestUrl = requestUri.toURL();
		} catch (URISyntaxException | MalformedURLException e) {
			logger().error("Could not parse URI '{}'", requestUri);
		}
	}
	
	protected URI parseUrlFromContext(JavaSamplerContext context) throws URISyntaxException {
		String requestUri = context.getParameter(REQUEST_URI_KEY);
		return new URI(requestUri);
	}
	
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		SampleResult result = new SampleResult();
		result.setURL(requestUrl);
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost request = new HttpPost(requestUri);
			request.setEntity(createRequestEntityFromContext(context));
			populateHeaders(request);
			result.sampleStart();
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				result.sampleEnd();
				populateResultFromResponse(context, result, response);
			}
		} catch (IOException e) {
			logger().error("Exception encountered while sending request: {}", e.getClass().getSimpleName());
			result.setSuccessful(false);
		}
		populateResultFromContext(context, result);
		return result;
	}
	
	protected abstract void populateHeaders(HttpPost request);
	
	protected abstract HttpEntity createRequestEntityFromContext(JavaSamplerContext context);
	
	protected void populateResultFromResponse(JavaSamplerContext context, SampleResult result, HttpResponse response) {
		/*
		 * Response status
		 */
		populateResultFromStatusLine(context, result, response.getStatusLine());

		/*
		 * Response body
		 */
		populateResultFromResponseEntity(context, result, response.getEntity());
	}
	
	protected void populateResultFromStatusLine(JavaSamplerContext context, SampleResult result, StatusLine statusLine) {
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
	
	protected void populateResultFromResponseEntity(JavaSamplerContext context, SampleResult result, HttpEntity responseEntity) {
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
			logger().warn("Could not parse body from response");
		}
	}
	
	protected void populateResultFromResponseBody(JavaSamplerContext context, SampleResult result, String responseBody) {
		result.setResponseData(responseBody, "UTF-8");
	}
	
	protected void populateResultFromContext(JavaSamplerContext context, SampleResult result) {
		// Do nothing by default, subclasses can override this.
	}
	
	/**
	 * Get the logger for this class.
	 */
	protected abstract Logger logger();
	
}
