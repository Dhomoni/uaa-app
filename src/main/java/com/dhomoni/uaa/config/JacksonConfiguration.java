package com.dhomoni.uaa.config;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

@Configuration
public class JacksonConfiguration {

	/**
	 * Support for Java date and time API.
	 * 
	 * @return the corresponding Jackson module.
	 */
	@Bean
	public JavaTimeModule javaTimeModule() {
		return new JavaTimeModule();
	}

	@Bean
	public Jdk8Module jdk8TimeModule() {
		return new Jdk8Module();
	}

	/*
	 * Support for Hibernate types in Jackson.
	 */
	@Bean
	public Hibernate5Module hibernate5Module() {
		return new Hibernate5Module();
	}

	/*
	 * Jackson Afterburner module to speed up serialization/deserialization.
	 */
	@Bean
	public AfterburnerModule afterburnerModule() {
		return new AfterburnerModule();
	}

	/*
	 * Module for serialization/deserialization of RFC7807 Problem.
	 */
	@Bean
	ProblemModule problemModule() {
		return new ProblemModule();
	}

	/*
	 * Module for serialization/deserialization of ConstraintViolationProblem.
	 */
	@Bean
	ConstraintViolationProblemModule constraintViolationProblemModule() {
		return new ConstraintViolationProblemModule();
	}

	@Bean
	public JtsModule jtsModule() {
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		return new JtsModule(gf);
	}
}
