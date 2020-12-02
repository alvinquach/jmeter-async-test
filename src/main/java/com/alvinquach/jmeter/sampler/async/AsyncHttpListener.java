package com.alvinquach.jmeter.sampler.async;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvinquach.jmeter.sampler.util.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Generic HTTP server that listens for asynchronous HTTP responses. The server
 * instance will have to be initialized by running a AsyncHttpListenerSetup
 * sampler once, before running any other samplers that uses the server.
 * 
 * @see <a href="https://github.com/pleutres/jmeter-asynchronous-http">https://github.com/pleutres/jmeter-asynchronous-http</a>
 */
public class AsyncHttpListener extends AbstractAsyncHttpListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpListener.class);
	
	private static AsyncHttpListener instance;
	
	/**
	 * Returns the AsyncHttpListener singleton instance.
	 */
	public static AsyncHttpListener instance() {
		if (instance == null) {
			throw new IllegalStateException("AsyncHttpListener instance has not been initialized yet");
		}
		return instance;
	}
	
	/**
	 * Instantiates the AsyncHttpListener singleton instance.
	 */
	public static AsyncHttpListener instantiate(int port, String identifierPath, long timeoutDuration) {
		synchronized (AsyncHttpListener.class) {
			if (instance != null) {
				throw new IllegalStateException("AsyncHttpListener instance is already initialized");
			}
			return instance = new AsyncHttpListener(port, identifierPath, timeoutDuration);
		}
	}

	/**
	 * Removes the AsyncHttpListener singleton instance and returns it.
	 */
	public static AsyncHttpListener removeInstance() {
		synchronized (AsyncHttpListener.class) {
			if (instance == null) {
				throw new IllegalStateException("AsyncHttpListener instance has not been initialized yet");
			}
			AsyncHttpListener oldInstance = instance;
			instance = null;
			
			return oldInstance;
		}
	}

	private final String identifierPath;
	
	private AsyncHttpListener(int port, String identifierPath, long timeoutDuration) {
		super(port, timeoutDuration);
		this.identifierPath = identifierPath;
	}

	@Override
	protected Response processResponseBody(IHTTPSession session, String body) {
   	    /*
		 * Deserialize the response body into a JsonNode.
		 */
		JsonNode jsonNode;
		try {
			jsonNode = JsonNodeUtils.deserializeString(body);
		} catch (JsonProcessingException e) {
			LOGGER.error("Exception encountered while deserializing the response body: {}", e.getClass().getSimpleName());
			return RESPONSE_400;
		}
	    
		/*
		 * Retrieve the identifier from the JsonNode at the location specified by the
		 * identifier path.
		 */
	    String identifier = JsonNodeUtils.getNumberOrTextAsString(jsonNode, identifierPath);
	    if (StringUtils.isEmpty(identifier)) {
	    	LOGGER.error("Payload does not contain a valid identifier value at the specified path '{}'", identifierPath);
	    	return RESPONSE_400;
	    }
	    
		/*
		 * Retrieve (or add) the CompletableFuture object from the results map that is
		 * associated with the identifier key, and mark it as complete.
		 */
	    ResponseWrapper response = addOrRetrieveResponse(identifier);
	    response.future.complete(body);
	    
	    return RESPONSE_200;
    }

	@Override
	protected Logger logger() {
		return LOGGER;
	}
	
}
