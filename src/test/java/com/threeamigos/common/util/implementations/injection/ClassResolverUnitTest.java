package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed1;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed2;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.subpackage.MultipleConcreteClassesNamed3;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesAlternativeImplementation1;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesInterface;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesStandardImplementation;
import com.threeamigos.common.util.implementations.injection.bind.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ClassResolver unit tests")
class ClassResolverUnitTest {

    ClassResolver sut;

    Collection<Annotation> qualifier = Collections.singleton(AnnotationLiteral.of(BindingQualifier.class));
    Collection<Annotation> nonMatchingQualifier = Collections.singleton(AnnotationLiteral.of(BindingNotMatchingQualifier.class));

    @BeforeEach
    void setUp() {
        // Given
        sut = new ClassResolver(getPackageName(ClassResolverUnitTest.class));
    }

    @Nested
    @DisplayName("Binding tests")
    class BindingTests {

        @BeforeEach
        void setUp() {
            // Given
            sut.setBindingsOnly(true); // Forces the class resolver NOT to scan for classes, using bound classes only.
        }

        @Nested
        @DisplayName("Binding classes")
        class BindingClasses {

            @ParameterizedTest
            @DisplayName("A class bound with no qualifiers should be resolved with target class when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void classWithNoQualifierShouldResolveWithNoQualifier(Collection<Annotation> noQualifier) throws Exception {
                //Given
                sut.bind(ClassToBind.class, noQualifier, TargetClass.class);
                // When
                Class<?> clazz = sut.resolveImplementation(ClassToBind.class, null);
                assertEquals(TargetClass.class, clazz);
            }

            @ParameterizedTest
            @DisplayName("A class bound with no qualifiers should not be resolved when resolving with any qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void classWithNoQualifierShouldNotResolveWithAnyQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(ClassToBind.class, noQualifier, TargetClass.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(ClassToBind.class, qualifier));
            }

            @ParameterizedTest
            @DisplayName("A class bound with qualifiers should not be resolved when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void classWithQualifierShouldNotResolveWithNoQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(ClassToBind.class, noQualifier));
            }

            @Test
            @DisplayName("A class bound with qualifiers should be resolved with target class when resolving with correct qualifiers")
            void classWithQualifierShouldResolveWithQualifier() throws Exception {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When
                Class<?> clazz = sut.resolveImplementation(ClassToBind.class, qualifier);
                assertEquals(TargetClass.class, clazz);
            }

            @Test
            @DisplayName("A class bound with qualifier should not be resolved when resolving with non-matching qualifiers")
            void classWithQualifierShouldResolveWithNonMatchingQualifier() throws Exception {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(ClassToBind.class, nonMatchingQualifier));
            }
        }

        @Nested
        @DisplayName("Binding interfaces")
        class BindingInterfaces {

            @ParameterizedTest
            @DisplayName("An interface bound with no qualifiers should be resolved with target implementation when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void interfaceWithNoQualifierShouldResolveWithNoQualifier(Collection<Annotation> noQualifier) throws Exception {
                //Given
                sut.bind(InterfaceToBind.class, noQualifier, TargetImplementation.class);
                // When
                Class<?> clazz = sut.resolveImplementation(InterfaceToBind.class, null);
                assertEquals(TargetImplementation.class, clazz);
            }

            @ParameterizedTest
            @DisplayName("An interface bound with no qualifiers should not be resolved when resolving with any qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void interfaceWithNoQualifierShouldNotResolveWithQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(InterfaceToBind.class, noQualifier, TargetImplementation.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(InterfaceToBind.class, qualifier));
            }

            @ParameterizedTest
            @DisplayName("An interface bound with qualifiers should not be resolved when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void interfaceWithQualifierShouldNotResolveWithNoQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(InterfaceToBind.class, noQualifier));
            }

            @Test
            @DisplayName("An interface bound with qualifiers should be resolved with target implementation when resolving with correct qualifiers")
            void interfaceWithQualifierShouldResolveWithQualifier() throws Exception {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When
                Class<?> clazz = sut.resolveImplementation(InterfaceToBind.class, qualifier);
                assertEquals(TargetImplementation.class, clazz);
            }

