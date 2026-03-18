package com.dkgeneric.filenetdocmgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@SpringBootApplication(scanBasePackages = { "com.dkgeneric.*" }, exclude = { DataSourceAutoConfiguration.class })
@EnableEncryptableProperties
@EnableCaching
@EnableTransactionManagement
public class FilenetDocumentManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilenetDocumentManagementApplication.class, args);
	}

}
