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
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultPArguments = new Arguments();
		defaultPArguments.addArgument(PORT_NUMBER_KEY, "8080");
		defaultPArguments.addArgument(IDENTIFIER_PATH_KEY, StringUtils.EMPTY);
		return defaultPArguments;
	}

	@Override
	public void setupTest(JavaSamplerContext context) {
		LOGGER.info("Setting up AsyncHttpListener instance...");
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		SampleResult result = new SampleResult();
		result.sampleStart();

		String portNumber = context.getParameter(PORT_NUMBER_KEY);
		int port;
		try {
			port = Integer.parseInt(portNumber);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Port number must be an integer");
		}

		String identifierPath = context.getParameter(IDENTIFIER_PATH_KEY);
		if (StringUtils.isEmpty(identifierPath)) {
			throw new IllegalArgumentException("Identifier path is required");
		}

		AsyncHttpListener httpListener = AsyncHttpListener.instantiate(port, identifierPath);
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
