package com.dkgeneric.filenetdocmgmt.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.dkgeneric.commons.common.ApplicationValue;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@Getter
@Setter
@NoArgsConstructor
public class ApplicationConfiguration {

	public static final String APPLICATION_NAME = "ECM Document Management";

	@ApplicationValue(key = "maxResults")
	private int maxResults = 100;
	@ApplicationValue(key = "maxResultsWithContent")
	private int maxResultsWithContent = 30;
	@ApplicationValue(key ="kafkaAppIds")
	private String kafkaAppIds;
	@ApplicationValue(key ="kafkaTopic")
	private String kafkaTopic;
	@ApplicationValue(key = "teCorePoolSize")
	private int teCorePoolSize = 5;
	@ApplicationValue(key = "teMaxPoolSize")
	private int teMaxPoolSize = 15;
	@ApplicationValue(key = "teQueueCapacity")
	private int teQueueCapacity = 25;
	@ApplicationValue(key = "excludeHeadersForKafka")
	private List<String> excludeHeadersForKafka = new ArrayList<>();
	@ApplicationValue(key = "excludeDocPropertiesForKafka")
	private List<String> excludeDocPropertiesForKafka = new ArrayList<>();
	
}
