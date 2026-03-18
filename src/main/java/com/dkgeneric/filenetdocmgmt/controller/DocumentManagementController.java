package com.dkgeneric.filenetdocmgmt.controller;

import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dkgeneric.audit.model.RequestParameter;
import com.dkgeneric.audit.model.WebServiceRequestAuditEntry;
import com.dkgeneric.commons.model.json.JsonDocument;
import com.dkgeneric.commons.model.json.JsonDocumentWithSearchFilter;
import com.dkgeneric.commons.model.json.JsonResource;
import com.dkgeneric.commons.model.json.JsonSearch;
import com.dkgeneric.commons.service.FilenetPropMappingService;
import com.dkgeneric.commons.service.ValidationService;
import com.dkgeneric.filenet.content.resources.P8ContentResource;
import com.dkgeneric.filenetdocmgmt.configuration.ApplicationConfiguration;
import com.dkgeneric.filenetdocmgmt.service.DocumentManagementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Document Management Controller", description = "Document Management Controller")
@RestController
@Slf4j
@RequestMapping("documents")

public class DocumentManagementController extends ECMBaseController {

	public DocumentManagementController(DocumentManagementService documentManagementService,
			ApplicationConfiguration applicationConfiguration, FilenetPropMappingService p8PropMappingService,
			ValidationService validationService) {
		super(applicationConfiguration, p8PropMappingService, validationService);
		this.documentManagementService = documentManagementService;
	}

	public static final String CREATE_DOCUMENT = "Create Document";
	public static final String CREATE_VESRION = "Create Version";
	public static final String UPDATE_DOCUMENT = "Update Document";
	public static final String RETRIEVE_DOCUMENTS = "Retrieve Documents";
	public static final String GET_CONTENT = "Get Content";

	private DocumentManagementService documentManagementService;

	@Operation(description = "Create new document")
	@PostMapping(path = "document", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonNode> createDocument(@RequestBody JsonResource<JsonDocument> jsonResource) throws Exception {
		log.info("POST /document. resource: {} ", jsonResource);
		WebServiceRequestAuditEntry auditEntry = auditService.createWebServiceRequestAuditEntry(CREATE_DOCUMENT,
				new RequestParameter(RequestParameter.BODY_PARAMETER_NAME, jsonResource));
		auditEntry.setAppServiceAccount(getServiceAccount());
		auditEntry.setAppUser(jsonResource.getAppUser());
		validationService.validateObject(jsonResource);
		ObjectNode result = createSuccessJson();
		String guid = documentManagementService.createDocument(jsonResource);
		auditEntry.setObjectId(guid);
		result.put("guid", guid);
		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@Operation(description = "Create new document with multipart")
	@PostMapping(path = "document/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonNode> createDocumentMultipart(@RequestParam String jsonResource,
			@RequestPart MultipartFile file) throws Exception {
		log.info("POST /document/multipart. resource: {} ", jsonResource);
		WebServiceRequestAuditEntry auditEntry = auditService.createWebServiceRequestAuditEntry(CREATE_DOCUMENT,
				new RequestParameter("jsonResource", jsonResource));

		auditEntry.setAppServiceAccount(getServiceAccount());
		JsonResource<JsonDocument> jsonResourceO = objectMapper.readValue(jsonResource,
				new TypeReference<JsonResource<JsonDocument>>() {
				});
		auditEntry.setAppUser(jsonResourceO.getAppUser());
		ObjectNode result = createSuccessJson();
		String guid = documentManagementService.createDocument(jsonResourceO, file);
		auditEntry.setObjectId(guid);
		result.put("guid", guid);
		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@Operation(description = "Retrieve  documents")
	@PostMapping(path = "document/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<JsonDocument>> searchDocuments(@RequestBody JsonResource<JsonSearch> jsonResource) throws Exception {
		log.info("POST /document/search. resource: {} ", jsonResource);
		WebServiceRequestAuditEntry auditEntry = auditService.createWebServiceRequestAuditEntry(RETRIEVE_DOCUMENTS,
				new RequestParameter(RequestParameter.BODY_PARAMETER_NAME, jsonResource));
		auditEntry.setAppServiceAccount(getServiceAccount());
		auditEntry.setAppUser(jsonResource.getAppUser());
		validationService.validateObject(jsonResource);
		List<JsonDocument> result = documentManagementService.searchDocuments(jsonResource);
		return new ResponseEntity<>(result, CollectionUtils.isEmpty(result) ? HttpStatus.NO_CONTENT : HttpStatus.OK);
	}

	@Operation(description = "Update document properties")
	@PutMapping(path = "document", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public JsonNode updateDocument(@RequestBody JsonResource<JsonDocumentWithSearchFilter> jsonResource) throws Exception {
		log.info("PUT /document. resource: {} ", jsonResource);
		WebServiceRequestAuditEntry auditEntry = auditService.createWebServiceRequestAuditEntry(UPDATE_DOCUMENT,
				new RequestParameter(RequestParameter.BODY_PARAMETER_NAME, jsonResource));
		auditEntry.setAppServiceAccount(getServiceAccount());
		auditEntry.setAppUser(jsonResource.getAppUser());
		validationService.validateObject(jsonResource);
		documentManagementService.updateDocument(jsonResource, auditEntry);
		return createSuccessJson();
	}

	@Operation(description = "Update document properties and content(Create new version)")
	@PutMapping(path = "documentcontent", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public JsonNode updateDocumentContent(@RequestBody JsonResource<JsonDocumentWithSearchFilter> jsonResource) throws Exception {
		log.info("PUT /documentcontent. resource: {} ", jsonResource);
		WebServiceRequestAuditEntry auditEntry = auditService.createWebServiceRequestAuditEntry(CREATE_VESRION,
				new RequestParameter(RequestParameter.BODY_PARAMETER_NAME, jsonResource));
		auditEntry.setAppServiceAccount(getServiceAccount());
		auditEntry.setAppUser(jsonResource.getAppUser());
		validationService.validateObject(jsonResource);
		documentManagementService.createDocumentVersion(jsonResource, auditEntry);
		return createSuccessJson();
	}

	@Operation(description = "Get document content")
	@GetMapping(path = "document/content")
	public ResponseEntity<InputStreamResource> getContent(@RequestParam String guid,
			@RequestParam(required = false) String converter) throws Exception {
		log.info("GET /document/content. guid: {} ", guid);
		WebServiceRequestAuditEntry auditEntry = auditService.createWebServiceRequestAuditEntry(GET_CONTENT,
				new RequestParameter("guid", guid));
		auditEntry.setAppServiceAccount(getServiceAccount());
		auditEntry.setObjectId(guid);
		P8ContentResource p8ContentResource = documentManagementService.getContent(guid, converter);
		if (p8ContentResource == null)
			return ResponseEntity.noContent().build();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(p8ContentResource.getContentType()));
		headers.setContentDisposition(
				ContentDisposition.builder("attachment").filename(p8ContentResource.getFileName()).build());
		return new ResponseEntity<>(new InputStreamResource(p8ContentResource.getInputStream()), headers,
				HttpStatus.OK);
	}
}
