package com.futuremedia.repositories;

import com.futuremedia.models.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    Optional<Banner> findFirstByActiveTrueOrderByUpdatedDateDesc();

    List<Banner> findAllByOrderByCreatedDateDesc();
}
