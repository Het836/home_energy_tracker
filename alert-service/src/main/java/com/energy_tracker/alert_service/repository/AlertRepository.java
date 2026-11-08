package com.energy_tracker.alert_service.repository;

import com.energy_tracker.alert_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}
