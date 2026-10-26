package com.futuremedia.repositories;

import com.futuremedia.models.ShopbeatFeeling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopbeatFeelingRepository extends JpaRepository<ShopbeatFeeling, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<ShopbeatFeeling> findByNameIgnoreCase(String name);

    List<ShopbeatFeeling> findByActiveTrueOrderByNameAsc();
}
