package com.pulse.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.repository.MessageRepository;
import com.pulse.repository.UserRepository;
import com.pulse.repository.GroupRepository;
import com.pulse.repository.GroupMemberRepository;

import lombok.RequiredArgsConstructor;
import com.pulse.dto.CreateMessageRequest;
import com.pulse.dto.MessageResponse;
import com.pulse.entity.Message;
import com.pulse.entity.User;
import com.pulse.entity.Group;


@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public MessageResponse createMessage(CreateMessageRequest request, Long currentUserId, Long groupId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!isMember(currentUserId, groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        Message message = Message.builder()
                .content(request.getContent())
                .sender(currentUser)
                .group(group)
                .build();

        Message savedMessage = messageRepository.save(message);

        return MessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesByGroup(Long groupId, Long currentUserId, int page, int size) {
        groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!isMember(currentUserId, groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messages = messageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        return messages.map(MessageResponse::from);
    }

    private boolean isMember(Long userId, Long groupId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

}
