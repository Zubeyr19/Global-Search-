package com.globalsearch.controller;

import com.globalsearch.dto.request.PolicyRequest;
import com.globalsearch.dto.response.PolicyResponse;
import com.globalsearch.entity.User;
import com.globalsearch.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {
    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody PolicyRequest request,
            @AuthenticationPrincipal User user) {

        PolicyResponse response = policyService.createPolicy(request, user.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal User user) {

        List<PolicyResponse> policies = policyService.getAllPolicies(user.getTenantId(), includeInactive);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PolicyResponse> getPolicyById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        PolicyResponse policy = policyService.getPolicyById(id, user.getTenantId());
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request,
            @AuthenticationPrincipal User user) {

        PolicyResponse updated = policyService.updatePolicy(id, request, user.getTenantId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        policyService.deletePolicy(id, user.getTenantId());
        return ResponseEntity.noContent().build();
    }
}

