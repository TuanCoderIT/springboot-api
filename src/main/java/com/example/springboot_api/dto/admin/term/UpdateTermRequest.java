package com.example.springboot_api.dto.admin.term;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request cập nhật Term.
 */
@Data
public class UpdateTermRequest {

    @Size(max = 50, message = "Mã học kỳ không được quá 50 ký tự")
    @Pattern(regexp = "^\\d{4}_HK[1-3](_\\d+)?$", message = "Mã học kỳ phải theo format: YYYY_HKx hoặc YYYY_HKx_y (VD: 2024_HK1, 2024_HK2_1)")
    private String code;

    @Size(max = 255, message = "Tên học kỳ không được quá 255 ký tự")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isActive;
}
