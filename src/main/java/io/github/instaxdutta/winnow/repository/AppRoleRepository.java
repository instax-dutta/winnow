package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.AppRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByCode(String code);
}
