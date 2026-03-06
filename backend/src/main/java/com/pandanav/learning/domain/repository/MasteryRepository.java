package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Mastery;

import java.util.List;

public interface MasteryRepository {

    List<Mastery> findByUserIdAndChapterId(String userId, String chapterId);
}
