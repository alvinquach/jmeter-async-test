package com.alvinquach.jmeter.sampler.async;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;

/**
 * Base sampler plug-in for listening to asynchronous HTTP responses from an API
 * endpoint that was hit by the AsyncHttpRequestSampler. The response is tracked
 * by an identifier value provided in the previous sample result by the
 * AbstractAsyncHttpRequestSampler. This requires the unique identifier to be
 * present in the asynchronous response.
 *
 * @author Alvin Quach
 */
public abstract class AbstractAsyncHttpResponseSampler extends AbstractJavaSamplerClient {

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		/*
		 * Instantiate the sample result and record its start time.
		 */
		SampleResult result = new SampleResult();
		result.sampleStart();
		
		runTest(context, result);
		
		if (result.getEndTime() == 0) {
			result.sampleEnd();
		}
		return result;
	}
	
	private void runTest(JavaSamplerContext context, SampleResult result) {
		/*
		 * Retrieve the result of the previous sampler stage from the context.
		 */
		SampleResult previousResult = context.getJMeterContext().getPreviousResult();
		
		/*
		 * Retrieve the identifier string from the previous result. This is required to
		 * know exactly which payload corresponds to the response from the previous
		 * stage. If a valid identifier could not be retrieved, then don't bother
		 * listening for a response.
		 */
		String identifierKey = identifierKey();
		String identifier = context.getJMeterVariables().get(identifierKey);
		if (StringUtils.isBlank(identifier)) {
			logger().error("Identifier from previous result could not be parsed or is invalid");
			return;
		}
		
		/*
		 * If previous sampler stage reported a failure, then don't bother listening for
		 * a response.
		 */
		if (!previousResult.isSuccessful()) {
			logger().error("Previous result reported a failure");
			return;
		}
		
		logger().info("Waiting for response with identifier '{}'", identifier);
		
		CompletableFuture<String> future = httpListener().getResponse(identifier);
		String response;
		try {
			response = future.get();
			result.sampleEnd();
			logger().info("Received async response with identifier '{}'", identifier);
			populateResult(result, previousResult, response);
		} catch (CancellationException | ExecutionException e) {
			logger().error("Response timed out for identifier '{}'", identifier);
		} catch (Exception e) {
			logger().error("Exception encountered while awaiting response with identifier '{}': {}", e.getClass().getSimpleName());
		}
		
		httpListener().notifyComplete(identifier);
	}
	
	protected void populateResult(SampleResult result, SampleResult previousResult, String response) {
		result.setBodySize((long) response.length());
		result.setContentType("application/json");
		result.setResponseData(response, "UTF-8");
		result.setResponseCodeOK();
		result.setSuccessful(true);
	}
	
	/**
	 * Get the HTTP listener.
	 */
	protected abstract AbstractAsyncHttpListener httpListener();

	/**
	 * Get the key for retrieving the identifier variable from the context.
	 */
	protected abstract String identifierKey();
	
	/**
	 * Get the logger for this class.
	 */
	protected abstract Logger logger();
	
}
