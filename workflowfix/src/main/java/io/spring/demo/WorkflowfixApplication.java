package io.spring.demo;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableTask
public class WorkflowfixApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkflowfixApplication.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(DataSource dataSource) {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) throws Exception {
				JdbcTemplate template = new JdbcTemplate(dataSource);
				template.execute("truncate table task_status");
			}
		};
	}

}
