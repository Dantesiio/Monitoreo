package com.futurex.services.FutureXCourseCatalog;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@RestController
public class CatalogController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    @Value("${course.service.url}")
    private String courseServiceUrl;

    private final RestTemplate restTemplate;
    private final Timer catalogRequestTimer;
    private final Timer courseServiceTimer;

    public CatalogController(RestTemplate restTemplate, MeterRegistry registry) {
        this.restTemplate = restTemplate;

        // Registrar métricas para el tiempo de respuesta
        this.catalogRequestTimer = Timer.builder("catalog_request_seconds")
                .tag("endpoint", "/catalog")
                .description("Time taken to process catalog requests")
                .register(registry);

        this.courseServiceTimer = Timer.builder("course_service_request_seconds")
                .description("Time taken for course service requests")
                .register(registry);
    }

    @RequestMapping("/")
    public String getCatalogHome() {
        logger.info("Received request for catalog home page");
        try {
            logger.debug("Making request to course service at: {}", courseServiceUrl);

            long startTime = System.nanoTime();
            String courseAppMessage = restTemplate.getForObject(courseServiceUrl, String.class);
            long duration = System.nanoTime() - startTime;

            courseServiceTimer.record(duration, TimeUnit.NANOSECONDS);

            logger.info("Successfully retrieved data from course service");
            return "Welcome to FutureX Course Catalog " + courseAppMessage;
        } catch (Exception e) {
            logger.error("Error connecting to course service: {}", e.getMessage(), e);
            return "Welcome to FutureX Course Catalog. Course service is currently unavailable.";
        }
    }

    @RequestMapping("/catalog")
    public String getCatalog() {
        logger.info("Received request for /catalog endpoint");

        return catalogRequestTimer.record(() -> {
            try {
                logger.debug("Making request to course service for courses at: {}/courses", courseServiceUrl);
                String courses = restTemplate.getForObject(courseServiceUrl + "/courses", String.class);
                logger.info("Successfully retrieved course list from course service");
                return "Our courses are " + courses;
            } catch (Exception e) {
                logger.error("Error retrieving courses from course service: {}", e.getMessage(), e);
                return "Error retrieving course catalog. Please try again later.";
            }
        });
    }

    @RequestMapping("/firstcourse")
    public String getSpecificCourse() {
        logger.info("Received request for first course");
        try {
            logger.debug("Making request to course service for first course at: {}/1", courseServiceUrl);
            Course course = restTemplate.getForObject(courseServiceUrl + "/1", Course.class);

            if (course != null) {
                logger.info("Successfully retrieved course: {}", course.getCoursename());
                return "Our first course is " + course.getCoursename();
            } else {
                logger.warn("Course with ID 1 not found");
                return "First course not found";
            }
        } catch (Exception e) {
            logger.error("Error retrieving first course: {}", e.getMessage(), e);
            return "Error retrieving first course. Please try again later.";
        }
    }

    // Agregar endpoint para generar un error (para probar logs de error)
    @RequestMapping("/error-test")
    public String generateError() {
        logger.info("Generating a test error log");
        try {
            // Provocar una excepción deliberadamente
            throw new RuntimeException("This is a test error for observability");
        } catch (Exception e) {
            logger.error("Test error generated: {}", e.getMessage(), e);
            return "Error log generated successfully";
        }
    }
}