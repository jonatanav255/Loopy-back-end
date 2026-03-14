package com.loopy.topic.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.repository.CardRepository;
import com.loopy.config.ReorderRequest;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.TopicResponse;
import com.loopy.topic.dto.UpdateTopicRequest;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final CardRepository cardRepository;

    public TopicService(TopicRepository topicRepository, CardRepository cardRepository) {
        this.topicRepository = topicRepository;
        this.cardRepository = cardRepository;
    }

    public List<TopicResponse> getTopics(User user) {
        return topicRepository.findByUserIdOrderBySortOrderAsc(user.getId()).stream()
                .map(t -> TopicResponse.from(t, cardRepository.countByTopicId(t.getId())))
                .toList();
    }

    public TopicResponse getTopic(UUID id, User user) {
        Topic topic = topicRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        return TopicResponse.from(topic, cardRepository.countByTopicId(topic.getId()));
    }

    @Transactional
    public TopicResponse createTopic(CreateTopicRequest request, User user) {
        // Assign next sort order
        List<Topic> existing = topicRepository.findByUserIdOrderBySortOrderAsc(user.getId());
        int nextOrder = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getSortOrder() + 1;

        Topic topic = new Topic(
                user,
                request.name(),
                request.description(),
                request.colorHex() != null ? request.colorHex() : "#6366F1"
        );
        topic.setSortOrder(nextOrder);
        return TopicResponse.from(topicRepository.save(topic));
    }

    @Transactional
    public TopicResponse updateTopic(UUID id, UpdateTopicRequest request, User user) {
        Topic topic = topicRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        topic.setName(request.name());
        topic.setDescription(request.description());
        if (request.colorHex() != null) {
            topic.setColorHex(request.colorHex());
        }

        return TopicResponse.from(topicRepository.save(topic));
    }

    @Transactional
    public void deleteTopic(UUID id, User user) {
        Topic topic = topicRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        topicRepository.delete(topic);
    }

    /** Reorder topics based on the ordered list of IDs. */
    @Transactional
    public List<TopicResponse> reorderTopics(ReorderRequest request, User user) {
        List<UUID> orderedIds = request.orderedIds();
        Map<UUID, Topic> topicMap = topicRepository.findAllByUserIdAndIdIn(user.getId(), orderedIds)
                .stream().collect(Collectors.toMap(Topic::getId, Function.identity()));

        if (topicMap.size() != orderedIds.size()) {
            throw new ResourceNotFoundException("One or more topics not found");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            topicMap.get(orderedIds.get(i)).setSortOrder(i + 1);
        }
        topicRepository.saveAll(topicMap.values());

        return topicRepository.findByUserIdOrderBySortOrderAsc(user.getId()).stream()
                .map(t -> TopicResponse.from(t, cardRepository.countByTopicId(t.getId())))
                .toList();
    }
}
