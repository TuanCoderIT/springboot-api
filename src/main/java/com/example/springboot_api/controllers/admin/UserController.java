package com.example.springboot_api.controllers.admin;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.user.CreateUserRequest;
import com.example.springboot_api.dto.admin.user.ListUserRequest;
import com.example.springboot_api.dto.admin.user.UpdateUserRequest;
import com.example.springboot_api.dto.admin.user.UserResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping
  public PagedResponse<UserResponse> list(@ParameterObject @ModelAttribute ListUserRequest req) {
    return userService.list(req);
  }

  @GetMapping("/{id}")
  public UserResponse getOne(@PathVariable UUID id) {
    return userService.getOne(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
    return userService.create(req);
  }

  @PutMapping("/{id}")
  public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
    return userService.update(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    userService.delete(id);
  }

}
