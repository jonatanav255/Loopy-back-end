package com.loopy.topic.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.TopicResponse;
import com.loopy.topic.dto.UpdateTopicRequest;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public List<TopicResponse> getTopics(User user) {
        return topicRepository.findByUserIdOrderByNameAsc(user.getId()).stream()
                .map(TopicResponse::from)
                .toList();
    }

    public TopicResponse getTopic(java.util.UUID id, User user) {
        Topic topic = topicRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        return TopicResponse.from(topic);
    }

    @Transactional
    public TopicResponse createTopic(CreateTopicRequest request, User user) {
        Topic topic = new Topic(
                user,
                request.name(),
                request.description(),
                request.colorHex() != null ? request.colorHex() : "#6366F1"
        );
        return TopicResponse.from(topicRepository.save(topic));
    }

    @Transactional
    public TopicResponse updateTopic(java.util.UUID id, UpdateTopicRequest request, User user) {
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
    public void deleteTopic(java.util.UUID id, User user) {
        Topic topic = topicRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        topicRepository.delete(topic);
    }
}
