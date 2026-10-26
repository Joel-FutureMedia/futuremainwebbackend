package com.futuremedia.repositories;

import com.futuremedia.models.FreelancerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerRoleRepository extends JpaRepository<FreelancerRole, Long> {

    List<FreelancerRole> findByPublishedTrueOrderByCreatedDateDesc();

    List<FreelancerRole> findAllByOrderByCreatedDateDesc();

    long countByPublishedTrue();
}
