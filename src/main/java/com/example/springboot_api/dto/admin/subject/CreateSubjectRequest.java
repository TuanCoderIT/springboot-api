package com.example.springboot_api.dto.admin.subject;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request tạo Subject mới.
 */
@Data
public class CreateSubjectRequest {

    @NotBlank(message = "Mã môn học không được để trống")
    @Size(max = 50, message = "Mã môn học không được quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 255, message = "Tên môn học không được quá 255 ký tự")
    private String name;

    private Integer credit;

    private Boolean isActive = true;

    /**
     * Danh sách ngành học để gán môn vào (tạo MajorSubject).
     * Nếu rỗng hoặc null, môn sẽ không thuộc ngành nào.
     */
    private List<MajorAssignment> majorAssignments;
}
