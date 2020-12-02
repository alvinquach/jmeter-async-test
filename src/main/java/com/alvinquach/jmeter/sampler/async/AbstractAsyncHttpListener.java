package com.alvinquach.jmeter.sampler.async;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.alvinquach.jmeter.sampler.util.HttpListenerUtils;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * 
 * @see <a href="https://github.com/pleutres/jmeter-asynchronous-http">https://github.com/pleutres/jmeter-asynchronous-http</a>
 */
public abstract class AbstractAsyncHttpListener extends NanoHTTPD {

	protected static class ResponseWrapper {
		
		public final CompletableFuture<String> future = new CompletableFuture<>();
		
		public final long expirationTime;
		
		ResponseWrapper(long timeoutDuration) {
			if (timeoutDuration == 0) {
				expirationTime = Long.MAX_VALUE;
			} else {
				expirationTime = new Date().getTime() + timeoutDuration;
			}
		}
	}
	
	protected static final String MIME_JSON = "application/json";
	
	protected static final Response RESPONSE_200 = NanoHTTPD.newFixedLengthResponse("OK");
	
	protected static final Response RESPONSE_400;
	static {
		IStatus status = Status.BAD_REQUEST;
		String message = status.getDescription();
		RESPONSE_400 = NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, message);
	}
	
	protected static final Response RESPONSE_405;
	static {
		IStatus status = Status.METHOD_NOT_ALLOWED;
		String message = "Currently, only POST responses are supported";
		RESPONSE_405 = NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, message);
	}
	
	protected static final Response RESPONSE_415;
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
		RESPONSE_415 = NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, message);
	}
	
	protected static final long EXPIRED_RESPONSE_CHECK_INTERVAL = 100;
	
	private final ConcurrentHashMap<String, ResponseWrapper> responses = new ConcurrentHashMap<>();
	
	private final long timeoutDuration;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public AbstractAsyncHttpListener(int port) {
		this(port, 0);
	}
	
	public AbstractAsyncHttpListener(int port, long timeoutDuration) {
		super(port);
		
		this.timeoutDuration = timeoutDuration;
		if (timeoutDuration > 0) {
			scheduler.scheduleAtFixedRate(this::timeoutReponses, timeoutDuration, EXPIRED_RESPONSE_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
		}
	}
	
	@Override
	public void start() throws IOException {
		super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}
	
	public CompletableFuture<String> getResponse(String identifier) {
		if (identifier == null) {
			// TODO Throw exception instead
			return null;
		}
		return addOrRetrieveResponse(identifier).future;
	}

	public void notifyComplete(String identifier) {
		ResponseWrapper removed = responses.remove(identifier);
		if (removed != null && removed.future.isDone()) {
			removed.future.cancel(true);
		}
	}

	protected ResponseWrapper addOrRetrieveResponse(String identifier) {
		return responses.computeIfAbsent(identifier, this::mappingFunction);
	}
	
	@Override
	public Response serve(IHTTPSession session) {
	    /*
	     * Currently, only POST requests are supported.
	     */
		if (session.getMethod() != Method.POST) {
			logger().error("Received a {} request; only POST requests are supported at this time", session.getMethod().name());
	    	return RESPONSE_405;
	    }
	    
	    /*
	     * Current, only application/json content type is supported.
	     */
		String contentType = session.getHeaders().get("content-type");
	    if (StringUtils.isBlank(contentType)) {
			logger().error("Content-type was not provided; content-type must be {}", contentType, MIME_JSON);
	    	return RESPONSE_415;
	    }
	    if (!contentType.toLowerCase().startsWith(MIME_JSON)) {
			logger().error("Content-type is {}; only {} is supported at this time", contentType, MIME_JSON);
			return RESPONSE_415;
	    }
	    
	    /*
	     * Retrieve the body from the HTTP response as a string.
	     */
	    String body;
	    try {
	    	body = HttpListenerUtils.parseResponseBody(session);
	    	logger().debug("Received response: '{}'", body);
	    } catch (Exception e) {
			logger().error("Exception encountered while parsing response body: {}", e.getClass().getSimpleName());
			return RESPONSE_400;
	    }
	    if (StringUtils.isBlank(body)) {
			logger().error("Response does not contain a body");
	    	return RESPONSE_400;
	    }
	    
	    return processResponseBody(session, body);
	}
	
	protected abstract Response processResponseBody(IHTTPSession session, String body);
	
	/**
	 * Goes through the responses that have not been completed yet and cancels the
	 * ones that are expired.
	 */
	private void timeoutReponses() {
		int count = 0;
		long now = new Date().getTime();
		for (ResponseWrapper response : responses.values()) {
			if (!response.future.isDone() && now > response.expirationTime) {
				response.future.cancel(true);
				count++;
			}
		}
		if (count > 0) {
			logger().info("Cancelled {} awaiting responses due to exceeding timeout limit.", count);
		}
	}
	
	@Override
	public void stop() {
		scheduler.shutdown();
		super.stop();
		
		int count = 0;
		for (ResponseWrapper response : responses.values()) {
			if (!response.future.isDone()) {
				response.future.cancel(true);
				count++;
			}
		}
		if (count > 0) {
			logger().info("Cancelled {} awaiting responses due to HTTP listener shutting down.", count);
		}
	}
	
	protected ResponseWrapper mappingFunction(String identifier) {
		return new ResponseWrapper(timeoutDuration);
	}
	
	protected abstract Logger logger();
	
}
