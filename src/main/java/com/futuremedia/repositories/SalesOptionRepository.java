package com.futuremedia.repositories;

import com.futuremedia.models.SalesOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOptionRepository extends JpaRepository<SalesOption, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<SalesOption> findByNameIgnoreCase(String name);

    List<SalesOption> findByActiveTrueOrderByNameAsc();

    List<SalesOption> findAllByOrderByNameAsc();
}
