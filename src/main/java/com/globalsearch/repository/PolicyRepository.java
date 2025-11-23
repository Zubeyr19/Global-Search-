package com.globalsearch.repository;

import com.globalsearch.entity.Policy;
import com.globalsearch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByTenantIdAndActive(String tenantId, Boolean active);
    List<Policy> findByRoleAndActive(User.Role role, Boolean active);
    List<Policy> findByTenantId(String tenantId);
    Optional<Policy> findByIdAndTenantId(Long id, String tenantId);
}
