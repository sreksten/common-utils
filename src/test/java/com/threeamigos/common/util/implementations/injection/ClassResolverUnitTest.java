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
import com.threeamigos.common.util.implementations.injection.interfaces.namedimplementationsonly.NamedImplementationsOnlyImplementation1;
import com.threeamigos.common.util.implementations.injection.interfaces.namedimplementationsonly.NamedImplementationsOnlyInterface;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsNamed1;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsNamed2;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsStandardImplementation;
import com.threeamigos.common.util.implementations.injection.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.interfaces.noimplementations.NoImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multiplenotannotatedconcreteclasses.MultipleNotAnnotatedAbstractClass;
import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationClass;
import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationInterface;
import com.threeamigos.common.util.implementations.injection.interfaces.multiplenotannotatedimplementations.MultipleNotAnnotatedImplementationsInterface;
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
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
    @DisplayName("Wrong parameters")
    class WrongPaths {

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
        @DisplayName("Should resolve an interface with specified alternate implementation")
        void shouldResolveInterfaceWithSpecifiedAlternateImplementation() throws Exception {
            // Given
            ClassResolver sut = new ClassResolver();
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), new NamedLiteral("name1"));
            // Then
            assertEquals(MultipleImplementationsNamed1.class, resolved);
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
            // When
            Class<?> resolved = sut.resolveImplementation(NamedImplementationsOnlyInterface.class, getPackageName(NamedImplementationsOnlyInterface.class), new NamedLiteral("name1"));
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
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleImplementationsInterface.class, getPackageName(MultipleImplementationsInterface.class), new NamedLiteral("not-found")));
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
            assertEquals(3, classes.size());
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
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, getPackageName(MultipleConcreteClassesAbstractClass.class), new NamedLiteral("name1"));
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
            // Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, getPackageName(MultipleConcreteClassesAbstractClass.class), new NamedLiteral("not-found")));
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

    /**
     * Helper class to create instances of @Named for testing.
     */
    private static class NamedLiteral implements Named {
        private final String value;

        public NamedLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Named.class;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Named)) return false;
            Named other = (Named) o;
            return value.equals(other.value());
        }

        @Override
        public int hashCode() {
            return (127 * "value".hashCode()) ^ value.hashCode();
        }
    }

}