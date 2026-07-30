package com.futuremedia.repositories;

import com.futuremedia.models.JobApplication;
import com.futuremedia.models.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByVacancyOrderByAppliedDateDesc(Vacancy vacancy);

    List<JobApplication> findAllByOrderByAppliedDateDesc();

    @Query("SELECT j FROM job_applications j WHERE LOWER(j.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(j.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<JobApplication> searchApplicants(@Param("keyword") String keyword);
}
