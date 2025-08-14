package com.brian.file_sharing_app;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class FileSharingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileSharingAppApplication.class, args);
	}

}
