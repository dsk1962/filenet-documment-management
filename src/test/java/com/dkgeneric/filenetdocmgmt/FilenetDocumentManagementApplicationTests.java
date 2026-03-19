package com.dkgeneric.filenetdocmgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.dkgeneric.commons.config.GitInformations;
import com.dkgeneric.filenet.content.common.ServiceException;
import com.dkgeneric.filenet.content.model.P8ResultSet;
import com.dkgeneric.filenet.content.model.SearchParameters;
import com.dkgeneric.filenet.content.provider.P8ProviderImpl;
import com.dkgeneric.filenet.content.request.BaseRequest;
import com.dkgeneric.filenet.content.response.BaseResponse;
import com.dkgeneric.filenet.content.service.AuthService;
import com.filenet.api.core.Document;
import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.exception.ExceptionCode;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@TestPropertySource(locations = "classpath:/application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(Lifecycle.PER_CLASS)
@EnableEncryptableProperties
@AutoConfigureMockMvc
@Slf4j
class FilenetDocumentManagementApplicationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private GitInformations gitInformations;
	@Autowired
	AuthService authService;

	// instance of P8ProviderImpl to perform "utility" tasks (like getObject...)
	P8ProviderImpl p8ProviderImpl;
	static final Level LIB_LOG_LEVEL = Level.DEBUG;
	static final String NON_NULL_RESPONSE_CODE_ASSERTMESSAGE = "Non-null error code returned. Error message: ";

	@Getter
	@Setter
	@Value("${jsonSearchText}")
	private String jsonSearchText;
	@Getter
	@Setter
	@Value("${jsonCreateText}")
	private String jsonCreateText;
	@Getter
	@Setter
	@Value("${jsonUpdateText}")
	private String jsonUpdateText;

	@Test
	void contextLoads() {
		assertThat(mockMvc).isNotNull();
	}

	@Test
	@Order(1)
	void createTest() throws Exception {
		log.info("Create test started.");
		mockMvc.perform(post("/documents/document").contentType(MediaType.APPLICATION_JSON).content(jsonCreateText))
				.andExpect(status().isCreated()).andReturn();
		log.info("Create test completed.");
	}

	@Test
	@Order(2)
	void searchTest() throws Exception {

		log.info("Search test started.");
		mockMvc.perform(post("/documents/document/search").contentType(MediaType.APPLICATION_JSON).content(jsonSearchText))
				.andExpect(jsonPath("$", hasSize(1))).andExpect(status().isOk()).andReturn();
		log.info("Search test completed.");
	}

	@Test
	@Order(3)
	void updateTest() throws Exception {

		log.info("Update test started.");
		mockMvc.perform(put("/documents/document")
				.contentType(MediaType.APPLICATION_JSON).content(jsonUpdateText)).andExpect(status().isOk())
				.andReturn();
		log.info("Update test completed.");
	}

	@BeforeAll
	void initialize() throws Exception {
		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.dkgeneric.audit.content"))
				.setLevel(LIB_LOG_LEVEL);
		log.info("Test started.");
		log.info("Initialization code started.");
		log.info(gitInformations.printAllGitInfo());

		p8ProviderImpl = authService.createConnection(new BaseRequest());

		// delete documents created for testing purposes
		BaseResponse response = cleanTestDocuments();
		assertNull(response.getErrorCode(), NON_NULL_RESPONSE_CODE_ASSERTMESSAGE + response.getErrorMessage());
	}

	@AfterAll
	void cleanUp() throws ServiceException {
		log.info("Cleanup code started.");
		// delete documents created for testing purposes
		BaseResponse response = cleanTestDocuments();
		assertNull(response.getErrorCode(), NON_NULL_RESPONSE_CODE_ASSERTMESSAGE + response.getErrorMessage());
		try {
			if (null != p8ProviderImpl)
				p8ProviderImpl.close();
		} catch (Exception e) {
			// ignore
		}
		log.info("Test completed.");
	}

	BaseResponse cleanTestDocuments() throws ServiceException {
		BaseResponse response = new BaseResponse();
		String query = "select Id from FilenetLibTest WHERE DocumentTitle LIKE 'FilenetLibTest%'";
		P8ResultSet p8ResultSet = p8ProviderImpl.searchDocuments(query, new SearchParameters());
		if (p8ResultSet.getSearchResults().size() > 10)
			throw new ServiceException("Too much documnets to delete. Clean up repository manually. Query : " + query);
		for (IndependentlyPersistableObject independentlyPersistableObject : p8ResultSet.getSearchResults()) {
			try {
				p8ProviderImpl.deleteDocument((Document) independentlyPersistableObject);
			} catch (EngineRuntimeException e) {
				ExceptionCode exceptionCode = e.getExceptionCode();
				if (exceptionCode != ExceptionCode.E_OBJECT_NOT_FOUND)
					throw e;
			}
		}
		log.info("{} test document(s) deleted.", p8ResultSet.getSearchResults().size());
		return response;
	}
}
