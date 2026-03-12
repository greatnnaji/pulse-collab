package com.pulse.repository;

import com.pulse.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCreatedBy(Long userId);

    // TODO: Add custom queries for finding groups by member, search, etc.
}
