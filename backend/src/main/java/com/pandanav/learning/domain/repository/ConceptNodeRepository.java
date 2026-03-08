package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.ConceptNode;

import java.util.List;
import java.util.Optional;

public interface ConceptNodeRepository {

    Optional<ConceptNode> findById(Long id);

    Optional<ConceptNode> findFirstByChapterIdOrderByOrderNoAsc(String chapterId);

    List<ConceptNode> findByChapterIdOrderByOrderNoAsc(String chapterId);

    ConceptNode save(ConceptNode node);
}
