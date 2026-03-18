package com.dkgeneric.filenetdocmgmt.service;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;

import com.dkgeneric.audit.common.FilenetContentLibConverter;
import com.dkgeneric.commons.model.json.JsonProperty;
import com.dkgeneric.commons.service.FilenetPropMappingService;
import com.dkgeneric.commons.service.KafkaConfigMappingService;
import com.dkgeneric.commons.service.KafkaProducerService;
import com.dkgeneric.filenet.content.common.ServiceException;
import com.dkgeneric.filenet.content.model.P8ContentObject;
import com.dkgeneric.filenet.content.service.ContentService;
import com.dkgeneric.filenet.content.service.DocumentService;
import com.dkgeneric.filenet.content.service.MappingService;
import com.dkgeneric.filenet.content.service.SchemaService;
import com.dkgeneric.filenet.content.service.SearchService;
import com.dkgeneric.filenetdocmgmt.configuration.ApplicationConfiguration;
import com.dkgeneric.filenetdocmgmt.util.JsonProcessor;
import com.dkgeneric.jpa.audit.repository.FilenetAuditRepository;
import com.dkgeneric.pdf.service.PDFService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

public class FilenetBaseService {

	protected static final String SAVE_P8AUDIT_ERROR = "Failed to save P8 audit log.\n Log data: {}.";

	@Autowired
	protected DocumentService documentService;
	@Autowired
	protected SearchService searchService;
	@Autowired
	protected ContentService contentService;
	@Autowired
	protected SchemaService schemaService;
	@Autowired
	protected ApplicationConfiguration applicationConfiguration;
	@Autowired
	protected MappingService mappingService;
	@Autowired
	protected FilenetContentLibConverter p8ContentLibConverter;
	@Autowired
	protected FilenetAuditRepository p8AuditRepository;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected FilenetPropMappingService p8PropMappingService;
	@Autowired
	protected HttpServletRequest request;
	@Autowired
	protected KafkaConfigMappingService kafkaConfigMappingService;
	@Autowired
	protected KafkaProducerService kafkaProducerService;
	@Autowired
	JsonProcessor jsonProcessor;
	@Autowired
	protected PDFService pdfService;

	protected Base64.Encoder base64Encoder = Base64.getEncoder();

	protected String getEncodedContent(P8ContentObject p8ContentObject) throws IOException, ServiceException {
		if (p8ContentObject != null && p8ContentObject.getResource() != null) {
			byte[] data = p8ContentObject.getResource().getBytes();
			if (data != null)
				return base64Encoder.encodeToString(data);
		}
		return null;
	}

	protected String getPropertyStringValue(JsonProperty property) {
		return (property == null || property.getPropertyValue() == null) ? null
				: property.getPropertyValue().toString();
	}
}
