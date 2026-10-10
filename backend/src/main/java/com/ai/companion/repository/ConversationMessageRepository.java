package com.ai.companion.repository;

import com.ai.companion.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository
        extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByIdentityIdOrderByIdAsc(Long identityId);

    List<ConversationMessage> findTop200ByIdentityIdOrderByIdDesc(Long identityId);

    long deleteByIdentityId(Long identityId);

    long deleteAllByIdentityId(Long identityId);
}
