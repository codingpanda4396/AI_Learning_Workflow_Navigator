package navigator.application.learning;

import navigator.domain.enums.LearningActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningActionDetectorTest {

    private final LearningActionDetector detector = new LearningActionDetector();

    @Test
    void detectsExampleAndComparison() {
        assertEquals(LearningActionType.ASK_FOR_EXAMPLE, detector.detect("请举个例子说明"));
        assertEquals(LearningActionType.ASK_FOR_COMPARISON, detector.detect("数组和链表有什么区别"));
        assertEquals(LearningActionType.ASK_FOR_SIMPLIFICATION, detector.detect("说得更简单一点"));
        assertEquals(LearningActionType.CONFUSION_SIGNAL, detector.detect("我还是不懂"));
        assertEquals(LearningActionType.SEEK_DIRECT_ANSWER, detector.detect("直接给我答案"));
        assertEquals(LearningActionType.SELF_EXPLANATION, detector.detect("我这样理解对吗"));
    }
}
