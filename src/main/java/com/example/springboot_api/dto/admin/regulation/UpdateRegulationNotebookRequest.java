package com.example.springboot_api.dto.admin.regulation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request để cập nhật thông tin regulation notebook.
 */
@Data
public class UpdateRegulationNotebookRequest {

    @NotBlank(message = "Title không được để trống")
    @Size(max = 255, message = "Title không được quá 255 ký tự")
    private String title;

    @Size(max = 1000, message = "Description không được quá 1000 ký tự")
    private String description;
}
