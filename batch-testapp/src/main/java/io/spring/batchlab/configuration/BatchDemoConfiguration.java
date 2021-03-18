/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.spring.batchlab.configuration;

import java.util.List;
import java.util.Map;

import io.spring.batchlab.BatchLabProperties;
import io.spring.batchlab.exceptions.InvalidEntryException;
import io.spring.batchlab.exceptions.NoTableException;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

@EnableBatchProcessing
@Configuration
@EnableTask
public class BatchDemoConfiguration {

	private static final Log logger = LogFactory.getLog(BatchDemoConfiguration.class);
	private static final String STEP_PREFIX = "gen-step-";
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;

	@Bean
	public BatchLabProperties batchLabProperties() {
		return new BatchLabProperties();
	}

	private int exitCode=1;

	@Bean
	public Job steps(BatchLabProperties batchLabProperties) {
		int randId = (int) ((100000) * Math.random());
		String stepOneName = STEP_PREFIX + "ONE_" + batchLabProperties.getBatchName();
		String stepTwoName = STEP_PREFIX + "TWO_" + batchLabProperties.getBatchName();
		String stepThreeName = STEP_PREFIX + "THREE_" + batchLabProperties.getBatchName();

		SimpleJobBuilder jobBuilder = jobBuilderFactory.get(batchLabProperties.getBatchName())
				.start(stepBuilderFactory.get(stepOneName)
						.tasklet(new Tasklet() {
							@Override
							public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

								logger.info("********ONE: Obtaining Records********");
								return RepeatStatus.FINISHED;
							}
						}).build())
				.next(stepBuilderFactory.get(stepTwoName)
						.tasklet(new Tasklet() {
							@Override
							public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
								logger.info("********TWO: Calculating********");
								JdbcTemplate template = new JdbcTemplate(dataSource);
								try {
									List<Map<String, Object>> result = template.queryForList("SELECT issue FROM task_status");

									if (result.size() > 0) {
										exitCode = 2;
										throw new InvalidEntryException("Whoops");
									}
								}
								catch (BadSqlGrammarException bsge) {
									exitCode = 3;
									throw new NoTableException("Whoops no table");
								}
								return RepeatStatus.FINISHED;
							}
						}).build())
				.next(stepBuilderFactory.get(stepThreeName)
						.tasklet(new Tasklet() {
							@Override
							public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
								logger.info("********THREE: Writing Results********");
								return RepeatStatus.FINISHED;
							}
						}).build());
		return jobBuilder.build();
	}

	@Bean
	ExitCodeExceptionMapper exitCodeToexceptionMapper() {
		return exception -> {
			return exitCode;
		};
	}

}
