package com.example.springboot_api.services;

import com.example.springboot_api.dto.lecturer.StudentExcelData;
import com.example.springboot_api.services.lecturer.ExcelReaderService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderServiceTest {

    private final ExcelReaderService excelReaderService = new ExcelReaderService();

    @Test
    void testReadExcelWithMergedHeader() throws IOException {
        // Tạo file Excel giống file mẫu
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");
        
        // Dòng hành chính
        Row adminRow1 = sheet.createRow(0);
        adminRow1.createCell(0).setCellValue("Trường Đại học ABC");
        
        Row adminRow2 = sheet.createRow(1);
        adminRow2.createCell(0).setCellValue("Danh sách sinh viên");
        
        // Header row
        Row headerRow = sheet.createRow(2);
        headerRow.createCell(0).setCellValue("Mã sinh viên");
        headerRow.createCell(1).setCellValue("Họ và tên");
        headerRow.createCell(3).setCellValue("Ngày sinh");
        
        // Merge cell cho "Họ và tên"
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 1, 2));
        
        // Data rows - họ tên tách cột
        Row dataRow1 = sheet.createRow(3);
        dataRow1.createCell(0).setCellValue("2021001");
        dataRow1.createCell(1).setCellValue("Nguyễn Văn");
        dataRow1.createCell(2).setCellValue("A");
        Cell dateCell1 = dataRow1.createCell(3);
        dateCell1.setCellValue(LocalDate.of(2000, 1, 1));
        
        Row dataRow2 = sheet.createRow(4);
        dataRow2.createCell(0).setCellValue("2021002");
        dataRow2.createCell(1).setCellValue("Trần Thị");
        dataRow2.createCell(2).setCellValue("B");
        Cell dateCell2 = dataRow2.createCell(3);
        dateCell2.setCellValue(LocalDate.of(2000, 2, 2));
        
        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            outputStream.toByteArray()
        );
        
        // Test
        List<StudentExcelData> students = excelReaderService.readStudentDataFromExcel(file);
        
        // Assertions
        assertEquals(2, students.size());
        
        StudentExcelData student1 = students.get(0);
        assertEquals("2021001", student1.getStudentCode());
        assertEquals("Nguyễn Văn A", student1.getFullName());
        assertEquals("Nguyễn Văn", student1.getLastName());
        assertEquals("A", student1.getFirstName());
        
        StudentExcelData student2 = students.get(1);
        assertEquals("2021002", student2.getStudentCode());
        assertEquals("Trần Thị B", student2.getFullName());
        assertEquals("Trần Thị", student2.getLastName());
        assertEquals("B", student2.getFirstName());
    }
    
    @Test
    void testReadExcelWithSeparateNameColumns() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("MSSV");
        headerRow.createCell(1).setCellValue("Họ");
        headerRow.createCell(2).setCellValue("Tên");
        headerRow.createCell(3).setCellValue("Ngày sinh");
        
        // Data row
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("2021001");
        dataRow.createCell(1).setCellValue("Nguyễn Văn");
        dataRow.createCell(2).setCellValue("A");
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            outputStream.toByteArray()
        );
        
        List<StudentExcelData> students = excelReaderService.readStudentDataFromExcel(file);
        
        assertEquals(1, students.size());
        StudentExcelData student = students.get(0);
        assertEquals("2021001", student.getStudentCode());
        assertEquals("Nguyễn Văn A", student.getFullName());
        assertEquals("Nguyễn Văn", student.getLastName());
        assertEquals("A", student.getFirstName());
    }
}