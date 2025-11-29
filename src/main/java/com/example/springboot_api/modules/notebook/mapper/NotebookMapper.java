package com.example.springboot_api.modules.notebook.mapper;

import com.example.springboot_api.modules.notebook.dto.NotebookCreateRequest;
import com.example.springboot_api.modules.notebook.dto.NotebookUpdateRequest;
import com.example.springboot_api.modules.notebook.dto.NotebookResponse;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotebookMapper {

    // createdBy sẽ set trong service, nên ignore ở đây
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Notebook toEntity(NotebookCreateRequest request);

    // Response: map createdBy.id -> createdById
    @Mapping(source = "createdBy.id", target = "createdById")
    NotebookResponse toResponse(Notebook notebook);

    // Update: chỉ set field non-null
    // Bạn có thể dùng @BeanMapping(ignoreByDefault = true) + @Mapping cho từng field nếu muốn strict hơn
    void updateEntityFromRequest(NotebookUpdateRequest request, @MappingTarget Notebook notebook);
}
