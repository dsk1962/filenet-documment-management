package com.dkgeneric.filenetdocmgmt.controller;

import com.davita.ecm.p8.audit.controller.AuditBaseController;
import com.dkgeneric.commons.service.P8PropMappingService;
import com.dkgeneric.commons.service.ValidationService;
import com.dkgeneric.filenetdocmgmt.configuration.ApplicationConfiguration;

public class ECMBaseController extends AuditBaseController {

	public ECMBaseController(ApplicationConfiguration applicationConfiguration,
			P8PropMappingService p8PropMappingService, ValidationService validationService) {
		this.applicationConfiguration = applicationConfiguration;
		this.p8PropMappingService = p8PropMappingService;
		this.validationService = validationService;
	}

	protected ApplicationConfiguration applicationConfiguration;
	protected P8PropMappingService p8PropMappingService;
	protected ValidationService validationService;

	protected String getServiceAccount() {
		return request.getHeader("username");
	}

}
