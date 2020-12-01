package com.alvinquach.jmeter.sampler.async;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvinquach.jmeter.sampler.util.JsonNodeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * 
 * @see <a href="https://github.com/pleutres/jmeter-asynchronous-http">https://github.com/pleutres/jmeter-asynchronous-http</a>
 */
public class AsyncHttpListener extends NanoHTTPD {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpListener.class);
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final String MIME_JSON = "application/json";
	
	private static final Response RESPONSE_200 = newFixedLengthResponse("OK");
	
	private static final Response RESPONSE_400;
	static {
		IStatus status = Status.BAD_REQUEST;
		String message = status.getDescription();
		RESPONSE_400 = newFixedLengthResponse(status, MIME_PLAINTEXT, message);
	}
	
	private static final Response RESPONSE_405;
	static {
		IStatus status = Status.METHOD_NOT_ALLOWED;
		String message = "Currently, only POST responses are supported";
		RESPONSE_405 = newFixedLengthResponse(status, MIME_PLAINTEXT, message);
	}
	
	private static final Response RESPONSE_415;
	static {
		IStatus status = new IStatus() {
			private static final String DESCRIPTION = "Unsupported Media Type";
			@Override public int getRequestStatus() {
				return 415;
			}
			@Override public String getDescription() {
				return DESCRIPTION;
			}
		};
		String message = "Currently, only " + MIME_JSON + " content type is supported";
		RESPONSE_415 = newFixedLengthResponse(status, MIME_PLAINTEXT, message);
	}

	private final ConcurrentHashMap<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
	
	private final String identifierPath;
	
	public AsyncHttpListener(int port, String identifierPath) {
		super(port);
		this.identifierPath = identifierPath;
	}

	@Override
	public void start() throws IOException {
		super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}
	
	public CompletableFuture<String> getResponse(String identifier) {
		if (StringUtils.isEmpty(identifier)) {
			// TODO Throw exception instead
			return null;
		}
		return addOrRetrieveResponse(identifier);
	}

	public void notifyComplete(String identifier) {
		CompletableFuture<String> removed = responses.remove(identifier);
		if (removed != null && removed.isDone()) {
			removed.cancel(true);
		}
	}

	private CompletableFuture<String> addOrRetrieveResponse(String identifier) {
		return responses.computeIfAbsent(identifier, this::mappingFunction);
	}

	private CompletableFuture<String> mappingFunction(String identifier) {
		return new CompletableFuture<>();
	}

	@Override
	public Response serve(IHTTPSession session) {
	    /*
	     * Currently, only POST requests are supported.
	     */
		if (session.getMethod() != Method.POST) {
			LOGGER.error("Received a {} request; only POST requests are supported at this time", session.getMethod().name());
	    	return RESPONSE_405;
	    }
	    
	    /*
	     * Current, only application/json content type is supported.
	     */
		String contentType = session.getHeaders().get("content-type");
	    if (StringUtils.isBlank(contentType)) {
			LOGGER.error("Content-type was not provided; content-type must be {}", contentType, MIME_JSON);
	    	return RESPONSE_415;
	    }
	    if (!contentType.toLowerCase().startsWith(MIME_JSON)) {
			LOGGER.error("Content-type is {}; only {} is supported at this time", contentType, MIME_JSON);
			return RESPONSE_415;
	    }
	    
	    /*
	     * Retrieve the body from the HTTP response as a string.
	     */
	    String body = parseResponseBody(session);
	    if (StringUtils.isBlank(body)) {
			LOGGER.error("Response does not contain a body");
	    	return RESPONSE_400;
	    }
	    LOGGER.debug("Received response: '{}'", body);

	    /*
	     * Deserialize the response body into a JsonNode.
	     */
	    JsonNode jsonNode = deserializeResponseBody(body);
	    if (jsonNode == null) {
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
	    CompletableFuture<String> future = addOrRetrieveResponse(identifier);
	    future.complete(body);
	    
	    return RESPONSE_200;
    }
		
	private String parseResponseBody(IHTTPSession session) {
		String length = session.getHeaders().get("content-length");
		if (length == null) {
			LOGGER.error("Content length is null");
			return null;
		}
		try {
			int contentLength = Integer.parseInt(length);
			byte[] buffer = new byte[contentLength];
			session.getInputStream().read(buffer, 0, contentLength);
			return new String(buffer);
		} catch (Exception e) {
			LOGGER.error("Exception encountered while parsing response body: {}", e.getClass().getSimpleName());
			return null;
		}
	}
	
	private JsonNode deserializeResponseBody(String body) {
		try {
			return MAPPER.readTree(body);
		} catch (JsonProcessingException e) {
			LOGGER.error("Exception encountered while deserializing the response body: {}", e.getClass().getSimpleName());
			return null;
		}
	}
	
}
