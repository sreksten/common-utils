package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@DisplayName("ClasspathScanner unit test")
class ClasspathScannerUnitTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("When package is wrong")
    class WrongPackage {

        @Test
        @DisplayName("Should return an empty collection if package to search does not exist")
        void shouldReturnEmptyCollectionIfPackageToSearchDoesNotExist() throws IOException, ClassNotFoundException {
            // Given
            ClasspathScanner sut = new ClasspathScanner("com.threeamigos.common.util.implementations.injection.notexistingpackage");
            // Then
            assertTrue(sut.getAllClasses(Thread.currentThread().getContextClassLoader()).isEmpty());
        }

        @Test
        @DisplayName("Should return an empty collection if package to search is a file")
        void shouldThrowIllegalArgumentExceptionIfPackageToSearchIsAFile() throws IOException, ClassNotFoundException {
            // Given
            ClasspathScanner sut = new ClasspathScanner("com.threeamigos.common.util.implementations.injection.wrongdirectory.fakeFileToSkip");
            // Then
            assertTrue(sut.getAllClasses(Thread.currentThread().getContextClassLoader()).isEmpty());
        }

        @Test
        @DisplayName("getClassesFromResource should return an empty collection if protocol is not recognized")
        void getClassesFromResourceShouldReturnAnEmptyCollectionIfProtocolNotRecognized() throws Exception {
            // Given
            ClasspathScanner sut = new ClasspathScanner();
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
            ClasspathScanner sut = new ClasspathScanner();
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
            ClasspathScanner sut = new ClasspathScanner();
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
            ClasspathScanner sut = new ClasspathScanner((String) null);
            // When
            Collection<Class<?>> classes = sut.getAllClasses(Thread.currentThread().getContextClassLoader());
            // Then
            assertTrue(classes.contains(SingleImplementationClass.class));
        }

        @Test
        @DisplayName("Should work if package is empty")
        void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
            // Given
            ClasspathScanner sut = new ClasspathScanner("");
            // When
            Collection<Class<?>> classes = sut.getAllClasses(Thread.currentThread().getContextClassLoader());
            // Then
            assertTrue(classes.contains(SingleImplementationClass.class));
        }
    }

    @Nested
    @DisplayName("Should find classes in a JAR file")
    class JARFileTests {

        @ParameterizedTest
        @DisplayName("Should find classes in a JAR file")
        @MethodSource("com.threeamigos.common.util.implementations.injection.ClasspathScannerUnitTest#getPackageNamesToFilter")
        void shouldFindClassesInJar(String packageNameToFilter) throws Exception {
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
                ClasspathScanner sut = new ClasspathScanner(packageNameToFilter);
                Collection<Class<?>> classes = sut.getAllClasses(testLoader);
                String classNameToFind = SingleImplementationClass.class.getSimpleName();
                Class<?> result = classes.stream().filter(c -> c.getSimpleName().equals(classNameToFind)).findFirst().orElse(null);

                assertNotNull(result);

                // This should now pass because we are explicitly using the loader that knows about the JAR
                String location = result.getProtectionDomain().getCodeSource().getLocation().toString();
                assertTrue(location.endsWith(".jar"), "Class should be loaded from JAR, but was: " + location);
            }
        }

        @Test
        @DisplayName("Should handle non-standard JAR URLs using fallback logic")
        void shouldHandleNonStandardJarUrl() {
            ClasspathScanner sut = new ClasspathScanner();

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

            // Package the entire directory into a JAR
            // at least until I decide not to support Java 8 any longer:
            //noinspection IOStreamConstructor
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
                addFiles(jos, baseEntryName, packageDir);

                // Add a fake test file for testing ClasspathScanner filters
                jos.putNextEntry(new JarEntry("com/threeamigos/common/utils/fakeFileToSkip"));
                jos.write("This file is used to test ClasspathScanner filters.".getBytes());
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
        ClasspathScanner sut = new ClasspathScanner(packageName);
        ClassLoader mockLoader = mock(ClassLoader.class);
        String expectedPath = "com/threeamigos";

        // Stub getResources to return an empty enumeration so the loop finishes
        when(mockLoader.getResources(expectedPath)).thenReturn(Collections.emptyEnumeration());

        // When - calling the public method that internally calls getClasses
        sut.getAllClasses(mockLoader);
        sut.getAllClasses(mockLoader);

        // Then - Verify the getResources method was queried exactly once
        verify(mockLoader, times(1)).getResources(expectedPath);
    }

    static String[] getPackageNamesToFilter() {
        return new String[] { "com.threeamigos.common.util.implementations.injection", "", null };
    }

}