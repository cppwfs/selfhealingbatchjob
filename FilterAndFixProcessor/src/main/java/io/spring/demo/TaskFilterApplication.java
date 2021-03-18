package io.spring.demo;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class TaskFilterApplication {

	@Autowired
	private DataSource dataSource;

	public static void main(String[] args) {
		SpringApplication.run(TaskFilterApplication.class, args);
	}

	@Bean
	public Function<String, String> transform() {
		return  (payload) -> {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = null;
			try {
				jsonNode = objectMapper.readTree(payload);
			}
			catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
			String taskName = jsonNode.get("taskName").asText();
			String result = null;
			if(payload.contains("\"exitCode\":2")) {
				JdbcTemplate template = new JdbcTemplate(dataSource);
				template.execute("truncate table task_status");
				result = "{\"name\":\""+ taskName + "\"}";
			}
			return result;
		};
	}
}
