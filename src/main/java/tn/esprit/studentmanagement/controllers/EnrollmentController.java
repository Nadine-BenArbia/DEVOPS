package tn.esprit.studentmanagement.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.studentmanagement.entities.Enrollment;
import tn.esprit.studentmanagement.services.IEnrollment;

import java.util.List;

@RestController
@RequestMapping("/Enrollment")
@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
@SuppressWarnings("java:S4684") // suppress Sonar warning (no DTO refactor)
public class EnrollmentController {

    private final IEnrollment enrollmentService;

    @GetMapping("/getAllEnrollment")
    public ResponseEntity<List<Enrollment>> getAllEnrollment() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }

    @GetMapping("/getEnrollment/{id}")
    public ResponseEntity<Enrollment> getEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(id));
    }

    @PostMapping("/createEnrollment")
    public ResponseEntity<Enrollment> createEnrollment(@RequestBody Enrollment enrollment) {
        return ResponseEntity.ok(enrollmentService.saveEnrollment(enrollment));
    }

    @PutMapping("/updateEnrollment")
    public ResponseEntity<Enrollment> updateEnrollment(@RequestBody Enrollment enrollment) {
        return ResponseEntity.ok(enrollmentService.saveEnrollment(enrollment));
    }

    @DeleteMapping("/deleteEnrollment/{id}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.noContent().build();
    }
}