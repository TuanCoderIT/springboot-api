package com.example.springboot_api.services.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.notebook.CreateCommunityNotebookRequest;
import com.example.springboot_api.dto.admin.notebook.MemberPendingResponse;
import com.example.springboot_api.dto.admin.notebook.NotebookAdminResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNotebookService {

    private final NotebookRepository notebookRepo;
    private final NotebookMemberRepository memberRepo;
    private final UserRepository userRepo;

    @Transactional
    public NotebookAdminResponse createCommunity(CreateCommunityNotebookRequest req, UUID adminId) {
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        Notebook nb = Notebook.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .visibility(req.getVisibility())
                .type("community")
                .createdBy(admin)
                .build();

        notebookRepo.save(nb);

        NotebookMember mem = NotebookMember.builder()
                .notebook(nb)
                .user(admin)
                .role("owner")
                .status("approved")
                .build();
        memberRepo.save(mem);

        return map(nb);
    }

    public List<NotebookAdminResponse> list() {
        return notebookRepo.findAll()
                .stream()
                .filter(n -> n.getType().equals("community"))
                .map(this::map)
                .toList();
    }

    public NotebookAdminResponse detail(UUID id) {
        Notebook n = notebookRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Notebook not found"));
        return map(n);
    }

    public List<MemberPendingResponse> pendingMembers(UUID notebookId) {
        return memberRepo.findByNotebookIdAndStatus(notebookId, "pending")
                .stream()
                .map(mem -> {
                    MemberPendingResponse m = new MemberPendingResponse();
                    m.setUserId(mem.getUser().getId());
                    m.setEmail(mem.getUser().getEmail());
                    m.setFullName(mem.getUser().getFullName());
                    m.setStatus(mem.getStatus());
                    m.setRole(mem.getRole());
                    return m;
                })
                .toList();
    }

    @Transactional
    public void approve(UUID notebookId, UUID userId) {
        NotebookMember m = memberRepo.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        m.setStatus("approved");
        memberRepo.save(m);
    }

    @Transactional
    public void reject(UUID notebookId, UUID userId) {
        NotebookMember m = memberRepo.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        m.setStatus("rejected");
        memberRepo.save(m);
    }

    @Transactional
    public void delete(UUID id) {
        notebookRepo.deleteById(id);
    }

    private NotebookAdminResponse map(Notebook n) {
        NotebookAdminResponse r = new NotebookAdminResponse();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setDescription(n.getDescription());
        r.setCreatedBy(n.getCreatedBy().getId());
        r.setVisibility(n.getVisibility());
        r.setType(n.getType());
        r.setCreatedAt(n.getCreatedAt().toInstant());
        return r;
    }
}
