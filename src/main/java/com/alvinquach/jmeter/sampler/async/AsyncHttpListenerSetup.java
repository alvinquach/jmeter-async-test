package com.alvinquach.jmeter.sampler.async;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special sampler that initializes the AsyncHttpListener singleton instance. It
 * is recommended to run this sampler in a setUp thread group before the main
 * thread group(s). The AsyncHttpListener will automatically be removed at the
 * end of the test, so the test can be run again with different parameters
 * without having to restart JMeter.
 * 
 * @author Alvin Quach
 */
public final class AsyncHttpListenerSetup extends AbstractJavaSamplerClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpListenerSetup.class);

	private static final String PORT_NUMBER_KEY = "listenerPortNumber";
	
	private static final String IDENTIFIER_PATH_KEY = "identifierPath";
	
	private static final String TIMEOUT_DURATION_KEY = "timeoutDuration";
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultArguments = new Arguments();
		defaultArguments.addArgument(PORT_NUMBER_KEY, "8080");
		defaultArguments.addArgument(IDENTIFIER_PATH_KEY, StringUtils.EMPTY);
		defaultArguments.addArgument(TIMEOUT_DURATION_KEY, "5000");
		return defaultArguments;
	}

	@Override
	public void setupTest(JavaSamplerContext context) {
		LOGGER.info("Setting up AsyncHttpListener instance...");
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		SampleResult result = new SampleResult();
		result.sampleStart();

		String portNumberValue = context.getParameter(PORT_NUMBER_KEY);
		int portNumber;
		try {
			portNumber = Integer.parseInt(portNumberValue);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Port number must be an integer");
		}

		String identifierPath = context.getParameter(IDENTIFIER_PATH_KEY);
		if (StringUtils.isEmpty(identifierPath)) {
			throw new IllegalArgumentException("Identifier path is required");
		}
		
		String timeoutDurationValue = context.getParameter(TIMEOUT_DURATION_KEY);
		long timeoutDuration;
		try {
			timeoutDuration = StringUtils.isBlank(timeoutDurationValue) ? 0 : Long.parseLong(timeoutDurationValue);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Timeout duration must be an integer");
		}

		AsyncHttpListener httpListener = AsyncHttpListener.instantiate(portNumber, identifierPath, timeoutDuration);
		try {
			httpListener.start();
			LOGGER.info("AsyncHttpListener started on port {}", portNumber);
			result.setSuccessful(true);
		} catch (IOException e) {
			LOGGER.error("Could not start the AsyncHttpListener on port {}", portNumber);
			result.setSuccessful(false);
		}

		result.sampleEnd();
		return result;
	}

	@Override
	public void teardownTest(JavaSamplerContext context) {
		LOGGER.info("Tearing down AsyncHttpListener instance...");
		try {
			AsyncHttpListener oldInstance = AsyncHttpListener.removeInstance();
			oldInstance.stop();
			LOGGER.info("AsyncHttpListener stopped");
		} catch (Exception e) {
			LOGGER.error("Exception encoutered while attempting to stop AsyncHttpListener: {}", e.getClass().getSimpleName());
		}
	}
	
}
