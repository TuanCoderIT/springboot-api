package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.LlmModel;

@Repository
public interface LlmModelRepository extends JpaRepository<LlmModel, UUID> {
    
    /**
     * Lấy danh sách các model đang active
     */
    List<LlmModel> findByIsActiveTrueOrderByIsDefaultDescDisplayNameAsc();
    
    /**
     * Lấy model mặc định
     */
    @Query("""
            SELECT lm FROM Llm_Model lm
            WHERE lm.isDefault = true
            AND lm.isActive = true
            """)
    LlmModel findDefaultModel();
    
    /**
     * Lấy model theo code
     */
    LlmModel findByCodeAndIsActiveTrue(String code);
}

