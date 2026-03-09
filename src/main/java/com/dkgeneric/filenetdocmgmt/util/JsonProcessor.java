package com.dkgeneric.filenetdocmgmt.util;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class JsonProcessor {

	private final ObjectMapper objectMapper;
	
	public JsonProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	public String processTemplate(String jsonTemplate, Map<String, Object> values) {
		try {
			JsonNode template = objectMapper.readTree(jsonTemplate);
			JsonNode processedNode = processNode(template, values);
			return objectMapper.writeValueAsString(processedNode);
		}catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Invalid Json Template");
		}
	}
	
	private JsonNode processNode(JsonNode node, Map<String, Object> values) {
		if(node.isObject()) {
			return processObjectNode((ObjectNode) node, values);
		}else if(node.isArray()) {
			return processArrayNode((ArrayNode) node, values);
		}else if(node.isTextual()) {
			return processTextNode(node,values) ;
		}
		return node;
	}

	private JsonNode processTextNode(JsonNode textNode, Map<String, Object> values) {
		String text = textNode.asText();
		if (isPlaceholder(text)) {
			String key = extractKey(text);
			if(values.containsKey(key)) {
				Object value = values.get(key);
				return objectMapper.valueToTree(value);
			}else {
				return textNode;
			}
		}
		return textNode;
	}

	private String extractKey(String placeholder) {
		return placeholder.substring(2, placeholder.length()-1);
	}

	private boolean isPlaceholder(String text) {
		return text.startsWith("${")&& text.endsWith("}");
	}

	private JsonNode processArrayNode(ArrayNode arrayNode, Map<String, Object> values) {
		ArrayNode newArrayNode = objectMapper.createArrayNode();
		arrayNode.forEach(element->{
			JsonNode processedElement = processNode(element, values);
			newArrayNode.add(processedElement);
		});
		return newArrayNode;
	}

	private JsonNode processObjectNode(ObjectNode node, Map<String, Object> values) {
		ObjectNode newNode = objectMapper.createObjectNode();
		node.fields().forEachRemaining(entry ->{
			JsonNode processedValue = processNode(entry.getValue(), values);
			newNode.set(entry.getKey(), processedValue);
		});
		return newNode;
	}
}
