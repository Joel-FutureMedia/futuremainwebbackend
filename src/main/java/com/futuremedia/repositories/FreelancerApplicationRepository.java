package com.futuremedia.repositories;

import com.futuremedia.models.FreelancerApplication;
import com.futuremedia.models.FreelancerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerApplicationRepository extends JpaRepository<FreelancerApplication, Long> {

    List<FreelancerApplication> findByRoleOrderByAppliedDateDesc(FreelancerRole role);

    List<FreelancerApplication> findAllByOrderByAppliedDateDesc();

    @Query("SELECT a FROM freelancer_applications a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.surname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.skill) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FreelancerApplication> searchApplicants(@Param("keyword") String keyword);
}
