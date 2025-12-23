package com.example.springboot_api.dto.lecturer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExcelData {
    private String studentCode;
    private String fullName;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private int rowNumber;
}