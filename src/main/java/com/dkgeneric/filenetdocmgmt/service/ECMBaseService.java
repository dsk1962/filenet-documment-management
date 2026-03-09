package com.dkgeneric.filenetdocmgmt.service;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;

import com.davita.ecm.p8.audit.common.P8ContentLibConverter;
import com.davita.ecm.p8.audit.service.KafkaOperationAuditService;
import com.dkgeneric.commons.impl.cache.TaxonomyServiceCache;
import com.dkgeneric.commons.model.json.JsonProperty;
import com.dkgeneric.commons.service.DavitaEncryptService;
import com.dkgeneric.commons.service.ImageConversionService;
import com.dkgeneric.commons.service.KafkaConfigMappingService;
import com.dkgeneric.commons.service.KafkaProducerService;
import com.dkgeneric.commons.service.P8PropMappingService;
import com.davita.ecm.p8.content.common.ServiceException;
import com.davita.ecm.p8.content.model.P8ContentObject;
import com.davita.ecm.p8.content.request.BaseRequest;
import com.davita.ecm.p8.content.service.ContentService;
import com.davita.ecm.p8.content.service.DocumentService;
import com.davita.ecm.p8.content.service.MappingService;
import com.davita.ecm.p8.content.service.SchemaService;
import com.davita.ecm.p8.content.service.SearchService;
import com.davita.ecm.pdf.service.PDFService;
import com.davita.jpa.taxonomy.p8.audit.repository.P8AuditRepository;
import com.dkgeneric.filenetdocmgmt.configuration.ApplicationConfiguration;
import com.dkgeneric.filenetdocmgmt.util.JsonProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

public class ECMBaseService {

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
	protected P8ContentLibConverter p8ContentLibConverter;
	@Autowired
	protected P8AuditRepository p8AuditRepository;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected P8PropMappingService p8PropMappingService;
	@Autowired
	protected TaxonomyServiceCache taxonomyServiceCache;
	@Autowired
	protected HttpServletRequest request;
	@Autowired
	protected DavitaEncryptService davitaEncryptService;
	@Autowired
	protected KafkaConfigMappingService kafkaConfigMappingService;
	@Autowired
	protected KafkaProducerService kafkaProducerService;
	@Autowired
	protected KafkaOperationAuditService kafkaAuditService;
	@Autowired
	JsonProcessor jsonProcessor;
	@Autowired
	protected PDFService pdfService;

	protected ImageConversionService conversionService = new ImageConversionService();

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

	protected void setCredentials(BaseRequest baseRequest) throws Exception {
		baseRequest.setUserName(request.getHeader("username"));
		baseRequest.setPassword(davitaEncryptService.decryptPhrase(request.getHeader("password")));
	}
}
