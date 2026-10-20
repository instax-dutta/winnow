package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameAndDeletedAtIsNull(String username);

    Optional<AppUser> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUsername(String username);

    List<AppUser> findByIdInAndDeletedAtIsNull(List<Long> ids);
}
