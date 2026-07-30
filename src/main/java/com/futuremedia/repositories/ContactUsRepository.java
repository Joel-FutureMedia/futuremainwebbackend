package com.futuremedia.repositories;

import com.futuremedia.models.ContactUs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactUsRepository extends JpaRepository<ContactUs, Long> {

    List<ContactUs> findAllByOrderBySubmittedDateDesc();
}
