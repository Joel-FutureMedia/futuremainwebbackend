package com.futuremedia.repositories;

import com.futuremedia.models.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOptionRepository extends JpaRepository<ServiceOption, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<ServiceOption> findByNameIgnoreCase(String name);

    List<ServiceOption> findByActiveTrueOrderByNameAsc();

    List<ServiceOption> findAllByOrderByNameAsc();
}
