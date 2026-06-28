package com.upi.npci.repository;

import com.upi.npci.entity.VpaRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VpaRegistryRepository extends JpaRepository<VpaRegistry, String> {
}
