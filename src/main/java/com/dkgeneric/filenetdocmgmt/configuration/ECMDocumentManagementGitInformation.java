package com.dkgeneric.filenetdocmgmt.configuration;

import org.springframework.stereotype.Component;

import com.dkgeneric.commons.config.CommonsLibGitInformation;

@Component
public class ECMDocumentManagementGitInformation extends CommonsLibGitInformation {
	@Override
	public String getProjectName() {
		return "ECMDocumentManagement";
	}
}
