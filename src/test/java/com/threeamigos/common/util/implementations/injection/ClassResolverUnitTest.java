package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed1;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed2;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.subpackage.MultipleConcreteClassesNamed3;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesTestAlternativeImplementation1;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesTestInterface;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesTestStandardImplementation;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.*;
import com.threeamigos.common.util.implementations.injection.interfaces.namedimplementationsonly.NamedImplementationsOnlyImplementation1;
import com.threeamigos.common.util.implementations.injection.interfaces.namedimplementationsonly.NamedImplementationsOnlyInterface;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.interfaces.noimplementations.NoImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multiplenotannotatedconcreteclasses.MultipleNotAnnotatedAbstractClass;
import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationClass;
import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationInterface;
import com.threeamigos.common.util.implementations.injection.interfaces.multiplenotannotatedimplementations.MultipleNotAnnotatedImplementationsInterface;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName( "ClassResolver unit test")
class ClassResolverUnitTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("When package is empty or null")
    class NullOrEmptyPackage {

        @Test
        @DisplayName("Should work if package is null")
        void shouldFindALotOfClassesIfPackageIsNull() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null, null);
            // Then
            assertEquals(SingleImplementationClass.class, resolved);
        }

        @Test
        @DisplayName("Should work if package is empty")
        void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, "", null);
            // Then
            assertEquals(SingleImplementationClass.class, resolved);
        }
    }

    @Nested
    @DisplayName("When qualifiers is empty or null")
    class NullOrEmptyQualifiers {

        @Test
        @DisplayName("Should work if qualifiers collection is null")
        void shouldFindALotOfClassesIfPackageIsNull() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null, null);
            // Then
            assertEquals(SingleImplementationClass.class, resolved);
        }

        @Test
        @DisplayName("Should work if qualifiers collection is empty")
        void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null, Collections.emptyList());
            // Then
            assertEquals(SingleImplementationClass.class, resolved);
        }

    }

    @Nested
    @DisplayName("resolveImplementations with qualifiers")
    class ResolveImplementationsWithQualifiers {

        @Test
        @DisplayName("Should return all active classes if qualifiers are null")
        void shouldReturnAllActiveClassesIfQualifiersAreNull() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), null);
            // Then
            // Expects 4: Standard, Named1, Named2, NamedAndAnnotated. Alternatives are filtered out by default if not enabled.
            assertEquals(4, resolved.size());
        }

        @Test
        @DisplayName("Should return only matching classes for specific qualifier")
        void shouldReturnOnlyMatchingClassesForSpecificQualifier() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), qualifiers);
            // Then
            assertEquals(1, resolved.size());
            assertTrue(resolved.contains(MultipleImplementationsNamed1.class));
        }

        @Test
        @DisplayName("Should return all classes when @Any qualifier is used")
        void shouldReturnAllClassesWhenAnyQualifierIsUsed() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new Any() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Any.class;
                }
            });
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), qualifiers);
            // Then
            assertEquals(4, resolved.size());
        }
    }

    @Nested
    @DisplayName("Wrong parameters")
    class WrongParameters {

        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if package to search is a file")
        void shouldThrowUnsatisfiedResolutionExceptionIfPackageToSearchIsAFile() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () ->sut.resolveImplementation(NoImplementationsInterface.class,
                    "com.threeamigos.common.util.implementations.injection.wrongdirectory.fakeFileToSkip", null));
        }

        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if package to search does not exist")
        void shouldThrowUnsatisfiedResolutionExceptionIfPackageToSearchDoesNotExist() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () ->sut.resolveImplementation(NoImplementationsInterface.class,
                    "com.threeamigos.common.util.implementations.injection.notexistingpackage", null));
        }

        @Test
        @DisplayName("getClassesFromResource should return an empty collection if protocol is not recognized")
        void getClassesFromResourceShouldReturnAnEmptyCollectionIfProtocolNotRecognized() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            URL url = mock(URL.class);
            when(url.getProtocol()).thenReturn("unknown");
            // When
            Collection<Class<?>> classes = sut.getClassesFromResource(Thread.currentThread().getContextClassLoader(), url, "my-package");
            // Then
            assertTrue(classes.isEmpty());
        }

        @Test
        @DisplayName("findClassesInDirectory returns an empty collection if directory does not exist")
        void findClassesInDirectoryReturnsEmptyIfDirectoryDoesNotExist() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            File directory = new File("does-not-exist");
            // When
            Collection<Class<?>> classes = sut.findClassesInDirectory(Thread.currentThread().getContextClassLoader(), directory, "my.package.name");
            // Then
            assertTrue(classes.isEmpty());
        }

        @Test
        @DisplayName("findClassesInDirectory returns an empty collection if directory is actually a file")
        void findClassesInDirectoryReturnsEmptyIfDirectoryIsActuallyAFile() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            File aFile = tempDir.resolve("a-file.txt").toFile();
            if (!aFile.createNewFile()) {
                fail("Could not create temporary file");
            }
            // When
            Collection<Class<?>> classes = sut.findClassesInDirectory(Thread.currentThread().getContextClassLoader(), aFile, "my.package.name");
            // Then
            assertTrue(classes.isEmpty());
        }

    }

    @Test
    @DisplayName("Should use the cache to avoid redundant class loading")
    void shouldUseTheCache() throws Exception {
        // Given
        ClassResolver sut = new ClassResolver();
        ClassLoader mockLoader = mock(ClassLoader.class);
        String packageName = "com.threeamigos";
        String expectedPath = "com/threeamigos";

        // Stub getResources to return an empty enumeration so the loop finishes
        when(mockLoader.getResources(expectedPath)).thenReturn(Collections.emptyEnumeration());

        // When - calling the public method that internally calls getClasses
        sut.resolveImplementations(mockLoader, SingleImplementationInterface.class, packageName);
        sut.resolveImplementations(mockLoader, MultipleImplementationsInterface.class, packageName);

        // Then - Verify the ClassLoader was queried exactly once
        verify(mockLoader, times(1)).getResources(expectedPath);
    }

    @Test
    @DisplayName("Should remember already resolved classes")
    void shouldRememberAlreadyResolvedClasses() throws Exception {
        // Given
        ClassResolver sut = new ClassResolver();
        ClassLoader mockLoader = mock(ClassLoader.class);
        String packageName = "com.threeamigos";
        String expectedPath = "com/threeamigos";

        // Stub getResources to return an empty enumeration so the loop finishes
        when(mockLoader.getResources(expectedPath)).thenReturn(Collections.emptyEnumeration());

        // When - calling the public method that internally calls getClasses
        sut.resolveImplementations(mockLoader, SingleImplementationInterface.class, packageName);
        sut.resolveImplementations(mockLoader, SingleImplementationInterface.class, packageName);

        // Then - Verify the ClassLoader was queried exactly once
        verify(mockLoader, times(1)).getResources(expectedPath);
    }

    @Nested
    @DisplayName("Interface tests")
    class InterfaceTests {

        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if no implementations found")
        void shouldThrowUnsatisfiedResolutionExceptionIfNoImplementationsFound() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoImplementationsInterface.class, getPackageName(NoImplementationsInterface.class), null));
        }

        /**
         * If we have a unique implementation, it does not need to be annotated.
         */
        @Test
        @DisplayName("Should resolve an interface with a single standard implementation")
        void shouldResolveInterfaceWithSingleStandardImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, getPackageName(SingleImplementationInterface.class), null);
            // Then
            assertEquals(SingleImplementationClass.class, resolved);
        }

        /**
         * When we have multiple implementations, the only one of them not annotated with {@link Named} is
         * considered the standard implementation.
         */
        @Test
        @DisplayName("Should resolve an interface with multiple implementations with standard implementation")
        void shouldResolveInterfaceWithStandardImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), null);
            // Then
            assertEquals(MultipleImplementationsStandardImplementation.class, resolved);
        }

        /**
         * When we have multiple implementations, we can specify one of the alternative implementations to be used
         * by specifying the qualifier.
         */
        @Test
        @DisplayName("Should resolve an interface with named-annotated implementation")
        void shouldResolveInterfaceWithSpecifiedNamedImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), qualifiers);
            // Then
            assertEquals(MultipleImplementationsNamed1.class, resolved);
        }

        /**
         * When we have multiple implementations, we can specify one of the alternative implementations to be used
         * by specifying the qualifier.
         */
        @Test
        @DisplayName("Should resolve an interface with qualifier-annotated implementation")
        void shouldResolveInterfaceWithSpecifiedQualifierImplementation() throws Exception {
            // Given
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Arrays.asList(
                    new NamedLiteral("name"),
                    new MyQualifier() {
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return MyQualifier.class;
                        }
                        @Override
                        public boolean equals(Object obj) {
                            return obj instanceof MyQualifier;
                        }
                        @Override
                        public int hashCode() {
                            return 0;
                        }
                    }
            );
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), qualifiers);
            // Then
            assertEquals(MultipleImplementationsNamedAndAnnotated.class, resolved);
        }

        @Test
        @DisplayName("Should resolve an interface with Default qualifier to standard implementation")
        void shouldResolveInterfaceWithDefaultQualifier() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new DefaultLiteral());

            // When
            Class<?> resolved = sut.resolveImplementation(
                    MultipleImplementationsInterface.class,
                    getPackageName(MultipleImplementationsInterface.class),
                    qualifiers
            );

            // Then
            // MultipleImplementationsStandardImplementation has NO qualifiers, so it matches @Default
            assertEquals(MultipleImplementationsStandardImplementation.class, resolved);
        }

        /**
         * We can have multiple implementations without having a standard one, but in this case we must
         * always specify the qualifier, or we will get an exception.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if only alternative implementations found and no qualifier specified")
        void shouldThrowUnsatisfiedResolutionExceptionIfOnlyAlternativeImplementationsFoundAndNoQualifierSpecified() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NamedImplementationsOnlyInterface.class, getPackageName(NamedImplementationsOnlyInterface.class), null));
        }

        /**
         * We can have multiple implementations without having a standard one, but in this case we must
         * always specify the qualifier to get the desired one.
         */
        @Test
        @DisplayName("Should resolve an interface if only alternative implementations found but qualifier specified")
        void shouldResolveInterfaceIfOnlyAlternativeImplementationsFoundButQualifierSpecified() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(NamedImplementationsOnlyInterface.class, getPackageName(NamedImplementationsOnlyInterface.class), qualifiers);
            // Then
            assertEquals(NamedImplementationsOnlyImplementation1.class, resolved);
        }

        /**
         * We can have multiple implementations, but only one can be missing the {@link Named} annotation,
         * or we will get an exception.
         */
        @Test
        @DisplayName("Should throw AmbiguousResolutionException with an interface with more than one standard implementations")
        void shouldThrowAmbiguousResolutionExceptionWithInterfaceWithMoreThanOneStandardImplementations() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(AmbiguousResolutionException.class, () -> sut.resolveImplementation(MultipleNotAnnotatedImplementationsInterface.class, getPackageName(MultipleNotAnnotatedImplementationsInterface.class), null));
        }

        /**
         * If we specify a wrong qualifier (no class exists that is marked with that value for {@link Named}),
         * we will get an UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if specified alternative implementation is not found")
        void shouldThrowUnsatisfiedResolutionExceptionIfAlternateImplementationNotFound() {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("not-found"));
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), qualifiers));
        }

        /**
         * If we have multiple implementations, we can retrieve all of them to subsequently choose the one we want.
         */
        @Test
        @DisplayName("Should return all implementations for a given interface")
        void shouldReturnAllImplementationsForAGivenInterface() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Class<? extends MultipleImplementationsInterface>> expected = new ArrayList<>();
            expected.add(MultipleImplementationsStandardImplementation.class);
            expected.add(MultipleImplementationsNamed1.class);
            expected.add(MultipleImplementationsNamed2.class);
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> classes = sut.resolveImplementations(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class));
            // Then
            assertEquals(4, classes.size());
            assertTrue(classes.containsAll(expected));
        }
    }

    @Nested
    @DisplayName("Class tests")
    class ClassTests {

        /**
         * If we inject a concrete class, it should be resolved by itself. IT will then be injected with all
         * dependencies.
         */
        @Nested
        @DisplayName("A concrete class should be resolved by itself")
        class ConcreteClassShouldBeResolvedByItself {

            @Test
            @DisplayName("resolveImplementation should return the concrete class itself")
            void resolveImplementation() throws Exception {
                // Given
                ClassResolver sut = new ClassResolver();
                // When
                Class<? extends SingleImplementationConcreteClass> resolved = sut.resolveImplementation(SingleImplementationConcreteClass.class, getPackageName(SingleImplementationConcreteClass.class), null);
                // Then
                assertEquals(SingleImplementationConcreteClass.class, resolved);
            }

            @Test
            @DisplayName("resolveImplementations should return the concrete class itself")
            void resolveImplementations() throws Exception {
                // Given
                ClassResolver sut = new ClassResolver();
                // When
                Collection<Class<? extends SingleImplementationConcreteClass>> resolved = sut.resolveImplementations(SingleImplementationConcreteClass.class, getPackageName(SingleImplementationConcreteClass.class));
                // Then
                assertEquals(1, resolved.size());
                assertEquals(SingleImplementationConcreteClass.class, resolved.iterator().next());
            }
        }

        /**
         * If we try to inject an abstract class that has no concrete implementations, we will get an
         * UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if no concrete classes found")
        void shouldThrowUnsatisfiedResolutionExceptionIfNoConcreteClassesFound() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoConcreteClassesAbstractClass.class, getPackageName(NoConcreteClassesAbstractClass.class), null));
        }

        /**
         * If we have a unique implementation, it does not need to be annotated.
         */
        @Test
        @DisplayName("Should resolve an abstract class with a single standard implementation")
        void shouldResolveAnAbstractClassWithStandardImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationAbstractClass.class, getPackageName(SingleImplementationAbstractClass.class), null);
            // Then
            assertEquals(SingleImplementationConcreteClass.class, resolved);
        }

        /**
         * When we have multiple concrete classes, the only one of them not annotated with {@link Named} is
         * considered the standard implementation.
         */        @Test
        @DisplayName("Should resolve an abstract class with multiple implementations with standard implementation")
        void shouldResolveInterfaceWithStandardImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, getPackageName(MultipleConcreteClassesAbstractClass.class), null);
            // Then
            assertEquals(MultipleConcreteClassesStandardClass.class, resolved);
        }

        /**
         * When we have multiple concrete classes, we can specify one of the alternate implementations to be used
         * by specifying the qualifier.
         */
        @Test
        @DisplayName("Should resolve a class with specified alternate implementation")
        void shouldResolveClassWithSpecifiedAlternateImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, getPackageName(MultipleConcreteClassesAbstractClass.class), qualifiers);
            // Then
            assertEquals(MultipleConcreteClassesNamed1.class, resolved);
        }

        /**
         * We can have multiple implementations, but only one can be missing the {@link Named} annotation,
         * or we will get an exception.
         */
        @Test
        @DisplayName("Should throw AmbiguousResolutionException with more than one standard concrete classes")
        void shouldThrowAmbiguousResolutionExceptionWithMoreThanOneStandardConcreteClasses() {
            // Given
            ClassResolver sut = new ClassResolver();
            // Then
            assertThrows(AmbiguousResolutionException.class, () -> sut.resolveImplementation(MultipleNotAnnotatedAbstractClass.class, getPackageName(MultipleNotAnnotatedAbstractClass.class), null));
        }

        /**
         * If we specify a wrong qualifier (no class exists that is marked with that value for {@link Named}),
         * we will get an UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if specified alternative implementation is not found")
        void shouldThrowUnsatisfiedResolutionExceptionIfSpecifiedAlternateImplementationNotFound() {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("not-found"));
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, getPackageName(MultipleConcreteClassesAbstractClass.class), qualifiers));
        }

        /**
         * If we have multiple implementations, we can retrieve all of them to subsequently choose the one we want.
         */
        @Test
        @DisplayName("Should return all concrete classes for a given abstract class")
        void shouldReturnAllConcreteClassesForAGivenAbstractClass() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            Collection<Class<? extends MultipleConcreteClassesAbstractClass>> expected = new ArrayList<>();
            expected.add(MultipleConcreteClassesStandardClass.class);
            expected.add(MultipleConcreteClassesNamed1.class);
            expected.add(MultipleConcreteClassesNamed2.class);
            expected.add(MultipleConcreteClassesNamed3.class);
            // When
            Collection<Class<? extends MultipleConcreteClassesAbstractClass>> classes = sut.resolveImplementations(MultipleConcreteClassesAbstractClass.class, getPackageName(MultipleConcreteClassesAbstractClass.class));
            // Then
            assertEquals(4, classes.size());
            assertTrue(classes.containsAll(expected));
        }
    }

    @Nested
    @DisplayName("Alternatives")
    class Alternatives {

        @Test
        @DisplayName("Should skip inactive alternatives")
        void shouldSkipInactiveAlternatives() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(AlternativesTestInterface.class, getPackageName(AlternativesTestInterface.class), null);
            // Then
            assertEquals(AlternativesTestStandardImplementation.class, resolved);
        }

        @Test
        @DisplayName("Should return enabled alternative")
        void shouldReturnEnabledAlternative() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            sut.enableAlternative(AlternativesTestAlternativeImplementation1.class);
            // When
            Class<?> resolved = sut.resolveImplementation(AlternativesTestInterface.class, getPackageName(AlternativesTestInterface.class), null);
            // Then
            assertEquals(AlternativesTestAlternativeImplementation1.class, resolved);
        }
    }

    @Nested
    @DisplayName("Generic Type Resolution (isAssignable)")
    class GenericTypeResolution {

        @Test
        @DisplayName("Branch 1: Should match implementation of a plain Class (non-generic)")
        void shouldMatchPlainClass() throws Exception {
            ClassResolver sut = new ClassResolver();
            // targetType is a Class, not a ParameterizedType
            Type targetType = SingleImplementationInterface.class;

            Collection<Class<?>> resolved = sut.resolveImplementations(
                    Thread.currentThread().getContextClassLoader(),
                    targetType,
                    getPackageName(SingleImplementationClass.class));

            assertTrue(resolved.contains(SingleImplementationClass.class));
        }

        @Test
        @DisplayName("Branch 2a: Should match implementation of a specific generic interface")
        void shouldMatchGenericInterface() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(Thread.currentThread().getContextClassLoader(), targetType, getPackageName(StringGenericImpl.class));

            assertTrue(resolved.contains(StringGenericImpl.class));
            assertFalse(resolved.contains(IntegerGenericImpl.class));
        }

        @Test
        @DisplayName("Branch 2b: Should match via deep interface hierarchy (recursive)")
        void shouldMatchDeepInterfaceHierarchy() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(Thread.currentThread().getContextClassLoader(), targetType, getPackageName(DeepStringGenericImpl.class));

            assertTrue(resolved.contains(DeepStringGenericImpl.class), "Should find implementation through sub-interface");
        }

        @Test
        @DisplayName("Branch 2c: Should match implementation of a specific generic superclass")
        void shouldMatchGenericSuperclass() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericBase<Double>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(Thread.currentThread().getContextClassLoader(), targetType, getPackageName(DoubleBaseImpl.class));

            assertTrue(resolved.contains(DoubleBaseImpl.class));
        }

        @Test
        @DisplayName("Branch 2d: Should match via deep superclass hierarchy (recursive)")
        void shouldMatchDeepSuperclassHierarchy() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericBase<Double>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(Thread.currentThread().getContextClassLoader(), targetType, getPackageName(DeepDoubleBaseImpl.class));

            assertTrue(resolved.contains(DeepDoubleBaseImpl.class), "Should find implementation through parent's superclass");
        }

        @Test
        @DisplayName("Branch 2 (fail): Should return false if arguments mismatch in hierarchy")
        void shouldFailIfArgumentsMismatch() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericInterface<Integer>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(Thread.currentThread().getContextClassLoader(), targetType, getPackageName(DeepStringGenericImpl.class));

            assertFalse(resolved.contains(DeepStringGenericImpl.class));
        }

        @Test
        @DisplayName("Should match via non-parameterized sub-interface in recursive check")
        void shouldMatchViaRawInterfaceTypeRecursive() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(
                    Thread.currentThread().getContextClassLoader(),
                    targetType,
                    getPackageName(ImplementsSubInterfaceViaNonParameterized.class));

            assertTrue(resolved.contains(ImplementsSubInterfaceViaNonParameterized.class),
                    "Should find implementation through non-parameterized intermediate interface");
        }

        @Test
        @DisplayName("Should return false when superclass is Object")
        void shouldReturnFalseWhenSuperclassIsObject() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(
                    Thread.currentThread().getContextClassLoader(),
                    targetType,
                    getPackageName(NoMatchingClass.class));

            assertFalse(resolved.contains(NoMatchingClass.class),
                    "Should not match class that doesn't implement the interface");
        }

        @Test
        @DisplayName("Should not match raw implementation without type parameters")
        void shouldNotMatchRawImplementation() throws Exception {
            ClassResolver sut = new ClassResolver();
            Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();

            Collection<Class<?>> resolved = sut.resolveImplementations(
                    Thread.currentThread().getContextClassLoader(),
                    targetType,
                    getPackageName(NonGenericImpl.class));

            assertFalse(resolved.contains(NonGenericImpl.class),
                    "Raw implementation should not match parameterized type");
        }

        @Nested
        @DisplayName("Direct isAssignable tests")
        class DirectIsAssignableTests {

            @Test
            @DisplayName("isAssignable should return true for plain Class assignable from candidate")
            void isAssignableShouldReturnTrueForPlainClass() {
                ClassResolver sut = new ClassResolver();
                Type targetType = SingleImplementationInterface.class;
                Class<?> candidate = SingleImplementationClass.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertTrue(result);
            }

            @Test
            @DisplayName("isAssignable should return false for plain Class not assignable from candidate")
            void isAssignableShouldReturnFalseForPlainClass() {
                ClassResolver sut = new ClassResolver();
                Type targetType = SingleImplementationInterface.class;
                Class<?> candidate = MultipleImplementationsStandardImplementation.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertFalse(result);
            }

            @Test
            @DisplayName("isAssignable should return true for matching ParameterizedType via interface")
            void isAssignableShouldReturnTrueForMatchingParameterizedTypeViaInterface() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();
                Class<?> candidate = StringGenericImpl.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertTrue(result);
            }

            @Test
            @DisplayName("isAssignable should return false for mismatched ParameterizedType")
            void isAssignableShouldReturnFalseForMismatchedParameterizedType() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericInterface<Integer>>() {}.getType();
                Class<?> candidate = StringGenericImpl.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertFalse(result);
            }

            @Test
            @DisplayName("isAssignable should return true for matching ParameterizedType via superclass")
            void isAssignableShouldReturnTrueForMatchingParameterizedTypeViaSuperclass() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericBase<Double>>() {}.getType();
                Class<?> candidate = DoubleBaseImpl.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertTrue(result);
            }

            @Test
            @DisplayName("isAssignable should return false when superclass is Object")
            void isAssignableShouldReturnFalseWhenSuperclassIsObject() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();
                Class<?> candidate = NoMatchingClass.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertFalse(result);
            }

            @Test
            @DisplayName("isAssignable should recurse through interface hierarchy with non-parameterized interface")
            void isAssignableShouldRecurseThroughNonParameterizedInterface() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();
                Class<?> candidate = ImplementsSubInterfaceViaNonParameterized.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertTrue(result);
            }

            @Test
            @DisplayName("isAssignable should recurse through superclass hierarchy")
            void isAssignableShouldRecurseThroughSuperclassHierarchy() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericBase<Double>>() {}.getType();
                Class<?> candidate = DeepDoubleBaseImpl.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertTrue(result);
            }

            @Test
            @DisplayName("isAssignable should return false for raw implementation")
            void isAssignableShouldReturnFalseForRawImplementation() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new TypeLiteral<GenericInterface<String>>() {}.getType();
                Class<?> candidate = NonGenericImpl.class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertFalse(result);
            }

            @Test
            @DisplayName("isAssignable should return false for unsupported Type implementations")
            void isAssignableShouldReturnFalseForUnsupportedTypes() {
                ClassResolver sut = new ClassResolver();
                Type targetType = new java.lang.reflect.GenericArrayType() {
                    @Override
                    public @NonNull Type getGenericComponentType() {
                        return String.class;
                    }
                };
                Class<?> candidate = String[].class;

                boolean result = sut.isAssignable(targetType, candidate);

                assertFalse(result, "isAssignable should return false for GenericArrayType as it only supports Class and ParameterizedType");
            }
        }
    }
    // --- Test Data Structures for Generics ---

    @SuppressWarnings("unused")
    interface GenericInterface<T> {}

    interface SubGenericInterface extends GenericInterface<String> {}
    // A non-parameterized interface that extends a parameterized one (tests the raw Class branch in recursion)
    interface NonParameterizedSubInterface extends SubGenericInterface {}
    static class StringGenericImpl implements GenericInterface<String> {}
    static class DeepStringGenericImpl implements SubGenericInterface {}
    static class IntegerGenericImpl implements GenericInterface<Integer> {}
    // Tests recursion through a non-parameterized interface (exercises line 196 with raw Class)
    static class ImplementsSubInterfaceViaNonParameterized implements NonParameterizedSubInterface {}

    // Since it doesn't provide generic info at the class definition level, getGenericInterfaces() returns the raw
    // GenericInterface.class, which is not equal to ParameterizedType of GenericInterface<String>. This fully
    // exercises the negative path of the ParameterizedType logic.
    @SuppressWarnings("rawtypes")
    static class NonGenericImpl implements GenericInterface { } // Raw implementation

    // A class that doesn't implement GenericInterface at all, to test the Object.class early return
    static class NoMatchingClass {}

    @SuppressWarnings("unused")
    abstract static class GenericBase<T> {}

    static class DoubleBaseImpl extends GenericBase<Double> {}
    static class DeepDoubleBaseImpl extends DoubleBaseImpl {}

    // --- End test data structures for Generics ---

    private String getPackageName(Class<?> clazz) {
        return clazz.getPackage().getName();
    }

    /**
     * The following test and the later methods are used to ensure class browsing works when running from a jar file.
     */
    @ParameterizedTest
    @DisplayName("Should resolve implementations from a JAR file")
    @MethodSource("getPackageNamesToFilter")
    void shouldResolveImplementationsFromJar(String packageNameToFilter) throws Exception {
        // 1. Create a dummy JAR file in the temp directory
        Path jarPath = tempDir.resolve("test-classes.jar");
        String packageName = "com.threeamigos.common.util.implementations.injection";
        String baseEntryName = packageName.replace('.', '/');
        createJarFile(jarPath, baseEntryName);

        // 3. Create a custom ClassLoader WITH parent delegation
        URL[] urls = { jarPath.toUri().toURL() };
        // Passing getClass().getClassLoader() as parent is necessary so the loader
        // can find the annotation classes and abstract classes during Class.forName()
        try (URLClassLoader testLoader = createURLClassLoader(urls, packageName, baseEntryName)) {
            ClassResolver sut = new ClassResolver();

            // 4. Resolve using the custom loader
            Class<?> abstractClass = testLoader.loadClass(MultipleConcreteClassesAbstractClass.class.getName());

            Class<?> result = sut.resolveImplementation(testLoader, abstractClass, packageNameToFilter,null);

            // 5. Verification
            assertNotNull(result);
            assertEquals(MultipleConcreteClassesStandardClass.class.getSimpleName(), result.getSimpleName());

            // This should now pass because we are explicitly using the loader that knows about the JAR
            String location = result.getProtectionDomain().getCodeSource().getLocation().toString();
            assertTrue(location.endsWith(".jar"), "Class should be loaded from JAR, but was: " + location);
        }
    }

    static String[] getPackageNamesToFilter() {
        return new String[] { "com.threeamigos.common.util.implementations.injection", "", null };
    }

    @Test
    @DisplayName("Should handle non-standard JAR URLs using fallback logic")
    void shouldHandleNonStandardJarUrl() {
        ClassResolver sut = new ClassResolver();

        // 1. Create a URL string with an unencoded space.
        // new URL(...).toURI() will fail on this string.
        String nonStandardUrl = "jar:file:/C:/My Documents/test.jar!/com/package";

        // 2. Mock the URL object
        URL mockUrl = mock(URL.class);
        when(mockUrl.toString()).thenReturn(nonStandardUrl);

        // 3. Since we want to test the catch block, we don't need the JarFile to actually open.
        // The test will likely throw an IOException later when trying to open "C:/My Documents/test.jar",
        // but we can verify that the code reached the fallback by checking the logs or
        // debugging the File object creation.

        assertThrows(IOException.class, () -> sut.findClassesInJar(Thread.currentThread().getContextClassLoader(),
                    mockUrl, "com.package"));
    }

    private void createJarFile(Path jarPath, String baseEntryName) throws Exception {
        // Note: We are packaging an existing compiled class from the project into this JAR
        // Usually, target/test-classes/... holds these files during test execution
        URL classUrl = getClass().getClassLoader().getResource(baseEntryName);
        assertNotNull(classUrl, "Could not find the compiled class file to package");

        // Find the actual directory on the file system where the test classes are compiled
        URL rootUrl = getClass().getClassLoader().getResource(baseEntryName);
        assertNotNull(rootUrl, "Could not find the test classes directory");
        File packageDir = new File(rootUrl.toURI());

        // 2. Package the entire directory into a JAR
        // at least until I decide not to support Java 8 any longer
        //noinspection IOStreamConstructor
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            addFiles(jos, baseEntryName, packageDir);

            // 2.1 Add a fake test file for testing ClassResolver filters
            jos.putNextEntry(new JarEntry("com/threeamigos/common/utils/fakeFileToSkip"));
            jos.write("This file is used to test ClassResolver filters.".getBytes());
            jos.closeEntry();
        }
    }

    private void addFiles(JarOutputStream jos, String baseEntryName, File dir) throws Exception {
        // First the directory
        jos.putNextEntry(new JarEntry(baseEntryName + "/"));
        jos.closeEntry();
        // Then the contents
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addFiles(jos, baseEntryName + "/" + file.getName(), file);
                } else {
                    String entryName = baseEntryName + "/" + file.getName();
                    jos.putNextEntry(new JarEntry(entryName));
                    Files.copy(file.toPath(), jos);
                    jos.closeEntry();
                }
            }
        }
    }

    private URLClassLoader createURLClassLoader(URL[] urls, String packageName, String baseEntryName) {
        return new URLClassLoader(urls, getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                // If it's one of our test classes, try to load it from the JAR first
                if (name.startsWith(packageName)) {
                    try {
                        return findClass(name);
                    } catch (ClassNotFoundException e) {
                        // ignore, fall through to parent
                    }
                }
                return super.loadClass(name);
            }

            // Also override getResources to ONLY return the JAR resource for this package
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (name.equals(baseEntryName)) {
                    return findResources(name); // Skip parent, only find in JAR
                }
                return super.getResources(name);
            }
        };
    }
}