package com.globalsearch.service;

import com.globalsearch.dto.request.PolicyRequest;
import com.globalsearch.dto.response.PolicyResponse;
import com.globalsearch.entity.Policy;
import com.globalsearch.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {
    private final PolicyRepository policyRepository;

    public PolicyResponse createPolicy(PolicyRequest request, String tenantId) {
        Policy policy = new Policy();
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setTenantId(tenantId);
        policy.setRole(request.getRole());
        policy.setRules(request.getRules());
        policy.setActive(request.getActive());

        Policy saved = policyRepository.save(policy);
        return mapToResponse(saved);
    }

    public List<PolicyResponse> getAllPolicies(String tenantId, boolean includeInactive) {
        List<Policy> policies = includeInactive
                ? policyRepository.findByTenantId(tenantId)
                : policyRepository.findByTenantIdAndActive(tenantId, true);

        return policies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PolicyResponse getPolicyById(Long id, String tenantId) {
        Policy policy = policyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        return mapToResponse(policy);
    }

    public PolicyResponse updatePolicy(Long id, PolicyRequest request, String tenantId) {
        Policy policy = policyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));

        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setRole(request.getRole());
        policy.setRules(request.getRules());
        policy.setActive(request.getActive());

        Policy updated = policyRepository.save(policy);
        return mapToResponse(updated);
    }

    public void deletePolicy(Long id, String tenantId) {
        Policy policy = policyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        policyRepository.delete(policy);
    }

    private PolicyResponse mapToResponse(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .tenantId(policy.getTenantId())
                .role(policy.getRole())
                .rules(policy.getRules())
                .active(policy.getActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
