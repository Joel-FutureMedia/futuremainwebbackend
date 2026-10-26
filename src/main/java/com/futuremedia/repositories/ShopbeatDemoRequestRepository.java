package com.futuremedia.repositories;

import com.futuremedia.models.ShopbeatDemoRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopbeatDemoRequestRepository extends JpaRepository<ShopbeatDemoRequest, Long> {

    List<ShopbeatDemoRequest> findAllByOrderBySubmittedDateDesc();
}
