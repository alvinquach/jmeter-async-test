package com.alvinquach.jmeter.sampler.async;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic sampler plug-in for listening to asynchronous HTTP responses from an
 * API endpoint that was hit by the AsyncHttpRequestSampler. The response is
 * tracked by an identifier value provided in the previous sample result by the
 * AsyncHttpRequestSampler. This requires the unique identifier to be present in
 * the asynchronous response.
 * <p>
 * The test plan should be set up such that a AsyncHttpRequestSampler is placed
 * directly before this in the same thread group. In addition, the test plan
 * should include an AsyncHttpListenerInitializer that runs once before this
 * sampler runs the first time.
 *
 * @author Alvin Quach
 */
public class AsyncHttpResponseSampler extends AbstractJavaSamplerClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpResponseSampler.class);

	private static AsyncHttpListener httpListener;
	
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		if (httpListener == null) {
			httpListener = AsyncHttpListener.instance();
		}
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		/*
		 * Instantiate the sample result and record its start time.
		 */
		AsyncRequestResult<String> result = new AsyncRequestResult<>();
		result.sampleStart();
		
		/*
		 * Retrieve the result of the previous sampler stage from the context.
		 */
		SampleResult _previousResult = context.getJMeterContext().getPreviousResult();
		
		/*
		 * Since the AsyncHttpResponseSampler is intended to be placed right after a
		 * AsyncHttpRequestSampler in the sampler chain, the previous result should be
		 * of type AsyncRequestResult.
		 */
		if (!(_previousResult instanceof AsyncRequestResult<?>)) {
			return null;
		}
		AsyncRequestResult<?> previousResult = (AsyncRequestResult<?>) _previousResult;
		
		/*
		 * If previous sampler stage reported a failure, then don't bother listening for
		 * a response.
		 */
		if (!previousResult.isSuccessful()) {
			LOGGER.error("Previous result reported a failure");
			return null;
		}
		
		/*
		 * Retrieve the identifier string from the previous result. This is required to
		 * know exactly which payload corresponds to the response from the previous
		 * stage. If a valid identifier could not be retrieved, then don't bother
		 * listening for a response.
		 */
		String identifier = getIdentifierFromPreviousResult(previousResult);
		if (StringUtils.isBlank(identifier)) {
			LOGGER.error("Identifier from previous result could not be parsed or is invalid");
			return null;
		}
		
		LOGGER.info("Waiting for response with identifier '{}'", identifier);
		
		CompletableFuture<String> future = httpListener.getResponse(identifier);
		String response;
		try {
			response = future.get();
			result.sampleEnd();
		} catch (Exception e) {
			LOGGER.error("Exception encountered while awaiting response with identifier '{}': {}", identifier, e.getClass().getSimpleName());
			return null;
		}
		
		LOGGER.info("Received async response with identifier '{}'", identifier);
		httpListener.notifyComplete(identifier);
		
		populateResult(result, previousResult, response);
		return result;
	}
	
	private String getIdentifierFromPreviousResult(AsyncRequestResult<?> previousResult) {
		Object identifier = previousResult.getIdentifier();
		if (identifier != null) {
			return identifier.toString();
		}
		return null;
	}
	
	private void populateResult(AsyncRequestResult<String> result, AsyncRequestResult<?> previousResult, String response) {
		result.setOriginalRequestStartTime(previousResult.getOriginalRequestStartTime());
		result.setBodySize((long) response.length());
		result.setContentType("application/json");
		result.setResponseData(response, "UTF-8");
		result.setResponseCodeOK();
		result.setSuccessful(true);
	}
	
}
