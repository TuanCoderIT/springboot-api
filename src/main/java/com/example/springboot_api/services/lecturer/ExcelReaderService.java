package com.example.springboot_api.services.lecturer;

import com.example.springboot_api.dto.lecturer.StudentExcelData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExcelReaderService {
    
    // Patterns để detect header
    private static final Pattern STUDENT_CODE_PATTERN = Pattern.compile("(?i).*(mã\\s*sinh\\s*viên|mssv).*");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("(?i).*(họ\\s*và\\s*tên|họ\\s*tên).*");
    private static final Pattern LAST_NAME_PATTERN = Pattern.compile("(?i)^\\s*họ\\s*$");
    private static final Pattern FIRST_NAME_PATTERN = Pattern.compile("(?i)^\\s*tên\\s*$");
    private static final Pattern DATE_OF_BIRTH_PATTERN = Pattern.compile("(?i).*(ngày\\s*sinh).*");
    
    // Class để lưu thông tin mapping cột
    private static class ColumnMapping {
        int studentCodeCol = -1;
        int dateOfBirthCol = -1;
        int fullNameCol = -1;
        int lastNameCol = -1;
        int firstNameCol = -1;
        NameHandlingMode nameMode = NameHandlingMode.UNKNOWN;
        
        enum NameHandlingMode {
            SEPARATE_COLUMNS,    // "Họ" + "Tên" riêng biệt
            MERGED_HEADER_SPLIT_DATA,  // Header merge nhưng data tách cột
            SINGLE_COLUMN,       // "Họ và tên" trong 1 cột
            UNKNOWN
        }
    }
    
    public List<StudentExcelData> readStudentDataFromExcel(MultipartFile file) throws IOException {
        List<StudentExcelData> students = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 1. Detect header row
            int headerRowIndex = detectHeaderRow(sheet);
            if (headerRowIndex == -1) {
                throw new RuntimeException("Không tìm thấy header row chứa thông tin sinh viên");
            }
            
            log.info("Phát hiện header row tại dòng: {}", headerRowIndex + 1);
            
            // 2. Mapping các cột
            ColumnMapping mapping = mapColumns(sheet, headerRowIndex);
            validateMapping(mapping);
            
            log.info("Column mapping: studentCode={}, dateOfBirth={}, nameMode={}", 
                    mapping.studentCodeCol, mapping.dateOfBirthCol, mapping.nameMode);
            
            // 3. Đọc dữ liệu từ các dòng sau header
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    StudentExcelData student = parseStudentFromRow(row, i + 1, mapping);
                    if (student != null && isValidStudent(student)) {
                        students.add(student);
                    }
                } catch (Exception e) {
                    log.warn("Lỗi đọc dòng {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return students;
    }
    
    /**
     * Detect header row bằng cách tìm dòng chứa ít nhất 2 trong 3 pattern:
     * - Mã sinh viên/MSSV
     * - Họ và tên/Họ/Tên  
     * - Ngày sinh
     */
    private int detectHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) { // Chỉ check 10 dòng đầu
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            int matchCount = 0;
            boolean hasStudentCode = false;
            boolean hasName = false;
            boolean hasDateOfBirth = false;
            
            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue == null || cellValue.trim().isEmpty()) continue;
                
                if (STUDENT_CODE_PATTERN.matcher(cellValue).matches()) {
                    hasStudentCode = true;
                    matchCount++;
                } else if (FULL_NAME_PATTERN.matcher(cellValue).matches() || 
                          LAST_NAME_PATTERN.matcher(cellValue).matches() ||
                          FIRST_NAME_PATTERN.matcher(cellValue).matches()) {
                    hasName = true;
                    matchCount++;
                } else if (DATE_OF_BIRTH_PATTERN.matcher(cellValue).matches()) {
                    hasDateOfBirth = true;
                    matchCount++;
                }
            }
            
            // Cần ít nhất 2 trong 3 pattern để coi là header
            if (matchCount >= 2) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Map các cột dựa trên header và detect cách xử lý họ tên
     */
    private ColumnMapping mapColumns(Sheet sheet, int headerRowIndex) {
        ColumnMapping mapping = new ColumnMapping();
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) return mapping;
        
        // Lấy thông tin merged cells
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        
        // Map các cột cơ bản
        for (Cell cell : headerRow) {
            String cellValue = getCellValueAsString(cell);
            if (cellValue == null || cellValue.trim().isEmpty()) continue;
            
            int colIndex = cell.getColumnIndex();
            
            if (STUDENT_CODE_PATTERN.matcher(cellValue).matches()) {
                mapping.studentCodeCol = colIndex;
            } else if (DATE_OF_BIRTH_PATTERN.matcher(cellValue).matches()) {
                mapping.dateOfBirthCol = colIndex;
            } else if (LAST_NAME_PATTERN.matcher(cellValue).matches()) {
                mapping.lastNameCol = colIndex;
            } else if (FIRST_NAME_PATTERN.matcher(cellValue).matches()) {
                mapping.firstNameCol = colIndex;
            } else if (FULL_NAME_PATTERN.matcher(cellValue).matches()) {
                mapping.fullNameCol = colIndex;
            }
        }
        
        // Determine name handling mode
        if (mapping.lastNameCol != -1 && mapping.firstNameCol != -1) {
            // Case 1: Có "Họ" + "Tên" riêng biệt
            mapping.nameMode = ColumnMapping.NameHandlingMode.SEPARATE_COLUMNS;
        } else if (mapping.fullNameCol != -1) {
            // Case 2 hoặc 3: Có "Họ và tên"
            // Check xem có bị merge không và có dữ liệu tách cột không
            boolean isMerged = isCellMerged(mergedRegions, headerRowIndex, mapping.fullNameCol);
            
            if (isMerged) {
                // Tìm range của merged cell
                CellRangeAddress mergedRange = findMergedRange(mergedRegions, headerRowIndex, mapping.fullNameCol);
                if (mergedRange != null && mergedRange.getLastColumn() > mergedRange.getFirstColumn()) {
                    // Header bị merge, check xem data có tách không
                    if (hasDataInSeparateColumns(sheet, headerRowIndex + 1, 
                            mergedRange.getFirstColumn(), mergedRange.getLastColumn())) {
                        // Case 2: Header merge nhưng data tách
                        mapping.nameMode = ColumnMapping.NameHandlingMode.MERGED_HEADER_SPLIT_DATA;
                        mapping.lastNameCol = mergedRange.getFirstColumn();
                        mapping.firstNameCol = mergedRange.getLastColumn();
                    } else {
                        // Case 3: Thực sự là 1 cột
                        mapping.nameMode = ColumnMapping.NameHandlingMode.SINGLE_COLUMN;
                    }
                }
            } else {
                // Case 3: "Họ và tên" thực sự là 1 cột
                mapping.nameMode = ColumnMapping.NameHandlingMode.SINGLE_COLUMN;
            }
        }
        
        return mapping;
    }
    
    private boolean isCellMerged(List<CellRangeAddress> mergedRegions, int row, int col) {
        return mergedRegions.stream().anyMatch(range -> 
            range.isInRange(row, col));
    }
    
    private CellRangeAddress findMergedRange(List<CellRangeAddress> mergedRegions, int row, int col) {
        return mergedRegions.stream()
            .filter(range -> range.isInRange(row, col))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check xem có dữ liệu trong các cột tách riêng không
     */
    private boolean hasDataInSeparateColumns(Sheet sheet, int startRow, int firstCol, int lastCol) {
        for (int i = startRow; i <= Math.min(startRow + 5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String firstColData = getCellValueAsString(row.getCell(firstCol));
            String lastColData = getCellValueAsString(row.getCell(lastCol));
            
            if (firstColData != null && !firstColData.trim().isEmpty() &&
                lastColData != null && !lastColData.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    private void validateMapping(ColumnMapping mapping) {
        if (mapping.studentCodeCol == -1) {
            throw new RuntimeException("Không tìm thấy cột 'Mã sinh viên' hoặc 'MSSV'");
        }
        
        if (mapping.nameMode == ColumnMapping.NameHandlingMode.UNKNOWN) {
            throw new RuntimeException("Không tìm thấy cột họ tên hợp lệ");
        }
    }
    
    private StudentExcelData parseStudentFromRow(Row row, int rowNumber, ColumnMapping mapping) {
        try {
            // Lấy mã sinh viên
            String studentCode = getCellValueAsString(row.getCell(mapping.studentCodeCol));
            
            // Lấy ngày sinh
            LocalDate dateOfBirth = null;
            if (mapping.dateOfBirthCol != -1) {
                dateOfBirth = getCellValueAsDate(row.getCell(mapping.dateOfBirthCol));
            }
            
            // Xử lý họ tên theo mode
            String fullName = null;
            String firstName = null;
            String lastName = null;
            
            switch (mapping.nameMode) {
                case SEPARATE_COLUMNS:
                    // Case 1: "Họ" + "Tên" riêng biệt
                    lastName = getCellValueAsString(row.getCell(mapping.lastNameCol));
                    firstName = getCellValueAsString(row.getCell(mapping.firstNameCol));
                    if (lastName != null && firstName != null) {
                        fullName = (lastName.trim() + " " + firstName.trim()).trim();
                    }
                    break;
                    
                case MERGED_HEADER_SPLIT_DATA:
                    // Case 2: Header merge nhưng data tách cột
                    lastName = getCellValueAsString(row.getCell(mapping.lastNameCol));
                    firstName = getCellValueAsString(row.getCell(mapping.firstNameCol));
                    if (lastName != null && firstName != null) {
                        fullName = (lastName.trim() + " " + firstName.trim()).trim();
                    }
                    break;
                    
                case SINGLE_COLUMN:
                    // Case 3: "Họ và tên" trong 1 cột
                    fullName = getCellValueAsString(row.getCell(mapping.fullNameCol));
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        // Tách firstName và lastName từ fullName
                        String[] nameParts = fullName.trim().split("\\s+");
                        if (nameParts.length > 0) {
                            firstName = nameParts[nameParts.length - 1]; // Từ cuối cùng
                            if (nameParts.length > 1) {
                                lastName = String.join(" ", Arrays.copyOf(nameParts, nameParts.length - 1));
                            } else {
                                lastName = "";
                            }
                        }
                    }
                    break;
            }
            
            return StudentExcelData.builder()
                    .studentCode(studentCode)
                    .fullName(fullName)
                    .firstName(firstName)
                    .lastName(lastName)
                    .dateOfBirth(dateOfBirth)
                    .rowNumber(rowNumber)
                    .build();
                    
        } catch (Exception e) {
            log.error("Lỗi parse dòng {}: {}", rowNumber, e.getMessage());
            return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Nếu là số nguyên, không hiển thị .0
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return getCellValueAsString(cell.getCachedFormulaResultType(), cell);
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
    
    private String getCellValueAsString(CellType cellType, Cell cell) {
        switch (cellType) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
    
    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            log.warn("Không thể parse ngày từ cell: {}", e.getMessage());
        }
        
        return null;
    }
    
    private boolean isValidStudent(StudentExcelData student) {
        // Validate mã sinh viên: không rỗng và không chứa chữ cái (chỉ số)
        if (student.getStudentCode() == null || student.getStudentCode().trim().isEmpty()) {
            return false;
        }
        
        String studentCode = student.getStudentCode().trim();
        // Check xem có phải là số không (có thể có ký tự đặc biệt như dấu chấm, gạch ngang)
        if (!studentCode.matches("^[0-9A-Za-z\\-\\.]+$")) {
            return false;
        }
        
        // Validate họ tên: không rỗng
        if (student.getFullName() == null || student.getFullName().trim().isEmpty()) {
            return false;
        }
        
        // Chuẩn hóa dữ liệu
        student.setStudentCode(studentCode);
        student.setFullName(normalizeString(student.getFullName()));
        
        return true;
    }
    
    /**
     * Chuẩn hóa string: trim và gộp nhiều space thành 1
     */
    private String normalizeString(String str) {
        if (str == null) return null;
        return str.trim().replaceAll("\\s+", " ");
    }
}