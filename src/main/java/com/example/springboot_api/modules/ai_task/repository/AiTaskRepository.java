package com.example.springboot_api.modules.ai_task.repository;

import com.example.springboot_api.modules.ai_task.entity.AiTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiTaskRepository extends JpaRepository<AiTask, UUID> {

}