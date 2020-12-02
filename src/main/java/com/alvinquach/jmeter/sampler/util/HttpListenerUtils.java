package com.alvinquach.jmeter.sampler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public final class HttpListenerUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpListenerUtils.class);
	
	private HttpListenerUtils() {
		
	}
	
	public static String parseResponseBody(IHTTPSession session) throws Exception {
		String length = session.getHeaders().get("content-length");
		if (length == null) {
			LOGGER.error("Content length is null");
			return null;
		}
		int contentLength = Integer.parseInt(length);
		byte[] buffer = new byte[contentLength];
		session.getInputStream().read(buffer, 0, contentLength);
		return new String(buffer);
	}
	
}
