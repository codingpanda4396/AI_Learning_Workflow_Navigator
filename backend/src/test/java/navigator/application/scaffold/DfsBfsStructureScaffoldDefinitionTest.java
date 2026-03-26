package navigator.application.scaffold;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsBfsStructureScaffoldDefinitionTest {

    @Test
    void fourOrderedStructureCards() {
        assertEquals(4, DfsBfsStructureScaffoldDefinition.orderedActionIds().size());
        assertTrue(DfsBfsStructureScaffoldDefinition.isValidPromptKey(DfsBfsStructureScaffoldDefinition.ACTION_DEFER));
    }
}
