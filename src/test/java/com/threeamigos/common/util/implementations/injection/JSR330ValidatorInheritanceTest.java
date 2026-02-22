package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JSR330Validator's inheritance hierarchy checking for @Inject method overrides.
 * Verifies that the validator correctly warns when a subclass overrides an @Inject-annotated
 * method from any level in the inheritance hierarchy without re-annotating it with @Inject.
 */
@DisplayName("JSR330Validator Inheritance Hierarchy Tests")
class JSR330ValidatorInheritanceTest {

    private KnowledgeBase knowledgeBase;
    private JSR330Validator validator;

    @BeforeEach
    void setUp() {
        knowledgeBase = new KnowledgeBase();
        validator = new JSR330Validator(knowledgeBase);
        validator.setStrict(true);
    }

    // Test classes for inheritance scenarios

    // Scenario 1: Direct inheritance (1 level)
    static class DirectParent {
        @Inject
        public void configure(String config) {
            // Parent method with @Inject
        }
    }

    static class DirectChild extends DirectParent {
        @Override
        public void configure(String config) {
            // Child overrides WITHOUT @Inject - should warn
        }
    }

    static class DirectChildWithInject extends DirectParent {
        @Inject
        @Override
        public void configure(String config) {
            // Child overrides WITH @Inject - should NOT warn
        }
    }

    // Scenario 2: Deep inheritance (3 levels)
    static class GrandParent {
        @Inject
        public void initialize(Integer value) {
            // Grandparent method with @Inject
        }
    }

    static class Parent extends GrandParent {
        // Does not override - inherits @Inject behavior from GrandParent
    }

    static class Child extends Parent {
        @Override
        public void initialize(Integer value) {
            // Child overrides WITHOUT @Inject (2 levels deep) - should warn
        }
    }

    // Scenario 3: Multiple levels with re-annotation
    static class Level1 {
        @Inject
        public void process(String data) {
            // Level 1 with @Inject
        }
    }

    static class Level2 extends Level1 {
        @Inject
        @Override
        public void process(String data) {
            // Level 2 re-annotates with @Inject
        }
    }

    static class Level3 extends Level2 {
        @Override
        public void process(String data) {
            // Level 3 overrides WITHOUT @Inject - should warn about Level2
        }
    }

    // Scenario 4: No @Inject in hierarchy
    static class PlainParent {
        public void regularMethod(String param) {
            // No @Inject
        }
    }

    static class PlainChild extends PlainParent {
        @Override
        public void regularMethod(String param) {
            // Override without @Inject, parent doesn't have it either - should NOT warn
        }
    }

    // Scenario 5: Child has @Inject field (triggering validation)
    static class ParentWithInjectMethod {
        @Inject
        public void setup(String config) {
            // Parent has @Inject method
        }
    }

    static class ChildWithField extends ParentWithInjectMethod {
        @Inject
        private String dependency; // This triggers class validation

        @Override
        public void setup(String config) {
            // Override without @Inject - should warn
        }
    }

    // Tests

    @Test
    @DisplayName("Should warn when direct child overrides @Inject method without annotation")
    void shouldWarnDirectChildOverrideWithoutInject() {
        // When
        validator.isValid(DirectChild.class);

        // Then
        assertTrue(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("configure") &&
                        w.contains("overrides @Inject method") &&
                        w.contains("DirectParent")),
                "Should warn about missing @Inject on overridden method");
    }

    @Test
    @DisplayName("Should NOT warn when direct child overrides @Inject method with annotation")
    void shouldNotWarnDirectChildOverrideWithInject() {
        // When
        validator.isValid(DirectChildWithInject.class);

        // Then
        assertFalse(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("configure") && w.contains("overrides @Inject method")),
                "Should NOT warn when @Inject is present on override");
    }

    @Test
    @DisplayName("Should warn when grandchild overrides @Inject method from grandparent (2 levels deep)")
    void shouldWarnGrandchildOverrideWithoutInject() {
        // When
        validator.isValid(Child.class);

        // Then
        assertTrue(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("initialize") &&
                        w.contains("overrides @Inject method") &&
                        w.contains("GrandParent")),
                "Should warn about missing @Inject even when override is 2 levels deep");
    }

    @Test
    @DisplayName("Should warn about immediate parent when method is re-annotated at intermediate level")
    void shouldWarnAboutImmediateParentWhenReAnnotated() {
        // When
        validator.isValid(Level3.class);

        // Then
        assertTrue(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("process") &&
                        w.contains("overrides @Inject method") &&
                        w.contains("Level2")),
                "Should warn about Level2 (immediate parent with @Inject)");

        // Should NOT mention Level1 because Level2 is closer
        assertFalse(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("Level1")),
                "Should only warn about closest @Inject parent");
    }

    @Test
    @DisplayName("Should NOT warn when neither parent nor child has @Inject")
    void shouldNotWarnWhenNoInjectInHierarchy() {
        // When
        validator.isValid(PlainChild.class);

        // Then
        assertFalse(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("regularMethod") && w.contains("overrides @Inject method")),
                "Should NOT warn when no @Inject exists in hierarchy");
    }

    @Test
    @DisplayName("Should warn in real validation scenario with injected fields")
    void shouldWarnInRealValidationScenario() {
        // When
        validator.isValid(ChildWithField.class);

        // Then
        assertTrue(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("setup") &&
                        w.contains("overrides @Inject method") &&
                        w.contains("ParentWithInjectMethod")),
                "Should warn in real scenario where child has @Inject field triggering validation");
    }

    @Test
    @DisplayName("Should handle classes with no superclass gracefully")
    void shouldHandleNoSuperclassGracefully() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> validator.isValid(Object.class));
    }

    @Test
    @DisplayName("Should stop checking at Object class")
    void shouldStopAtObjectClass() {
        // Given - a class hierarchy that extends Object
        class SimpleClass {
            public void method() {}
        }

        // When
        validator.isValid(SimpleClass.class);

        // Then - should not throw StackOverflowError or infinite loop
        assertTrue(true, "Validation should complete without infinite loop");
    }

    @Test
    @DisplayName("Should handle method with different signatures")
    void shouldHandleMethodWithDifferentSignatures() {
        // Given
        class Parent {
            @Inject
            public void method(String param) {}
        }

        class Child extends Parent {
            // Different signature - not an override
            public void method(Integer param) {}
        }

        // When
        validator.isValid(Child.class);

        // Then
        assertFalse(knowledgeBase.getWarnings().stream()
                .anyMatch(w -> w.contains("method") && w.contains("overrides @Inject method")),
                "Should NOT warn about methods with different signatures");
    }

    @Test
    @DisplayName("Warning message should include method name and declaring class")
    void warningMessageShouldIncludeDetails() {
        // When
        validator.isValid(DirectChild.class);

        // Then
        String warning = knowledgeBase.getWarnings().stream()
                .filter(w -> w.contains("configure"))
                .findFirst()
                .orElse("");

        assertTrue(warning.contains("Method configure"), "Should include method name");
        assertTrue(warning.contains("DirectChild"), "Should include child class name");
        assertTrue(warning.contains("DirectParent"), "Should include parent class name");
        assertTrue(warning.contains("injection will NOT occur"), "Should explain consequence");
    }
}
