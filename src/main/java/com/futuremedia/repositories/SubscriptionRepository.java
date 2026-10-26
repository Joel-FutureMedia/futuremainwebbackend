package com.futuremedia.repositories;

import com.futuremedia.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Subscription> findByActiveTrue();

    List<Subscription> findAllByOrderBySubscribedDateDesc();

    long countByActiveTrue();
}
