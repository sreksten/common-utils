package com.threeamigos.common.util.implementations.injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.util.TypeLiteral;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TypeCheckerUnitTest {

    private final TypeChecker sut = new TypeChecker();

    @Nested
    @DisplayName("Injection points validation")
    class InjectionPointsValidation {

        @Test
        @DisplayName("Should throw DefinitionException if injection point contains a Wildcard")
        void testInjectionPointNotWildcard() {
            assertThrows(javax.enterprise.inject.spi.DefinitionException.class, () -> sut.validateInjectionPoint(new TypeLiteral<List<?>>() {}.getType()));
        }

        @Test
        @DisplayName("Should throw DefinitionException if injection point is a TypeVariable")
        <E> void testInjectionPointNotTypeVariable() {
            assertThrows(javax.enterprise.inject.spi.DefinitionException.class, () -> sut.validateInjectionPoint(new TypeLiteral<E>() {}.getType()));
        }

        @Test
        @DisplayName("Should throw DefinitionException for nested wildcards")
        void testNestedWildcardValidation() {
            Type nestedWildcard = new TypeLiteral<List<Map<String, List<?>>>>() {}.getType();
            assertThrows(javax.enterprise.inject.spi.DefinitionException.class, () -> sut.validateInjectionPoint(nestedWildcard));
        }

        @Test
        @DisplayName("Should throw DefinitionException for GenericArray with Wildcard")
        void testGenericArrayWildcardValidation() throws NoSuchFieldException {
            class ArrayContainer { List<?>[] array; }
            Type type = ArrayContainer.class.getDeclaredField("array").getGenericType();
            assertThrows(javax.enterprise.inject.spi.DefinitionException.class, () -> sut.validateInjectionPoint(type));
        }

        @Test
        @DisplayName("Should throw DefinitionException for GenericArray with TypeVariable")
        <T> void testGenericArrayTypeVariableValidation() throws NoSuchFieldException {
            class ArrayContainer<E> { E[] array; }
            Type type = ArrayContainer.class.getDeclaredField("array").getGenericType();
            assertThrows(javax.enterprise.inject.spi.DefinitionException.class, () -> sut.validateInjectionPoint(type));
        }
    }

    @Nested
    @DisplayName("isAssignable tests")
    class IsAssignableTests {

        @Test
        @DisplayName("Should match specific target to generic implementation")
        void testSpecificTargetToGenericImplementation() {
            Type target = new TypeLiteral<List<String>>() {}.getType();
            // ArrayList<E> is the implementation
            assertTrue(sut.isAssignable(target, ArrayList.class));
        }

        @Test
        @DisplayName("Should return false when component types mismatch in arrays")
        void testArrayComponentMismatch() {
            Type target = String[].class;
            assertFalse(sut.isAssignable(target, Integer[].class));
        }

        @Test
        @DisplayName("Should return false when GenericArrayType target is matched against non-array")
        void testGenericArrayVsNonArray() throws NoSuchFieldException {
            class Container { String[] array; }
            Type target = Container.class.getDeclaredField("array").getGenericType();
            assertFalse(sut.isAssignable(target, String.class));
        }

        @Test
        @DisplayName("Should handle ParameterizedType fallback when getExactSuperType is null")
        void testParameterizedTypeFallback() {
            Type target = new TypeLiteral<List<String>>() {}.getType();
            // If implementationRaw is not assignable to targetRaw, it returns false early.
            // If it is assignable but getExactSuperType fails (rare with correct logic), it returns true.
            assertTrue(sut.isAssignable(target, ArrayList.class));
        }

        @Test
        @DisplayName("TargetType is none of the above")
        void testIsAssignableDefaultFalse() {
            Type weirdType = new Type() {
                @Override public String getTypeName() { return "weird"; }
            };
            assertThrows(IllegalArgumentException.class, () -> sut.isAssignable(weirdType, Object.class));
        }
    }

    @Nested
    @DisplayName("typesMatch tests")
    class TypesMatchTests {
        @Test
        @DisplayName("Should return false if raw types differ")
        void testTypesMatchDifferentRawTypes() {
            Type t1 = new TypeLiteral<List<String>>() {}.getType();
            Type t2 = new TypeLiteral<Set<String>>() {}.getType();
            assertFalse(sut.typesMatch(t1, t2));
        }

        @Test
        @DisplayName("Should return false if argument counts differ")
        void testTypesMatchDifferentArgCounts() {
            Type t1 = new TypeLiteral<Map<String, String>>() {}.getType();
            Type t2 = new TypeLiteral<List<String>>() {}.getType();
            assertFalse(sut.typesMatch(t1, t2));
        }
    }
    class ClassCheckTests {

        @Test
        @DisplayName("Check raw Class assignability")
        void testClassAssignability() {
            // Then
            assertTrue(sut.isAssignable(List.class, ArrayList.class));
            assertFalse(sut.isAssignable(String.class, Number.class));
        }

        @Test
        @DisplayName("Check ParameterizedType assignability")
        void testParameterizedTypeAssignability() {
            // Given
            Type listOfString = new TypeLiteral<List<String>>() {}.getType();
            Type arrayListOfString = new TypeLiteral<ArrayList<String>>() {}.getType();
            Type arrayListOfInteger = new TypeLiteral<ArrayList<Integer>>() {}.getType();
            // When / Then
            assertTrue(sut.isAssignable(listOfString, arrayListOfString));
            assertFalse(sut.isAssignable(listOfString, arrayListOfInteger));
        }

        @Test
        @DisplayName("Check GenericArrayType assignability")
        void testGenericArrayTypeAssignability() throws Exception {
            // Given
            abstract class ArrayCapture {
                String[] genericArray;
            }
            Type target = ArrayCapture.class.getDeclaredField("genericArray").getGenericType();
            // When / Then
            assertTrue(sut.isAssignable(target, String[].class));
            assertFalse(sut.isAssignable(target, Integer[].class));
        }
    }

    
}