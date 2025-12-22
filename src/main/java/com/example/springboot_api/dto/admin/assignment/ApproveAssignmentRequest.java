package com.example.springboot_api.dto.admin.assignment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApproveAssignmentRequest {
    @NotNull(message = "Trạng thái phê duyệt không được để trống")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Trạng thái phải là APPROVED hoặc REJECTED")
    private String status;

    private String note;
}
