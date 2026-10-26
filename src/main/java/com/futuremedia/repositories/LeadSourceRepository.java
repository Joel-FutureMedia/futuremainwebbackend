package com.futuremedia.repositories;

import com.futuremedia.models.LeadSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadSourceRepository extends JpaRepository<LeadSource, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<LeadSource> findByNameIgnoreCase(String name);

    List<LeadSource> findByActiveTrueOrderByNameAsc();
}
