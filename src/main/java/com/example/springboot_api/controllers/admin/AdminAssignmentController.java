package com.example.springboot_api.controllers.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.assignment.ApproveAssignmentRequest;
import com.example.springboot_api.dto.admin.assignment.AssignmentResponse;
import com.example.springboot_api.dto.admin.assignment.CreateAssignmentRequest;
import com.example.springboot_api.services.admin.AdminAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/teaching-assignments")
@RequiredArgsConstructor
@Tag(name = "Admin Teaching Assignment", description = "Các API dành cho Admin quản lý phân công giảng dạy")
public class AdminAssignmentController {

    private final AdminAssignmentService assignmentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phân công với bộ lọc")
    public ResponseEntity<List<AssignmentResponse>> list(
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(assignmentService.listAssignments(termId, teacherId, status));
    }

    @PostMapping
    @Operation(summary = "Admin phân công giảng dạy trực tiếp (Tự động APPROVED)")
    public ResponseEntity<AssignmentResponse> create(
            @Valid @RequestBody CreateAssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(req));
    }

    @PatchMapping("/{id}/approval")
    @Operation(summary = "Phê duyệt hoặc từ chối yêu cầu phân công từ giảng viên")
    public ResponseEntity<AssignmentResponse> approveOrReject(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveAssignmentRequest req) {
        return ResponseEntity.ok(assignmentService.approveOrReject(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phân công giảng dạy")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
