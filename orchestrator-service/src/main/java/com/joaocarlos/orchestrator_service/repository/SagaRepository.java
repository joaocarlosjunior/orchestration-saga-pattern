package com.joaocarlos.orchestrator_service.repository;

import com.joaocarlos.orchestrator_service.domain.Saga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaRepository extends JpaRepository<Saga, String> {}
