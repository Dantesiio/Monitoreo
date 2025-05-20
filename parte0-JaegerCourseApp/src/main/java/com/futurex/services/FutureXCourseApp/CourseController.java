package com.futurex.services.FutureXCourseApp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
    private final Timer catalogRequestTimer;

    @Autowired
    private CourseRepository courseRepository;

    public CourseController(MeterRegistry registry) {
        this.catalogRequestTimer = Timer.builder("catalog_courses_request_seconds")
                .tag("endpoint", "/catalog")
                .description("Time taken to process catalog requests")
                .register(registry);
    }

    @RequestMapping("/")
    public String getCourseAppHome() {
        logger.info("Home page accessed");
        return ("Course App Home");
    }

    @RequestMapping("/catalog")
    public ResponseEntity<List<Course>> getCatalog() {
        logger.info("Request received for /catalog endpoint");
        try {
            return catalogRequestTimer.record(() -> {
                List<Course> courses = courseRepository.findAll();
                logger.info("Found {} courses in catalog", courses.size());
                return ResponseEntity.ok(courses);
            });
        } catch (Exception e) {
            logger.error("Error processing catalog request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping("/courses")
    public List<Course> getCourses() {
        logger.info("Getting all courses");
        return courseRepository.findAll();
    }

    @RequestMapping("/{id}")
    public ResponseEntity<Course> getSpecificCourse(@PathVariable("id") BigInteger id) {
        logger.info("Request received for course with ID: {}", id);
        try {
            Course course = courseRepository.getOne(id);
            logger.info("Course found: {}", course.getCoursename());
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            logger.error("Error retrieving course with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(method = RequestMethod.POST, value="/courses")
    public ResponseEntity<Void> saveCourse(@RequestBody Course course) {
        logger.info("Saving new course: {}", course.getCoursename());
        try {
            courseRepository.save(course);
            logger.info("Course saved successfully");
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Error saving course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable BigInteger id) {
        logger.info("Deleting course with ID: {}", id);
        try {
            courseRepository.deleteById(id);
            logger.info("Course deleted successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting course with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}