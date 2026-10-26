package com.futuremedia.repositories;

import com.futuremedia.models.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Brand> findByNameIgnoreCase(String name);

    List<Brand> findByActiveTrueOrderByNameAsc();

    List<Brand> findAllByOrderByNameAsc();
}
