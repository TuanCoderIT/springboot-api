package com.example.springboot_api.common.security;

import java.util.UUID;

public interface CurrentUserProvider {

    UUID getCurrentUserId();
}
