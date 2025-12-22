package com.example.springboot_api.dto.admin.subject;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request cập nhật Subject.
 */
@Data
public class UpdateSubjectRequest {

    @Size(max = 50, message = "Mã môn học không được quá 50 ký tự")
    private String code;

    @Size(max = 255, message = "Tên môn học không được quá 255 ký tự")
    private String name;

    private Integer credit;

    private Boolean isActive;

    /**
     * Danh sách ngành học để cập nhật (replace toàn bộ MajorSubject).
     * Nếu null, giữ nguyên. Nếu rỗng [], xóa hết liên kết.
     */
    private List<MajorAssignment> majorAssignments;
}
