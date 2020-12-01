package com.alvinquach.jmeter.sampler.async;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncHttpResponseSampler extends AbstractJavaSamplerClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpResponseSampler.class);

	private static final String PORT_NUMBER_KEY = "listenerPortNumber";
	
	private static final String IDENTIFIER_PATH_KEY = "identifierPath";
	
	private static AsyncHttpListener httpListener;
	
	private int listenerPortNumber;
	
	private String identifierPath;
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultPArguments = new Arguments();
		defaultPArguments.addArgument(PORT_NUMBER_KEY, "8080");
		defaultPArguments.addArgument(IDENTIFIER_PATH_KEY, "");
		return defaultPArguments;
	}
	
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		
		String portNumber = context.getParameter(PORT_NUMBER_KEY);
		listenerPortNumber = Integer.parseInt(portNumber);
		
		identifierPath = context.getParameter(IDENTIFIER_PATH_KEY);
		if (StringUtils.isEmpty(identifierPath)) {
			throw new IllegalArgumentException("Setup error: identifier path must not be blank");
		}

		/*
		 * TODO Create a sampler for initializing the HTTP listener that runs before the
		 * other samplers in its own thread group. This will allow the HTTP listener to
		 * be ready before to listen for async responses before any requests even hit
		 * the tested server, and should also eliminate the need to synchronize the
		 * operation.
		 */
		synchronized (AsyncHttpResponseSampler.class) {
			if (httpListener == null) {
				httpListener = new AsyncHttpListener(listenerPortNumber, identifierPath);
				try {
					httpListener.start();
					LOGGER.info("HTTP listen server started on port {}", listenerPortNumber);
				} catch (IOException e) {
					LOGGER.error("Could not start the HTTP listen server on port {}", listenerPortNumber);
				}
			}
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
			LOGGER.error("Prevous result reported a failure");
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
		
		LOGGER.info("Waiting for reponse with identifier '{}'", identifier);
		
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
