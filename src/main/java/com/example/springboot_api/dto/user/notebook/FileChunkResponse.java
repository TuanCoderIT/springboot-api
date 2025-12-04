package com.example.springboot_api.dto.user.notebook;

import java.util.UUID;

public record FileChunkResponse(
                UUID id,
                Integer chunkIndex,
                String content) {
}
