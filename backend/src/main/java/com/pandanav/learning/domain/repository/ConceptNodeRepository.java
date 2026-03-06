package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.ConceptNode;

import java.util.List;
import java.util.Optional;

public interface ConceptNodeRepository {

    ConceptNode save(ConceptNode node);

    Optional<ConceptNode> findById(Long id);

    List<ConceptNode> findByChapterId(String chapterId);
}
