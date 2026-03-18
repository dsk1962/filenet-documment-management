package com.dkgeneric.filenetdocmgmt.controller;

import com.dkgeneric.audit.controller.AuditBaseController;
import com.dkgeneric.commons.service.FilenetPropMappingService;
import com.dkgeneric.commons.service.ValidationService;
import com.dkgeneric.filenetdocmgmt.configuration.ApplicationConfiguration;

public class ECMBaseController extends AuditBaseController {

	public ECMBaseController(ApplicationConfiguration applicationConfiguration,
			FilenetPropMappingService p8PropMappingService, ValidationService validationService) {
		this.applicationConfiguration = applicationConfiguration;
		this.p8PropMappingService = p8PropMappingService;
		this.validationService = validationService;
	}

	protected ApplicationConfiguration applicationConfiguration;
	protected FilenetPropMappingService p8PropMappingService;
	protected ValidationService validationService;

	protected String getServiceAccount() {
		return request.getHeader("username");
	}

}
