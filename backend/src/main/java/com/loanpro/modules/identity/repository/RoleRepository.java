package com.loanpro.modules.identity.repository;

import com.loanpro.modules.identity.domain.Role;
import com.loanpro.modules.identity.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
    boolean existsByName(RoleName name);
}
