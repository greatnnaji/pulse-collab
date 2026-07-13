package com.pulse.repository;

import com.pulse.entity.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    @Query("select gi from GroupInvite gi join fetch gi.group where gi.invitedUser.id = :userId order by gi.invitedAt desc")
    List<GroupInvite> findByInvitedUserIdWithGroup(Long userId);

    boolean existsByGroupIdAndInvitedUserId(Long groupId, Long invitedUserId);

    Optional<GroupInvite> findByIdAndInvitedUserId(Long id, Long invitedUserId);
}
