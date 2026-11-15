package com.stockflow.repository;

import com.stockflow.entity.Membership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

    List<Membership> findByUserId(Long userId);

    List<Membership> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);
}