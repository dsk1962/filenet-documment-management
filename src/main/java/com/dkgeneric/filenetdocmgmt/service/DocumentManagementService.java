package com.dkgeneric.filenetdocmgmt.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dkgeneric.audit.model.WebServiceRequestAuditEntry;
import com.dkgeneric.commons.common.ExceptionConstants;
import com.dkgeneric.commons.exceptions.CommonsErrorMessageServiceException;
import com.dkgeneric.commons.exceptions.FilenetPropMappingNotFoundException;
import com.dkgeneric.commons.model.json.JsonDocument;
import com.dkgeneric.commons.model.json.JsonDocumentWithSearchFilter;
import com.dkgeneric.commons.model.json.JsonProperty;
import com.dkgeneric.commons.model.json.JsonResource;
import com.dkgeneric.commons.model.json.JsonSearch;
import com.dkgeneric.commons.service.ImageConversionService;
import com.dkgeneric.filenet.content.common.ServiceException;
import com.dkgeneric.filenet.content.model.P8ContentObject;
import com.dkgeneric.filenet.content.model.P8Object;
import com.dkgeneric.filenet.content.request.CreateDocumentRequest;
import com.dkgeneric.filenet.content.request.CreateDocumentVersionRequest;
import com.dkgeneric.filenet.content.request.GetContentRequest;
import com.dkgeneric.filenet.content.request.GetDocumentByIdRequest;
import com.dkgeneric.filenet.content.request.SearchRequest;
import com.dkgeneric.filenet.content.request.UpdateDocumentMetadataRequest;
import com.dkgeneric.filenet.content.resources.P8ContentResource;
import com.dkgeneric.filenet.content.response.CreateDocumentResponse;
import com.dkgeneric.filenet.content.response.CreateDocumentVersionResponse;
import com.dkgeneric.filenet.content.response.GetContentResponse;
import com.dkgeneric.filenet.content.response.GetDocumentByIdResponse;
import com.dkgeneric.filenet.content.response.SearchResponse;
import com.dkgeneric.filenet.content.response.UpdateDocumentMetadataResponse;
import com.dkgeneric.filenetdocmgmt.configuration.ApplicationConfiguration;
import com.dkgeneric.filenetdocmgmt.controller.DocumentManagementController;
import com.dkgeneric.jpa.audit.kafka.model.KafkaOperationAuditLog;
import com.dkgeneric.jpa.audit.repository.model.FilenetOperationAuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DocumentManagementService extends FilenetBaseService {

	public String createDocument(JsonResource<JsonDocument> jsonResource, MultipartFile file) throws Exception {
		jsonResource.getBody().applyMultipartFile(file);
		return createDocument(jsonResource);
	}

	public String createDocument(JsonResource<JsonDocument> jsonResource) throws Exception {
		P8ContentObject p8ContentObject = createP8ContentObject(jsonResource.getResourceType());
		CreateDocumentRequest request = new CreateDocumentRequest();
		request.setP8ContentObject(p8ContentObject);
		mappingService.convertJsonDocument(jsonResource.getBody(), jsonResource.getAppId(), request, true);
		mappingService.validateRequiredProperties(jsonResource.getAppId(), p8ContentObject);
		CreateDocumentResponse response = documentService.createDocument(request);
		if (!response.success())
			throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
		FilenetOperationAuditLog entry = p8ContentLibConverter.convert(response);
		entry.setAppId(ApplicationConfiguration.APPLICATION_NAME);
		entry.setAppUser(jsonResource.getAppUser());
		entry.setAppEvent(DocumentManagementController.CREATE_DOCUMENT);
		try {
			p8AuditRepository.save(entry);
		} catch (Exception e) {
			log.error(SAVE_P8AUDIT_ERROR, entry, e);
		}

		// Kafka check
		notifyKafka(response, jsonResource);
		return response.getP8DocumentId();
	}

	private void notifyKafka(CreateDocumentResponse response, JsonResource<JsonDocument> jsonResource) {
		String message = null;
		// send notification for specific appIds only
		if (applicationConfiguration.getKafkaAppIds() != null
				&& applicationConfiguration.getKafkaAppIds().contains(jsonResource.getAppId())) {
			ObjectNode root = objectMapper.createObjectNode();
			addDocumentDetails(root, response.getP8Object(), jsonResource.getAppId());
			addRequestDetails(root);
			try {
				message = objectMapper.writeValueAsString(root);
				// send async
				kafkaProducerService.sendMessageInSeparateThread(applicationConfiguration.getKafkaTopic(), null,
						message, KafkaOperationAuditLog.createAuditParametersMap(jsonResource.getAppId(),
								jsonResource.getAppUser(), response.getP8DocumentId(), "Create Document"));
			} catch (JsonProcessingException e) {
				log.error("Failed to convert data to message for kafka.", e);
			}
		}
	}

	private void addDocumentDetails(ObjectNode jsonNode, P8Object p8Object, String appId) {
		List<JsonProperty> list = mappingService.convertToJsonPropertyList(appId, p8Object, true);
		List<JsonProperty> result = new ArrayList<>(list.size());
		list.forEach(jsonProperty -> {
			if (!applicationConfiguration.getExcludeDocPropertiesForKafka().contains(jsonProperty.getPropertyName()))
				result.add(jsonProperty);
		});
		jsonNode.putPOJO("documentProperties", result);
	}

	private void addRequestDetails(ObjectNode jsonNode) {
		List<JsonProperty> list = new ArrayList<>();
		request.getHeaderNames().asIterator().forEachRemaining(name -> {
			if (!applicationConfiguration.getExcludeHeadersForKafka().contains(name))
				list.add(new JsonProperty(name, request.getHeader(name)));
		});
		jsonNode.putPOJO("requestProperties", list);
	}

	private P8ContentObject createP8ContentObject(String resourceType) {
		P8ContentObject p8ContentObject = new P8ContentObject();
		p8ContentObject.setDocumentClass(resourceType);
		return p8ContentObject;
	}

	private P8Object findByFilter(JsonResource<JsonDocumentWithSearchFilter> jsonResource) throws FilenetPropMappingNotFoundException, ServiceException {
		if (CollectionUtils.isEmpty(jsonResource.getBody().getSearchFilter()))
			throw new CommonsErrorMessageServiceException("ecmdocmgmt.validation.nofilter");
		if (jsonResource.getBody().getSearchFilter().size() == 1) {
			JsonProperty jsonProperty = jsonResource.getBody().getSearchFilter().get(0);
			String name = p8PropMappingService.getFilenetNameMappingFromDb(jsonResource.getAppId(),
					jsonProperty.getPropertyName());
			if ("id".equalsIgnoreCase(name) || name == null && "id".equalsIgnoreCase(jsonProperty.getPropertyName())) {
				GetDocumentByIdRequest request = new GetDocumentByIdRequest();
				request.setDocumentId(jsonProperty.getPropertyValue().toString());
				GetDocumentByIdResponse response = documentService.getDocumentMetadataById(request);
				if (!response.success())
					throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
				return response.getP8ContentObject();
			}
		}
		JsonResource<JsonSearch> jsonSearchResource = new JsonResource<>();
		JsonSearch jsonSearch = new JsonSearch();
		jsonSearch.setPropertiesToRetrieve(Arrays.asList("Id"));
		jsonSearch.setSearchFilter(jsonResource.getBody().getSearchFilter());
		jsonSearchResource.setBody(jsonSearch);
		jsonSearchResource.setAppId(jsonResource.getAppId());
		jsonSearchResource.setResourceType(jsonResource.getResourceType());
		jsonSearch = mappingService.preprocessSearchResource(jsonSearchResource);
		SearchRequest request = new SearchRequest();
		request.getSearchData().setJsonSearch(jsonSearch);
		int maxResults = (jsonSearch.isIncludeContent() ? applicationConfiguration.getMaxResultsWithContent()
				: applicationConfiguration.getMaxResults());
		request.getSearchParameters().setMaxSize(maxResults + 1);
		SearchResponse response = searchService.searchDocuments(request);
		if (!response.success())
			throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
		List<P8Object> p8List = response.getSearchResults();
		if (CollectionUtils.isEmpty(p8List))
			throw new CommonsErrorMessageServiceException("ecmdocmgmt.validation.filternodocumentfound");
		if (p8List.size() != 1)
			throw new CommonsErrorMessageServiceException("ecmdocmgmt.validation.filtermultipleresults", p8List.size());
		return p8List.get(0);
	}

	public List<JsonDocument> searchDocuments(JsonResource<JsonSearch> jsonSearchResource) throws FilenetPropMappingNotFoundException, ServiceException, IOException  {
		JsonSearch jsonSearch = mappingService.preprocessSearchResource(jsonSearchResource);
		SearchRequest request = new SearchRequest();
		request.getSearchData().setJsonSearch(jsonSearch);
		int maxResults = (jsonSearch.isIncludeContent() ? applicationConfiguration.getMaxResultsWithContent()
				: applicationConfiguration.getMaxResults());
		request.getSearchParameters().setMaxSize(maxResults + 1);
		SearchResponse response = searchService.searchDocuments(request);
		if (!response.success())
			throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
		List<P8Object> list = response.getSearchResults();

		if (!CollectionUtils.isEmpty(list) && list.size() >= maxResults)
			throw new CommonsErrorMessageServiceException("ecmdocmgmt.validation.toomuchresults", maxResults);

		List<JsonDocument> result = new ArrayList<>();

		for (P8Object p8Object : response.getSearchResults())
			result.add(mappingService.convertToJsonDocument(jsonSearchResource.getAppId(), p8Object,
					jsonSearch.isIncludeMappedPropertiesOnly()));
		return result;
	}

	public void updateDocument(JsonResource<JsonDocumentWithSearchFilter> jsonResource,
			WebServiceRequestAuditEntry auditEntry) throws Exception {
		if (jsonResource.getBody().getContent() != null)
			throw new CommonsErrorMessageServiceException("ecmdocmgmt.validation.contentisnotallowed");
		P8Object p8Object = findByFilter(jsonResource);
		UpdateDocumentMetadataRequest request = new UpdateDocumentMetadataRequest();
		mappingService.convertJsonDocument(jsonResource.getBody(), jsonResource.getAppId(), request, true);
		request.setId(p8Object.getId());
		auditEntry.setObjectId(p8Object.getId());
		UpdateDocumentMetadataResponse response = documentService.updateDocumentMetadata(request);
		if (!response.success())
			throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
		FilenetOperationAuditLog entry = p8ContentLibConverter.convert(response).get(0);
		entry.setAppId(ApplicationConfiguration.APPLICATION_NAME);
		entry.setAppUser(jsonResource.getAppUser());
		entry.setAppEvent(DocumentManagementController.UPDATE_DOCUMENT);
		try {
			p8AuditRepository.save(entry);
		} catch (Exception e) {
			log.error(SAVE_P8AUDIT_ERROR, entry, e);
		}
	}

	public void createDocumentVersion(JsonResource<JsonDocumentWithSearchFilter> jsonResource,
			WebServiceRequestAuditEntry auditEntry) throws Exception {
		if (jsonResource.getBody().getContent() == null)
			throw new CommonsErrorMessageServiceException("ecmdocmgmt.validation.contentrequired");
		P8Object p8Object = findByFilter(jsonResource);

		CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
		P8ContentObject p8ContentObject = new P8ContentObject();
		request.setP8ContentObject(p8ContentObject);
		request.setP8ObjectId(p8Object.getId());
		mappingService.convertJsonDocument(jsonResource.getBody(), jsonResource.getAppId(), request, true);
		CreateDocumentVersionResponse response = documentService.createDocumentVersion(request);
		if (!response.success())
			throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
		auditEntry.setObjectId(response.getP8DocumentId());
		FilenetOperationAuditLog entry = p8ContentLibConverter.convert(response);
		entry.setAppId(ApplicationConfiguration.APPLICATION_NAME);
		entry.setAppUser(jsonResource.getAppUser());
		entry.setAppEvent(DocumentManagementController.CREATE_VESRION);
		try {
			p8AuditRepository.save(entry);
		} catch (Exception e) {
			log.error(SAVE_P8AUDIT_ERROR, entry, e);
		}
	}

	private P8ContentResource convertToPdf(P8ContentResource p8ContentResource) throws Exception {
		p8ContentResource.setResourceObject(
				pdfService.convertToPDFStream(p8ContentResource.getInputStream(), p8ContentResource.getContentType()));
		if (StringUtils.hasText(p8ContentResource.getFileName()))
			p8ContentResource.setFileName(p8ContentResource.getFileName() + ".pdf");
		else
			p8ContentResource.setFileName("resource.pdf");
		p8ContentResource.setContentType("application/pdf");
		return p8ContentResource;
	}

	public P8ContentResource getContent(String guid, String converter) throws Exception {
		GetContentRequest request = new GetContentRequest();
		request.setDocumentId(guid);
		GetContentResponse response = contentService.getContent(request);
		if (response.getErrorType() == ExceptionConstants.NOT_FOUND_ERROR_TYPE)
			return null;
		if (!response.success())
			throw new ServiceException(response.getErrorCode(), response.getErrorMessage());
		P8ContentResource p8ContentResource = response.getP8DocumentResource();
		String contentYpe = p8ContentResource.getContentType();
		if (StringUtils.hasText(converter) && StringUtils.hasText(contentYpe)) {
			switch (converter.toLowerCase()) {
			case ImageConversionService.TIFF_TO_PDF:
				if (contentYpe.toLowerCase().contains("image/tif"))
					return convertToPdf(p8ContentResource);
				break;
			case ImageConversionService.IMAGE_TO_PDF:
				if (contentYpe.toLowerCase().startsWith("image/"))
					return convertToPdf(p8ContentResource);
				break;
			default:
				break;
			}
		}
		return p8ContentResource;
	}

}
