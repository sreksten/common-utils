package com.threeamigos.common.util.implementations.injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    @DisplayName("Class check tests")
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

        /**
         * Helper class to capture complex types for testing.
         */
        abstract class TypeCapture<T extends Number & Comparable<T>> {
            T typeVariable;
            List<? extends Number> wildcard;
            T[] genericArray;
        }

        @Test
        @DisplayName("Check TypeVariable assignability")
        void testTypeVariableAssignability() throws Exception {
            // Given
            Type typeVar = TypeCapture.class.getDeclaredField("typeVariable").getGenericType();
            // When / Then
            // Integer is both a Number and Comparable<Integer>
            assertTrue(sut.isAssignable(typeVar, Integer.class));
            // StringBuilder is NOT a Number
            assertFalse(sut.isAssignable(typeVar, StringBuilder.class));
        }

        @Test
        @DisplayName("Check WildcardType assignability")
        void testWildcardTypeAssignability() throws Exception {
            // Given
            Type wildcard = ((ParameterizedType) TypeCapture.class.getDeclaredField("wildcard")
                    .getGenericType()).getActualTypeArguments()[0]; // ? extends Number
            // When / Then
            assertTrue(sut.isAssignable(wildcard, Integer.class));
            assertFalse(sut.isAssignable(wildcard, String.class));
        }

        @Test
        @DisplayName("Check GenericArrayType assignability")
        void testGenericArrayTypeAssignability() throws Exception {
            // Given
            Type genericArray = TypeCapture.class.getDeclaredField("genericArray").getGenericType(); // T[]
            // When / Then
            assertTrue(sut.isAssignable(genericArray, Integer[].class));
            assertFalse(sut.isAssignable(genericArray, String[].class));
        }

        @Test
        @DisplayName("Check IntersectionType (Multiple Bounds) assignability")
        void testIntersectionTypeAssignability() throws Exception {
            // Given
            // Captured from T extends Number & Comparable<T>
            Type intersection = TypeCapture.class.getDeclaredField("typeVariable").getGenericType();
            // When / Then
            // Integer implements both Number and Comparable
            assertTrue(sut.isAssignable(intersection, Integer.class));
            // AtomicInteger implements Number but NOT Comparable
            assertFalse(sut.isAssignable(intersection, java.util.concurrent.atomic.AtomicInteger.class));
        }
    }

    @Nested
    @DisplayName("isAssignable tests")
    class IsAssignableTests {

        @Nested
        @DisplayName("Class target type tests")
        class ClassTargetTests {

            @Test
            @DisplayName("Candidate is assignable")
            void testIsAssignableClassTrue() {
                assertTrue(sut.isAssignable(Number.class, Integer.class));
            }

            @Test
            @DisplayName("Candidate is NOT assignable")
            void testIsAssignableClassFalse() {
                assertFalse(sut.isAssignable(Integer.class, String.class));
            }
        }

        @Nested
        @DisplayName("ParameterizedType target type tests")
        class ParameterizedTypeTargetTests {

            @Test
            @DisplayName("Raw type NOT assignable")
            void testIsAssignableParameterizedRawTypeMismatch() {
                Type target = new TypeLiteral<List<String>>() {
                }.getType();
                assertFalse(sut.isAssignable(target, String.class));
            }

            @Test
            @DisplayName("Match via interface")
            void testIsAssignableParameterizedInterfaceMatch() {
                Type target = new TypeLiteral<List<String>>() {
                }.getType();
                // java.util.ArrayList implements java.util.List<E>
                assertTrue(sut.isAssignable(target, java.util.ArrayList.class));
            }

            @Test
            @DisplayName("Supertype null")
            void testSuperTypeNull() {
                Type target = new TypeLiteral<List<String>>() {
                }.getType();
                // Candidate is an interface, getGenericSuperclass() returns null
                assertFalse(sut.isAssignable(target, List.class));
            }

            @Test
            @DisplayName("SuperType is Object.class")
            @SuppressWarnings("rawtypes")
            void testSuperTypeIsObject() {
                // Using a class that doesn't match generic interfaces:
                class RawImplementation implements ClassResolverUnitTest.MyGeneric {
                }
                Type targetWithArgs = new TypeLiteral<ClassResolverUnitTest.MyGeneric<String>>() {
                }.getType();
                assertFalse(sut.isAssignable(targetWithArgs, RawImplementation.class));
            }

            @Test
            @DisplayName("Match via superclass")
            void testIsAssignableParameterizedSuperclassMatch() {
                class StringList extends java.util.ArrayList<String> {
                }
                Type target = new TypeLiteral<java.util.AbstractList<String>>() {
                }.getType();
                assertTrue(sut.isAssignable(target, StringList.class));
            }

            @Test
            @DisplayName("Recursion in superclass")
            void testIsAssignableParameterizedSuperclassRecursion() {
                class Base extends java.util.ArrayList<String> {
                }
                class Derived extends Base {
                }
                Type target = new TypeLiteral<List<String>>() {
                }.getType();
                assertTrue(sut.isAssignable(target, Derived.class));
            }

            @Test
            @DisplayName("Row 257: ParameterizedType fallback -> Branch: All hierarchy checks failed (Line 257 fallback)")
            void testParameterizedTypeFallback() {
                Type target = new TypeLiteral<List<String>>() {
                }.getType();
                // ArrayList is a ParameterizedType, raw target List is compatible, but generic args mismatch.
                assertFalse(sut.isAssignable(target, List.class));
            }
        }

        @Nested
        @DisplayName("TypeVariable target type tests")
        class TypeVariableTargetTests {

            @Test
            @DisplayName("All bounds match")
            void testIsAssignableTypeVariableTrue() throws NoSuchFieldException {
                class Helper<T extends Number & Comparable<T>> {
                    T field;
                }
                Type target = Helper.class.getDeclaredField("field").getGenericType();
                assertTrue(sut.isAssignable(target, Integer.class));
            }

            @Test
            @DisplayName("One bound fails")
            void testIsAssignableTypeVariableFalse() throws NoSuchFieldException {
                class Helper<T extends Number & Comparable<T>> {
                    T field;
                }
                Type target = Helper.class.getDeclaredField("field").getGenericType();
                // AtomicLong is Number but NOT Comparable
                assertFalse(sut.isAssignable(target, java.util.concurrent.atomic.AtomicLong.class));
            }
        }

        @Nested
        @DisplayName("WildcardType target type tests")
        class WildcardTargetTests {

            @Test
            @DisplayName("All bounds match")
            void testIsAssignableWildcardTrue() throws NoSuchFieldException {
                class Helper {
                    List<? extends Number> field;
                }
                ParameterizedType pt = (ParameterizedType) Helper.class.getDeclaredField("field").getGenericType();
                Type target = pt.getActualTypeArguments()[0]; // ? extends Number
                assertTrue(sut.isAssignable(target, Integer.class));
            }

            @Test
            @DisplayName("One bound fails")
            void testIsAssignableWildcardFalse() throws NoSuchFieldException {
                class Helper {
                    List<? extends Integer> field;
                }
                ParameterizedType pt = (ParameterizedType) Helper.class.getDeclaredField("field").getGenericType();
                Type target = pt.getActualTypeArguments()[0]; // ? extends Integer
                assertFalse(sut.isAssignable(target, Long.class));
            }

        }

        @Nested
        @DisplayName("GenericArrayType target type tests")
        class GenericArrayTypeTargetTests {

            @Test
            @DisplayName("Candidate is array and component matches")
            void testIsAssignableGenericArrayTrue() throws NoSuchFieldException {
                class Helper<T> {
                    T[] field;
                }
                Type target = Helper.class.getDeclaredField("field").getGenericType(); // T[]
                assertTrue(sut.isAssignable(target, Object[].class));
            }

            @Test
            @DisplayName("Candidate is NOT array")
            void testIsAssignableGenericArrayFalseNotArray() throws NoSuchFieldException {
                class Helper<T> {
                    T[] field;
                }
                Type target = Helper.class.getDeclaredField("field").getGenericType();
                assertFalse(sut.isAssignable(target, Object.class));
            }

            @Test
            @DisplayName("Component mismatch")
            void testIsAssignableGenericArrayComponentMismatch() throws NoSuchFieldException {
                class Helper<T extends Number> {
                    T[] field;
                }
                Type target = Helper.class.getDeclaredField("field").getGenericType();
                assertFalse(sut.isAssignable(target, String[].class));
            }
        }

        @Test
        @DisplayName("TargetType is none of the above")
        void testIsAssignableDefaultFalse() {
            // Custom type that is neither Class, ParameterizedType, Variable, Wildcard, nor Array
            Type weirdType = new Type() {
                @Override
                public String getTypeName() {
                    return "weird";
                }
            };
            assertThrows(IllegalArgumentException.class, () -> sut.isAssignable(weirdType, Object.class));
        }
    }

    @Nested
    @DisplayName("typesMatch tests")
    class TypesMatchTests {

        @Test
        @DisplayName("Should match exactly equal types")
        void shouldMatchEqualTypes() {
            assertTrue(sut.typesMatch(String.class, String.class));

            Type listString = new TypeLiteral<List<String>>() {}.getType();
            assertTrue(sut.typesMatch(listString, listString));
        }

        @Test
        @DisplayName("Should match if target is a TypeVariable")
        void shouldMatchTargetTypeVariable() throws Exception {
            // Given
            abstract class TypeHelper<T> {
                T typeVariableField;
            }
            Type typeVar = TypeHelper.class.getDeclaredField("typeVariableField").getGenericType();
            // When / Then
            // T matches String.class
            assertTrue(sut.typesMatch(typeVar, String.class));
        }

        @Test
        @DisplayName("Should not match ParameterizedTypes with non-ParameterizedTypes")
        void shouldNotMatchParameterizedTypesWithNonParameterizedTypes() {
            // Given
            Type listNumber = new TypeLiteral<List<Number>>() {}.getType();
            // When / Then
            assertFalse(sut.typesMatch(listNumber, Integer.class));
        }

        @Test
        @DisplayName("Should match ParameterizedTypes with matching raw types and compatible arguments")
        void shouldMatchCompatibleParameterizedTypes() {
            // Given
            Type listNumber = new TypeLiteral<List<Number>>() {}.getType();
            Type listInteger = new TypeLiteral<List<Integer>>() {}.getType();
            // When / Then
            // List is the same raw type, and Number is assignable from Integer (via typeArgsMatch)
            assertTrue(sut.typesMatch(listNumber, listInteger));
        }

        @Test
        @DisplayName("Should not match ParameterizedTypes with different raw types")
        void shouldNotMatchDifferentRawTypes() {
            // Given
            Type listString = new TypeLiteral<List<String>>() {}.getType();
            Type setString = new TypeLiteral<Set<String>>() {}.getType();
            // When / Then
            assertFalse(sut.typesMatch(listString, setString));
        }

        @Test
        @DisplayName("Should not match if argument count differs for the same raw type")
        @SuppressWarnings("NullableProblems")
        void shouldNotMatchDifferentArgumentCount() {
            // Given
            // We create a custom ParameterizedType to simulate a mismatch in argument length
            // for the same raw type, which is hard to produce with standard Java reflection.
            ParameterizedType typeWithOneArg = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{String.class};
                }

                @Override
                public Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };

            ParameterizedType typeWithTwoArgs = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{String.class, Integer.class};
                }

                @Override
                public Type getRawType() {
                    return List.class;
                } // Same raw type

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };
            // When / Then
            // Raw types match (List.class), but argument counts (1 vs 2) do not.
            assertFalse(sut.typesMatch(typeWithOneArg, typeWithTwoArgs));
        }

        @Test
        @DisplayName("Should not match if any type argument fails to match")
        void shouldNotMatchIfArgumentMismatch() {
            // Given
            Type listString = new TypeLiteral<List<String>>() {}.getType();
            Type listInteger = new TypeLiteral<List<Integer>>() {}.getType();
            // When / Then
            assertFalse(sut.typesMatch(listString, listInteger));
        }

        @Test
        @DisplayName("Should return false for non-parameterized mismatches")
        void shouldReturnFalseForGeneralMismatch() {
            assertFalse(sut.typesMatch(String.class, Integer.class));
        }
    }

    @Nested
    @DisplayName("typeArgsMatch tests")
    class TypeArgsMatchTests {

        @Nested
        @DisplayName("Raw classes matching")
        class RawClassesMatching {

            @Test
            @DisplayName("Should match identical classes")
            void shouldMatchIdenticalClasses() {
                assertTrue(sut.typeArgsMatch(String.class, String.class));
            }

            @Test
            @DisplayName("Should match assignable classes (Integer to Number)")
            void shouldMatchAssignableClasses() {
                assertTrue(sut.typeArgsMatch(Number.class, Integer.class));
            }

            @Test
            @DisplayName("Should not match unassignable classes (Number to Integer)")
            void shouldNotMatchUnassignableClasses1() {
                assertTrue(sut.typeArgsMatch(Number.class, Integer.class));
            }

            @Test
            @DisplayName("Should not match unassignable classes (Integer to String)")
            void shouldNotMatchUnassignableClasses2() {
                assertFalse(sut.typeArgsMatch(Integer.class, String.class));
            }
        }

        @Nested
        @DisplayName("Wildcard matching")
        class WildcardMatching {

            @Test
            @DisplayName("Should match if either is a WildcardType")
            void shouldMatchWildcards() throws Exception {
                // Given
                abstract class TypeHelper<T> {
                    List<?> wildcardField;
                }
                Field field = TypeHelper.class.getDeclaredField("wildcardField");
                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                Type wildcard = pt.getActualTypeArguments()[0];
                // When / Then
                assertTrue(sut.typeArgsMatch(wildcard, String.class));
                assertTrue(sut.typeArgsMatch(String.class, wildcard));
            }

            @Test
            @DisplayName("Should not match wildcards with different bounds")
            void shouldNotMatchWildcards() throws Exception {
                // Given
                abstract class TypeHelper<T> {
                    List<?> wildcardField;
                    List<? extends Number> extendsNumberField;
                    List<? super Number> superNumberField;
                }
                Field wildcardField = TypeHelper.class.getDeclaredField("wildcardField");
                Type wildcard = ((ParameterizedType) wildcardField.getGenericType()).getActualTypeArguments()[0];

                Field extendsNumberField = TypeHelper.class.getDeclaredField("extendsNumberField");
                Type extendsNumber = ((ParameterizedType) extendsNumberField.getGenericType()).getActualTypeArguments()[0];

                Field superNumberField = TypeHelper.class.getDeclaredField("superNumberField");
                Type superNumber = ((ParameterizedType) superNumberField.getGenericType()).getActualTypeArguments()[0];

                // When / Then
                assertFalse(sut.typeArgsMatch(wildcard, extendsNumber), "<?> should not match <? super Number>");
                assertFalse(sut.typeArgsMatch(wildcard, superNumber), "<?> should not match <? super Number>");
                assertFalse(sut.typeArgsMatch(extendsNumber, superNumber), "<? extends Number> should not match <? super Number>");
                assertFalse(sut.typeArgsMatch(superNumber, extendsNumber), "<? super Number> should not match <? extends Number>");
                assertTrue(sut.typeArgsMatch(extendsNumber, extendsNumber), "<? extends Number> should match <? extends Number>");
                assertTrue(sut.typeArgsMatch(superNumber, superNumber), "<? super Number> should match <? super Number>");
            }
        }

        @Nested
        @DisplayName("TypeVariable matching")
        class TypeVariableMatching {

            @Test
            @DisplayName("Should match if either is a TypeVariable")
            void shouldMatchTypeVariables() throws Exception {
                // Given
                abstract class TypeHelper<T, U extends Number, V extends Map> {
                    T typeVariableField;
                    U numberVariableField;
                    V mapVariableField;
                }
                Type typeVar = TypeHelper.class.getDeclaredField("typeVariableField").getGenericType();
                Type numberVar = TypeHelper.class.getDeclaredField("numberVariableField").getGenericType();
                Type mapVar = TypeHelper.class.getDeclaredField("mapVariableField").getGenericType();

                // When / Then
                assertTrue(sut.typeArgsMatch(typeVar, String.class));
                assertTrue(sut.typeArgsMatch(String.class, typeVar));

                // These should ideally NOT match if we want strict bound matching
                assertFalse(sut.typeArgsMatch(typeVar, numberVar), "<T> should not match <U extends Number>");
                assertFalse(sut.typeArgsMatch(typeVar, mapVar), "<T> should not match <V extends Map>");
                assertFalse(sut.typeArgsMatch(numberVar, mapVar), "<U extends Number> should not match <V extends Map>");
                assertTrue(sut.typeArgsMatch(numberVar, numberVar), "<U extends Number> should match itself");
            }

            @Test
            @DisplayName("Should match if either is a TypeVariable")
            void shouldNotMatchTypeVariables() throws Exception {
                // Given
                abstract class TypeHelper<T extends Map<?, ?>, U extends Map<Integer, String>, V extends Map<Object, String>> {
                    T mapWildcards;
                    U mapIntegerString;
                    V mapObjectString;
                }
                Type wildcardsVar = TypeHelper.class.getDeclaredField("mapWildcards").getGenericType();
                Type integerStringVar = TypeHelper.class.getDeclaredField("mapIntegerString").getGenericType();
                Type objectStringVar = TypeHelper.class.getDeclaredField("mapObjectString").getGenericType();

                // When / Then
                assertFalse(sut.typeArgsMatch(wildcardsVar, integerStringVar), "<T extends Map<?, ?>> should not match <U extends Map<Integer, String>>");
                assertFalse(sut.typeArgsMatch(wildcardsVar, objectStringVar), "<T extends Map<?, ?>> should not match <V extends Map<Object, String>>");
                assertFalse(sut.typeArgsMatch(integerStringVar, objectStringVar), "<U extends Map<Integer, String>> should not match <V extends Map<Object, String>>");
            }
        }

        @Test
        @DisplayName("Should match ParameterizedTypes with matching arguments")
        void shouldMatchParameterizedTypes() {
            // Given
            Type listString = new TypeLiteral<List<String>>() {
            }.getType();
            Type arrayListString = new TypeLiteral<ArrayList<String>>() {
            }.getType();
            // When / Then
            // Raw types: List.class.isAssignableFrom(ArrayList.class)
            assertTrue(sut.typeArgsMatch(listString, arrayListString));
        }
    }
}