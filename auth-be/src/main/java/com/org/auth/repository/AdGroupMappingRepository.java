package com.org.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.auth.entity.AdGroupMapping;

public interface AdGroupMappingRepository extends JpaRepository<AdGroupMapping, Long> {

    Optional<AdGroupMapping> findByAdGroupId(String adGroupId);

    List<AdGroupMapping> findByAdGroupIdIn(List<String> adGroupIds);

    boolean existsByAdGroupId(String adGroupId);
}
