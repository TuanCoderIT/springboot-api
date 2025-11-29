package com.example.springboot_api.services.admin;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ConflictException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.user.CreateUserRequest;
import com.example.springboot_api.dto.admin.user.ListUserRequest;
import com.example.springboot_api.dto.admin.user.UpdateUserRequest;
import com.example.springboot_api.dto.admin.user.UserResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepo;
  private final BCryptPasswordEncoder encoder;

  private UserResponse map(User u) {
    UserResponse r = new UserResponse();
    r.setId(u.getId());
    r.setFullName(u.getFullName());
    r.setEmail(u.getEmail());
    r.setRole(u.getRole());
    r.setAvatarUrl(u.getAvatarUrl());
    r.setCreatedAt(u.getCreatedAt());
    return r;
  }

  public PagedResponse<UserResponse> list(ListUserRequest req) {
    String sortBy = Optional.ofNullable(req.getSortBy()).orElse("createdAt");
    String sortDir = Optional.ofNullable(req.getSortDir()).orElse("desc");

    Sort sort = sortDir.equalsIgnoreCase("asc")
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

    Page<User> result = userRepo.allUserPage(req.getQ(), req.getRole(), pageable);

    return new PagedResponse<>(
        result.map(this::map).getContent(),
        new PagedResponse.Meta(
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()));
  }

  public UserResponse getOne(UUID id) {
    return userRepo.findById(id)
        .map(this::map)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
  }

  public UserResponse create(CreateUserRequest req) {
    if (userRepo.findByEmail(req.getEmail()).isPresent()) {
      throw new ConflictException("Email đã tồn tại");
    }

    User u = new User()
        .setEmail(req.getEmail())
        .setFullName(req.getFullName())
        .setRole("STUDENT")
        .setPasswordHash(encoder.encode(req.getPassword()))
        .setCreatedAt(java.time.Instant.now())
        .setUpdatedAt(java.time.Instant.now());

    userRepo.save(u);
    return map(u);
  }

  public UserResponse update(UUID id, UpdateUserRequest req) {
    User u = userRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

    if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())) {
      if (userRepo.findByEmail(req.getEmail()).isPresent()) {
        throw new ConflictException("Email đã tồn tại");
      }
      u.setEmail(req.getEmail());
    }

    if (req.getFullName() != null) {
      u.setFullName(req.getFullName());
    }

    u.setUpdatedAt(java.time.Instant.now());

    userRepo.save(u);
    return map(u);
  }

  public void delete(UUID id) {
    User u = userRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

    if ("admin".equalsIgnoreCase(u.getRole())) {
      throw new BadRequestException("Không thể xóa người dùng admin");
    }

    userRepo.deleteById(id);
  }
}