            @Test
            @DisplayName("An interface bound with qualifier should not be resolved when resolving with non-matching qualifiers")
            void classWithQualifierShouldResolveWithNonMatchingQualifier() throws Exception {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(InterfaceToBind.class, nonMatchingQualifier));
            }
        }
    }

    @Nested
    @DisplayName("Alternatives tests")
    class AlternativesTests {

        @Test
        @DisplayName("If no alternatives are enabled should return the default class")
        void shouldReturnTheDefaultClass() throws Exception{
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);
            // Then
            assertEquals(AlternativesStandardImplementation.class, clazz);
        }

        @Test
        @DisplayName("If alternatives are enabled should return the alternative class with no qualifiers")
        void shouldReturnTheAlternativeClassWithNoQualifiers() throws Exception{
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);
            // Then
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }

        @Test
        @DisplayName("If alternatives are enabled should return the alternative class with any qualifiers")
        void shouldReturnTheAlternativeClassWithAnyQualifiers() throws Exception{
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, qualifier);
            // Then
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }
    }

    @Nested
    @DisplayName("Class tests")
    class ClassTests {

        @Nested
        @DisplayName("A concrete class should be resolved by itself")
        class ConcreteClassShouldBeResolvedByItself {

            @Test
            @DisplayName("resolveImplementation should return the concrete class itself")
            void resolveImplementation() throws Exception {
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationConcreteClass.class, null);
                // Then
                assertEquals(SingleImplementationConcreteClass.class, resolved);
            }

            @Test
            @DisplayName("resolveImplementations should return the concrete class itself")
            void resolveImplementations() throws Exception {
                // When
                Collection<Class<?>> resolved = sut.resolveImplementations(SingleImplementationConcreteClass.class);
                // Then
                assertEquals(1, resolved.size());
                assertEquals(SingleImplementationConcreteClass.class, resolved.iterator().next());
            }
        }

        @Test
        @DisplayName("When resolving an abstract class should throw UnsatisfiedResolutionException if no concrete classes are found")
        void shouldThrowUnsatisfiedResolutionExceptionIfNoConcreteClassesFound() {
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoConcreteClassesAbstractClass.class, null));
        }

        /**
         * If we have a unique implementation, it does not need to be annotated.
         */
        @Test
        @DisplayName("Should resolve an abstract class with a single standard implementation")
        void shouldResolveAnAbstractClassWithStandardImplementation() throws Exception {
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationAbstractClass.class, null);
            // Then
            assertEquals(SingleImplementationConcreteClass.class, resolved);
        }

        /**
         * When we have multiple concrete classes, the only one of them not annotated with {@link Named} is
         * considered the standard implementation.
         */
        @Test
        @DisplayName("Should resolve an abstract class with multiple implementations with standard implementation")
        void shouldResolveInterfaceWithStandardImplementation() throws Exception {
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, null);
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
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, qualifiers);
            // Then
            assertEquals(MultipleConcreteClassesNamed1.class, resolved);
        }

        /**
         * We can have multiple implementations, but only one can be missing the {@link Named} annotation,
         * or we will get an exception.
         */
        @Test
        @DisplayName("Should throw AmbiguousResolutionException if more than one standard concrete classes are found")
        void shouldThrowAmbiguousResolutionExceptionWithMoreThanOneStandardConcreteClasses() {
            // When / Then
            assertThrows(AmbiguousResolutionException.class, () -> sut.resolveImplementation(MultipleNotAnnotatedAbstractClass.class, null));
        }

        /**
         * If we specify a wrong qualifier (no class exists that is marked with that value for {@link Named}),
         * we will get an UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if specified alternative implementation is not found")
        void shouldThrowUnsatisfiedResolutionExceptionIfSpecifiedAlternateImplementationNotFound() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("not-found"));
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, qualifiers));
        }

        /**
         * If we have multiple implementations, we can retrieve all of them to subsequently choose the one we want.
         */
        @Test
        @DisplayName("Should return all concrete classes for a given abstract class")
        void shouldReturnAllConcreteClassesForAGivenAbstractClass() throws Exception {
            // Given
            Collection<Class<? extends MultipleConcreteClassesAbstractClass>> expected = new ArrayList<>();
            expected.add(MultipleConcreteClassesStandardClass.class);
            expected.add(MultipleConcreteClassesNamed1.class);
            expected.add(MultipleConcreteClassesNamed2.class);
            expected.add(MultipleConcreteClassesNamed3.class);
            // When
            Collection<Class<? extends MultipleConcreteClassesAbstractClass>> classes = sut.resolveImplementations(MultipleConcreteClassesAbstractClass.class);
            // Then
            assertEquals(4, classes.size());
            assertTrue(classes.containsAll(expected));
        }
    }

    @Nested
    @DisplayName("Interface tests")
    class InterfaceTests {

        @ParameterizedTest
        @DisplayName("Should throw UnsatisfiedResolutionException if no implementations are found")
        @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
        void shouldThrowUnsatisfiedResolutionExceptionIfNoImplementationsFound(Collection<Annotation> noQualifier) {
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoImplementationsInterface.class, noQualifier));
        }

        /**
         * If we have a unique implementation, it does not need to be annotated.
         */
        @Test
        @DisplayName("Should resolve an interface with a single standard implementation")
        void shouldResolveInterfaceWithSingleStandardImplementation() throws Exception {
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null);
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
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, null);
            // Then
            assertEquals(MultipleImplementationsStandardImplementation.class, resolved);
        }

        /**
         * When we have multiple implementations, we can specify one of the alternative implementations to be used
         * by specifying the qualifier.
         */
        @Test
        @DisplayName("Should resolve an interface with specified qualifier")
        void shouldResolveInterfaceWithSpecifiedQualifier() throws Exception {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(MultipleImplementationsNamed1.class, resolved);
        }

        /**
         * When we have multiple implementations, we can specify one of the alternative implementations to be used
         * by specifying the qualifier (or the qualifiers).
         */
        @Test
        @DisplayName("Should resolve an interface with qualifier-annotated implementation")
        void shouldResolveInterfaceWithSpecifiedQualifierImplementation() throws Exception {
            // Given
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
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(MultipleImplementationsNamedAndAnnotated.class, resolved);
        }

        @Test
        @DisplayName("Should resolve an interface with Default qualifier to standard implementation")
        void shouldResolveInterfaceWithDefaultQualifier() throws Exception {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new DefaultLiteral());
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers);
            // Then
            // MultipleImplementationsStandardImplementation has NO qualifiers, so it matches @Default
            assertEquals(MultipleImplementationsStandardImplementation.class, resolved);
        }

        /**
         * We can have multiple implementations without having a standard one, but in this case we must
         * always specify the qualifier, or we will get an exception.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if only qualified implementations found and no qualifier specified")
        void shouldThrowUnsatisfiedResolutionExceptionIfOnlyQualifiedImplementationsFoundAndNoQualifierSpecified() {
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NamedImplementationsOnlyInterface.class, null));
        }

        /**
         * We can have multiple implementations without having a standard one, but in this case we must
         * always specify the qualifier to get the desired one.
         */
        @Test
        @DisplayName("Should resolve an interface if only qualified implementations found but qualifier specified")
        void shouldResolveInterfaceIfOnlyQualifiedImplementationsFoundButQualifierSpecified() throws Exception {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(NamedImplementationsOnlyInterface.class, qualifiers);
            // Then
            assertEquals(NamedImplementationsOnlyImplementation1.class, resolved);
        }

        /**
         * We can have multiple implementations, but only one can be missing a qualifier, or we will get an exception.
         */
        @Test
        @DisplayName("Should throw AmbiguousResolutionException if an interface has more than one non-qualified implementations")
        void shouldThrowAmbiguousResolutionExceptionWithInterfaceWithMoreThanOneNonqualifiedImplementations() {
            // When / Then
            assertThrows(AmbiguousResolutionException.class, () -> sut.resolveImplementation(MultipleNotAnnotatedImplementationsInterface.class, null));
        }

        /**
         * If we specify a wrong qualifier (no class exists that is marked with that value for {@link Named}),
         * we will get an UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if specified named implementation is not found")
        void shouldThrowUnsatisfiedResolutionExceptionIfAlternateImplementationNotFound() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("not-found"));
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers));
        }

        /**
         * If we have multiple implementations, we can retrieve all of them to subsequently choose the one we want.
         */
        @Test
        @DisplayName("Should return all implementations for a given interface")
        void shouldReturnAllImplementationsForAGivenInterface() throws Exception {
            // Given
            Collection<Class<? extends MultipleImplementationsInterface>> expected = new ArrayList<>();
            expected.add(MultipleImplementationsStandardImplementation.class);
            expected.add(MultipleImplementationsNamed1.class);
            expected.add(MultipleImplementationsNamed2.class);
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> classes = sut.resolveImplementations(MultipleImplementationsInterface.class);
            // Then
            assertEquals(5, classes.size());
            assertTrue(classes.containsAll(expected));
        }
    }

    @Nested
    @DisplayName("Qualifiers")
    class ResolveImplementationsWithQualifiers {

        @Test
        @DisplayName("Should return all active classes if qualifiers are null")
        void shouldReturnAllActiveClassesIfQualifiersAreNull() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver(getPackageName(MultipleImplementationsInterface.class));
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, null);
            // Then
            // Expects 4: Standard, Named1, Named2, NamedAndAnnotated. Alternatives are filtered out by default if not enabled.
            assertEquals(4, resolved.size());
        }

        @Test
        @DisplayName("Should return only matching classes for specific qualifier")
        void shouldReturnOnlyMatchingClassesForSpecificQualifier() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver(getPackageName(MultipleImplementationsInterface.class));
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(1, resolved.size());
            assertTrue(resolved.contains(MultipleImplementationsNamed1.class));
        }

        @Test
        @DisplayName("Should return all classes when @Any qualifier is used")
        void shouldReturnAllClassesWhenAnyQualifierIsUsed() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver(getPackageName(MultipleImplementationsInterface.class));
            Collection<Annotation> qualifiers = Collections.singletonList(new Any() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Any.class;
                }
            });
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(4, resolved.size());
        }
    }

    @Nested
    @DisplayName("Class retrieving tests")
    class ClassRetrievingTests {

        @Nested
        @DisplayName("When package is wrong")
        class WrongPackage {

            @Test
            @DisplayName("Should throw UnsatisfiedResolutionException if package to search is a file")
            void shouldThrowUnsatisfiedResolutionExceptionIfPackageToSearchIsAFile() {
                // Given
                ClassResolver sut = new ClassResolver("com.threeamigos.common.util.implementations.injection.wrongdirectory.fakeFileToSkip");
                // Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoImplementationsInterface.class, null));
            }

            @Test
            @DisplayName("Should throw UnsatisfiedResolutionException if package to search does not exist")
            void shouldThrowUnsatisfiedResolutionExceptionIfPackageToSearchDoesNotExist() {
                // Given
                ClassResolver sut = new ClassResolver("com.threeamigos.common.util.implementations.injection.notexistingpackage");
                // Then
                assertThrows(UnsatisfiedResolutionException.class, () ->sut.resolveImplementation(NoImplementationsInterface.class, null));
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

        @Nested
        @DisplayName("When package is empty or null")
        class NullOrEmptyPackage {

            @Test
            @DisplayName("Should work if package is null")
            void shouldFindALotOfClassesIfPackageIsNull() throws Exception {
                // Given
                ClassResolver sut = new ClassResolver();
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class,  null);
                // Then
                assertEquals(SingleImplementationClass.class, resolved);
            }

            @Test
            @DisplayName("Should work if package is empty")
            void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
                // Given
                ClassResolver sut = new ClassResolver();
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null);
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
                ClassResolver sut = new ClassResolver(getPackageName(SingleImplementationInterface.class));
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null);
                // Then
                assertEquals(SingleImplementationClass.class, resolved);
            }

            @Test
            @DisplayName("Should work if qualifiers collection is empty")
            void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
                // Given
                ClassResolver sut = new ClassResolver();
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, Collections.emptyList());
                // Then
                assertEquals(SingleImplementationClass.class, resolved);
            }

        }

        @Nested
        @DisplayName("Should resolve classes from a JAR file")
        class JARFileTests {

            @ParameterizedTest
            @DisplayName("Should resolve implementations from a JAR file")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#getPackageNamesToFilter")
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
                    ClassResolver sut = new ClassResolver(packageNameToFilter);

                    // 4. Resolve using the custom loader
                    Class<?> abstractClass = testLoader.loadClass(MultipleConcreteClassesAbstractClass.class.getName());

                    Class<?> result = sut.resolveImplementation(testLoader, abstractClass, null);

                    // 5. Verification
                    assertNotNull(result);
                    assertEquals(MultipleConcreteClassesStandardClass.class.getSimpleName(), result.getSimpleName());

                    // This should now pass because we are explicitly using the loader that knows about the JAR
                    String location = result.getProtectionDomain().getCodeSource().getLocation().toString();
                    assertTrue(location.endsWith(".jar"), "Class should be loaded from JAR, but was: " + location);
                }
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

        @Test
        @DisplayName("Should use the cache to avoid redundant class loading")
        void shouldUseTheCache() throws Exception {
            // Given
            String packageName = "com.threeamigos";
            ClassResolver sut = new ClassResolver(packageName);
            ClassLoader mockLoader = mock(ClassLoader.class);
            String expectedPath = "com/threeamigos";

            // Stub getResources to return an empty enumeration so the loop finishes
            when(mockLoader.getResources(expectedPath)).thenReturn(Collections.emptyEnumeration());

            // When - calling the public method that internally calls getClasses
            sut.resolveImplementations(mockLoader, SingleImplementationInterface.class);
            sut.resolveImplementations(mockLoader, MultipleImplementationsInterface.class);

            // Then - Verify the ClassLoader was queried exactly once
            verify(mockLoader, times(1)).getResources(expectedPath);
        }

        @Test
        @DisplayName("Should remember already resolved classes")
        void shouldRememberAlreadyResolvedClasses() throws Exception {
            // Given
            String packageName = "com.threeamigos";
            ClassResolver sut = new ClassResolver(packageName);
            ClassLoader mockLoader = mock(ClassLoader.class);
            String expectedPath = "com/threeamigos";

            // Stub getResources to return an empty enumeration so the loop finishes
            when(mockLoader.getResources(expectedPath)).thenReturn(Collections.emptyEnumeration());

            // When - calling the public method that internally calls getClasses
            sut.resolveImplementations(mockLoader, SingleImplementationInterface.class);
            sut.resolveImplementations(mockLoader, SingleImplementationInterface.class);

            // Then - Verify the ClassLoader was queried exactly once
            verify(mockLoader, times(1)).getResources(expectedPath);
        }
    }

    @Nested
    @DisplayName("Class assignability tests")
    class ClassAssignabilityTests {

        @Nested
        @DisplayName("typeArgsMatch tests")
        class TypeArgsMatchTests {

            @Test
            @DisplayName("Should match identical classes")
            void shouldMatchIdenticalClasses() {
                ClassResolver sut = new ClassResolver();
                assertTrue(sut.typeArgsMatch(String.class, String.class));
            }

            @Test
            @DisplayName("Should match assignable classes (Integer to Number)")
            void shouldMatchAssignableClasses() {
                ClassResolver sut = new ClassResolver();
                // targetType.isAssignableFrom(candidate)
                assertTrue(sut.typeArgsMatch(Number.class, Integer.class));
            }

            @Test
            @DisplayName("Should not match unassignable classes")
            void shouldNotMatchUnassignableClasses() {
                ClassResolver sut = new ClassResolver();
                assertFalse(sut.typeArgsMatch(Integer.class, String.class));
            }

            @Test
            @DisplayName("Should match if either is a WildcardType")
            void shouldMatchWildcards() throws Exception {
                ClassResolver sut = new ClassResolver();
                abstract class TypeHelper<T> {
                    List<?> wildcardField;
                }
                Field field = TypeHelper.class.getDeclaredField("wildcardField");
                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                Type wildcard = pt.getActualTypeArguments()[0];

                assertTrue(sut.typeArgsMatch(wildcard, String.class));
                assertTrue(sut.typeArgsMatch(String.class, wildcard));
            }

            @Test
            @DisplayName("Should match if either is a TypeVariable")
            void shouldMatchTypeVariables() throws Exception {
                ClassResolver sut = new ClassResolver();
                abstract class TypeHelper<T> {
                    T typeVariableField;
                }
                Type typeVar = TypeHelper.class.getDeclaredField("typeVariableField").getGenericType();

                assertTrue(sut.typeArgsMatch(typeVar, String.class));
                assertTrue(sut.typeArgsMatch(String.class, typeVar));
            }

            @Test
            @DisplayName("Should match ParameterizedTypes with matching arguments")
            void shouldMatchParameterizedTypes() {
                ClassResolver sut = new ClassResolver();
                Type listString = new TypeLiteral<List<String>>() {
                }.getType();
                Type arrayListString = new TypeLiteral<ArrayList<String>>() {
                }.getType();

                // Raw types: List.class.isAssignableFrom(ArrayList.class)
                assertTrue(sut.typeArgsMatch(listString, arrayListString));
            }
        }

        @Nested
        @DisplayName("typesMatch tests")
        class TypesMatchTests {

            @Test
            @DisplayName("Should match exactly equal types")
            void shouldMatchEqualTypes() {
                ClassResolver sut = new ClassResolver();
                assertTrue(sut.typesMatch(String.class, String.class));

                Type listString = new TypeLiteral<List<String>>() {
                }.getType();
                assertTrue(sut.typesMatch(listString, listString));
            }

            @Test
            @DisplayName("Should match if target is a TypeVariable")
            void shouldMatchTargetTypeVariable() throws Exception {
                ClassResolver sut = new ClassResolver();
                abstract class TypeHelper<T> {
                    T typeVariableField;
                }
                Type typeVar = TypeHelper.class.getDeclaredField("typeVariableField").getGenericType();

                // T matches String.class
                assertTrue(sut.typesMatch(typeVar, String.class));
            }

            @Test
            @DisplayName("Should not match ParameterizedTypes with non-ParameterizedTypes")
            void shouldNotMatchParameterizedTypesWithNonParameterizedTypes() {
                ClassResolver sut = new ClassResolver();
                Type listNumber = new TypeLiteral<List<Number>>() {
                }.getType();

                assertFalse(sut.typesMatch(listNumber, Integer.class));
            }

            @Test
            @DisplayName("Should match ParameterizedTypes with matching raw types and compatible arguments")
            void shouldMatchCompatibleParameterizedTypes() {
                ClassResolver sut = new ClassResolver();
                Type listNumber = new TypeLiteral<List<Number>>() {
                }.getType();
                Type listInteger = new TypeLiteral<List<Integer>>() {
                }.getType();

                // List is the same raw type, and Number is assignable from Integer (via typeArgsMatch)
                assertTrue(sut.typesMatch(listNumber, listInteger));
            }

            @Test
            @DisplayName("Should not match ParameterizedTypes with different raw types")
            void shouldNotMatchDifferentRawTypes() {
                ClassResolver sut = new ClassResolver();
                Type listString = new TypeLiteral<List<String>>() {
                }.getType();
                Type setString = new TypeLiteral<Set<String>>() {
                }.getType();

                assertFalse(sut.typesMatch(listString, setString));
            }

            @Test
            @DisplayName("Should not match if argument count differs for the same raw type")
            @SuppressWarnings("NullableProblems")
            void shouldNotMatchDifferentArgumentCount() {
                ClassResolver sut = new ClassResolver();

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

                // Raw types match (List.class), but argument counts (1 vs 2) do not.
                assertFalse(sut.typesMatch(typeWithOneArg, typeWithTwoArgs));
            }

            @Test
            @DisplayName("Should not match if any type argument fails to match")
            void shouldNotMatchIfArgumentMismatch() {
                ClassResolver sut = new ClassResolver();
                Type listString = new TypeLiteral<List<String>>() {
                }.getType();
                Type listInteger = new TypeLiteral<List<Integer>>() {
                }.getType();

                assertFalse(sut.typesMatch(listString, listInteger));
            }

            @Test
            @DisplayName("Should return false for non-parameterized mismatches")
            void shouldReturnFalseForGeneralMismatch() {
                ClassResolver sut = new ClassResolver();
                assertFalse(sut.typesMatch(String.class, Integer.class));
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
                    ClassResolver sut = new ClassResolver();
                    assertTrue(sut.isAssignable(Number.class, Integer.class));
                }

                @Test
                @DisplayName("Candidate is NOT assignable")
                void testIsAssignableClassFalse() {
                    ClassResolver sut = new ClassResolver();
                    assertFalse(sut.isAssignable(Integer.class, String.class));
                }
            }

            @Nested
            @DisplayName("ParameterizedType target type tests")
            class ParameterizedTypeTargetTests {

                @Test
                @DisplayName("Raw type NOT assignable")
                void testIsAssignableParameterizedRawTypeMismatch() {
                    ClassResolver sut = new ClassResolver();
                    Type target = new TypeLiteral<List<String>>() {
                    }.getType();
                    assertFalse(sut.isAssignable(target, String.class));
                }

                @Test
                @DisplayName("Match via interface")
                void testIsAssignableParameterizedInterfaceMatch() {
                    ClassResolver sut = new ClassResolver();
                    Type target = new TypeLiteral<List<String>>() {
                    }.getType();
                    // java.util.ArrayList implements java.util.List<E>
                    assertTrue(sut.isAssignable(target, java.util.ArrayList.class));
                }

                @Test
                @DisplayName("Supertype null")
                void testSuperTypeNull() {
                    ClassResolver sut = new ClassResolver();
                    Type target = new TypeLiteral<List<String>>() {
                    }.getType();
                    // Candidate is an interface, getGenericSuperclass() returns null
                    assertFalse(sut.isAssignable(target, List.class));
                }

                @Test
                @DisplayName("SuperType is Object.class")
                @SuppressWarnings("rawtypes")
                void testSuperTypeIsObject() {
                    ClassResolver sut = new ClassResolver();
                    // Using a class that doesn't match generic interfaces:
                    class RawImplementation implements MyGeneric {
                    }
                    Type targetWithArgs = new TypeLiteral<MyGeneric<String>>() {
                    }.getType();
                    assertFalse(sut.isAssignable(targetWithArgs, RawImplementation.class));
                }

                @Test
                @DisplayName("Match via superclass")
                void testIsAssignableParameterizedSuperclassMatch() {
                    ClassResolver sut = new ClassResolver();
                    class StringList extends java.util.ArrayList<String> {
                    }
                    Type target = new TypeLiteral<java.util.AbstractList<String>>() {
                    }.getType();
                    assertTrue(sut.isAssignable(target, StringList.class));
                }

                @Test
                @DisplayName("Recursion in superclass")
                void testIsAssignableParameterizedSuperclassRecursion() {
                    ClassResolver sut = new ClassResolver();
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
                    ClassResolver sut = new ClassResolver();
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
                    ClassResolver sut = new ClassResolver();
                    class Helper<T extends Number & Comparable<T>> {
                        T field;
                    }
                    Type target = Helper.class.getDeclaredField("field").getGenericType();
                    assertTrue(sut.isAssignable(target, Integer.class));
                }

                @Test
                @DisplayName("One bound fails")
                void testIsAssignableTypeVariableFalse() throws NoSuchFieldException {
                    ClassResolver sut = new ClassResolver();
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
                    ClassResolver sut = new ClassResolver();
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
                    ClassResolver sut = new ClassResolver();
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
                    ClassResolver sut = new ClassResolver();
                    class Helper<T> {
                        T[] field;
                    }
                    Type target = Helper.class.getDeclaredField("field").getGenericType(); // T[]
                    assertTrue(sut.isAssignable(target, Object[].class));
                }

                @Test
                @DisplayName("Candidate is NOT array")
                void testIsAssignableGenericArrayFalseNotArray() throws NoSuchFieldException {
                    ClassResolver sut = new ClassResolver();
                    class Helper<T> {
                        T[] field;
                    }
                    Type target = Helper.class.getDeclaredField("field").getGenericType();
                    assertFalse(sut.isAssignable(target, Object.class));
                }

                @Test
                @DisplayName("Component mismatch")
                void testIsAssignableGenericArrayComponentMismatch() throws NoSuchFieldException {
                    ClassResolver sut = new ClassResolver();
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
                ClassResolver sut = new ClassResolver();
                // Custom type that is neither Class, ParameterizedType, Variable, Wildcard, nor Array
                Type weirdType = new Type() {
                    @Override
                    public String getTypeName() {
                        return "weird";
                    }
                };
                assertFalse(sut.isAssignable(weirdType, Object.class));
            }
        }
    }

    @TempDir
    Path tempDir;

    private String getPackageName(Class<?> clazz) {
        return clazz.getPackage().getName();
    }

    static String[] getPackageNamesToFilter() {
        return new String[] { "com.threeamigos.common.util.implementations.injection", "", null };
    }

    static Stream<Arguments> noQualifiers() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(Collections.emptyList()));
    }

    // Test structures
    interface MyGeneric<T> {}

}