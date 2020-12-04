package com.alvinquach.jmeter.sampler.async;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
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
 * should include an AsyncHttpListenerSetup that runs once before this sampler
 * runs the first time.
 *
 * @author Alvin Quach
 */
public class AsyncHttpResponseSampler extends AbstractAsyncHttpResponseSampler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpResponseSampler.class);

	private static final String IDENTIFIER_KEY = "identifier";
	
	private static AsyncHttpListener httpListener;
	
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		if (httpListener == null) {
			httpListener = AsyncHttpListener.instance();
		}
	}
	
	@Override
	public void teardownTest(JavaSamplerContext context) {
		/*
		 * Must dereference the HTTP listener here since a new instance will be created
		 * when the test is run again.
		 */
		httpListener = null;
	}

	@Override
	protected AbstractAsyncHttpListener httpListener() {
		return httpListener;
	}

	@Override
	protected String identifierKey() {
		return IDENTIFIER_KEY;
	}
	
	@Override
	protected Logger logger() {
		return LOGGER;
	}
	
}
