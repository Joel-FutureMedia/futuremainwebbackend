package com.futuremedia.repositories;

import com.futuremedia.models.Lead;
import com.futuremedia.models.LeadSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findAllByOrderByCapturedDateDesc();

    List<Lead> findBySourceOrderByCapturedDateDesc(LeadSource source);

    @Query("SELECT l FROM leads l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(l.telephone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(l.company) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Lead> search(@Param("keyword") String keyword);

    @Query("SELECT s.name, COUNT(l) FROM leads l JOIN l.source s GROUP BY s.id, s.name ORDER BY COUNT(l) DESC")
    List<Object[]> countGroupedBySource();
}
