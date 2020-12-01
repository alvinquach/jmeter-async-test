package com.alvinquach.jmeter.sampler.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonNodeUtils {
	
	private static final String PATH_SEPARATOR = ".";
	
	private JsonNodeUtils() {
		
	}
	
	public static String getNumberOrTextAsString(JsonNode jsonNode, String path) {
		int separatorIndex = path.indexOf(PATH_SEPARATOR);
		String currentPath = separatorIndex == -1 ? path : path.substring(0, separatorIndex);

		JsonNode currentPathNode = jsonNode.get(currentPath);
		if (currentPathNode == null || currentPathNode.isNull()) {
			return null;
		}
		if (currentPathNode.isNumber() || currentPathNode.isTextual()) {
			return currentPathNode.asText();
		}
		if (currentPathNode.isObject() && separatorIndex != -1) {
			// Find next path in JsonNode recursively.
			return getNumberOrTextAsString(currentPathNode, path.substring(separatorIndex));
		}
		return null;
	}

}
