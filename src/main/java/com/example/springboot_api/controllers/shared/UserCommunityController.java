package com.example.springboot_api.controllers.shared;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.shared.community.AvailableGroupResponse;
import com.example.springboot_api.dto.shared.community.CommunityPreviewResponse;
import com.example.springboot_api.dto.shared.community.JoinGroupRequest;
import com.example.springboot_api.dto.shared.community.JoinGroupResponse;
import com.example.springboot_api.dto.shared.community.JoinedGroupResponse;
import com.example.springboot_api.dto.shared.community.MembershipStatusResponse;
import com.example.springboot_api.services.shared.UserCommunityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/community")
@RequiredArgsConstructor
public class UserCommunityController {

    private final UserCommunityService service;

    @PostMapping("/join")
    public JoinGroupResponse joinGroup(
            @RequestBody JoinGroupRequest req,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.joinGroup(req, user.getId());
    }

    @GetMapping("/available")
    public PagedResponse<AvailableGroupResponse> getAvailableGroups(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.getAvailableGroups(user.getId(), q, sortBy, sortDir, page, size);
    }

    @GetMapping("/my-groups")
    public PagedResponse<JoinedGroupResponse> getMyGroups(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.getMyGroups(user.getId(), status, q, sortBy, sortDir, page, size);
    }

    @GetMapping("/{notebookId}/preview")
    public CommunityPreviewResponse getCommunityPreview(
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.getCommunityPreview(notebookId);
    }

    @GetMapping("/{notebookId}/membership")
    public MembershipStatusResponse checkMembership(
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.checkMembershipStatus(notebookId, user.getId());
    }
}
