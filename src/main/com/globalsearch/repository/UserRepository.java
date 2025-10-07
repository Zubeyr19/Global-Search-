package com.globalsearch.repository;

import com.globalsearch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByTenantId(String tenantId);

    Page<User> findByTenantId(String tenantId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.companyId = :companyId")
    List<User> findByTenantIdAndCompanyId(@Param("tenantId") String tenantId,
                                          @Param("companyId") Long companyId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
}
