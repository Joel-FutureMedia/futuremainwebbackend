package com.futuremedia.repositories;

import com.futuremedia.models.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

    List<Vacancy> findByPublishedTrueOrderByCreatedDateDesc();

    List<Vacancy> findAllByOrderByCreatedDateDesc();
}
