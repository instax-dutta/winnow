package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.ExtractionResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractionResultRepository extends JpaRepository<ExtractionResult, Long> {

    List<ExtractionResult> findByRunId(Long runId);

    boolean existsByRunIdAndEmailHash(Long runId, byte[] emailHash);

    long countByRunId(Long runId);

    /**
     * Fetches the distinct domains of a run, used to report coverage.
     */
    @Query("select distinct r.domain from ExtractionResult r where r.run.id = :runId")
    List<String> findDistinctDomainsByRunId(@Param("runId") Long runId);
}
