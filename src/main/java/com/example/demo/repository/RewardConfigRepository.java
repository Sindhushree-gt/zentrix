package com.example.demo.repository;

import com.example.demo.model.RewardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardConfigRepository extends JpaRepository<RewardConfig, Long> {
}
