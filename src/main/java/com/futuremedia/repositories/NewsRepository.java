package com.futuremedia.repositories;

import com.futuremedia.models.ApprovalStatus;
import com.futuremedia.models.News;
import com.futuremedia.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    List<News> findByApprovalStatusOrderByCreatedDateDesc(ApprovalStatus status);

    List<News> findByCreatedByOrderByCreatedDateDesc(User createdBy);

    List<News> findAllByOrderByCreatedDateDesc();
}
