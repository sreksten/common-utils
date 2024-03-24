package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.persistence.file.FileUtils;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;

import static com.threeamigos.common.util.implementations.persistence.file.FileUtils.applyFileAttributes;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RootPathProviderImpl unit test")
@Tag("unit")
@Tag("persistence")
public class RootPathProviderUnitTest {

    private InMemoryMessageHandler messageHandler;
    private File temporaryDirectory;

    @BeforeEach
    void setup(@TempDir File temporaryDirectory) {
        messageHandler = new InMemoryMessageHandler();
        this.temporaryDirectory = temporaryDirectory;
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER);
    }

    @Nested
    @DisplayName("Null constructor parameters")
    class NullConstructorParameters {
        @Test
        @DisplayName("Should throw NullObjectException when object parameter is null")
        void shouldThrowsExceptionWhenObjectParameterIsNull() {
            assertThrows(NullObjectException.class, () -> new RootPathProviderImpl((Object) null, messageHandler));
        }

        @Test
        @DisplayName("Should throw NullClassException when class parameter is null")
        void shouldThrowExceptionWhenClassIsNull() {
            assertThrows(NullClassException.class, () -> new RootPathProviderImpl((Class<?>) null, messageHandler));
        }

        @Test
        @DisplayName("Should throw NullExceptionHandlerException when exceptionHandler parameter is null")
        void shouldThrowsExceptionWhenExceptionHandlerIsNull() {
            assertThrows(NullExceptionHandlerException.class, () -> new RootPathProviderImpl(this, null));
        }
    }

    @Nested
    @DisplayName("Should have unrecoverable errors for an invalid class")
    class UnrecoverableErrors {
        @Test
        @DisplayName("For a basic type class")
        void forBasicType() {
            // Given
            Class<?> clazz = int.class;
            // When
            RootPathProvider sut = new RootPathProviderImpl(clazz, messageHandler);
            // Then
            assertAll("Header",
                    () -> assertFalse(sut.isRootPathAccessible()),
                    () -> assertNull(sut.getRootPath()),
                    () -> assertTrue(sut.hasUnrecoverableErrors())
            );
        }

        @Test
        @DisplayName("For an array")
        void forArrayType() {
            // Given
            Class<?> clazz = int[].class;
            // When
            RootPathProvider sut = new RootPathProviderImpl(clazz, messageHandler);
            // Then
            assertFalse(sut.isRootPathAccessible());
            assertNull(sut.getRootPath());
            assertTrue(sut.hasUnrecoverableErrors());
        }

        @Test
        @DisplayName("For an anonymous class")
        void forAnonymousClass() {
            // Given
            Class<?> anonymousClass = new WindowAdapter() {
            }.getClass();
            // When
            RootPathProviderImpl sut = new RootPathProviderImpl(anonymousClass, messageHandler);
            // Then
            assertFalse(sut.isRootPathAccessible());
            assertNull(sut.getRootPath());
            assertTrue(sut.hasUnrecoverableErrors());
        }
    }

    @Nested
    @DisplayName("Should output a warning for an invalid class")
    class InvalidClass {
        @ParameterizedTest
        @DisplayName("int.class, int[].class")
        @ValueSource(classes = {int.class, int[].class})
        void shouldOutputWarningForInvalidClass(Class<?> testClass) {
            // Given
            // the test class
            // When
            new RootPathProviderImpl(testClass, messageHandler);
            // Then
            assertEquals(1, messageHandler.getAllExceptions().size());
            assertEquals(NoPackageException.class, messageHandler.getAllExceptions().getFirst().getClass());
        }

        @Test
        @DisplayName("anonymous class")
        void shouldOutputWarningForInvalidClass() {
            // Given
            Class<?> testClass = new WindowAdapter() {
            }.getClass();
            // When
            new RootPathProviderImpl(testClass, messageHandler);
            // Then
            assertEquals(1, messageHandler.getAllExceptions().size());
            assertEquals(NoCanonicalNameException.class, messageHandler.getAllExceptions().getFirst().getClass());
        }
    }

    @Nested
    @DisplayName("Root path")
    class UserHome {
        @Test
        @DisplayName("Should use user's home directory when no " + RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER + " is set")
        void shouldUseUsersHomeWhenNoParameterIsSet() {
            // Given
            RootPathProvider rootPathProvider;
            synchronized (System.getProperties()) {
                System.clearProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER);
                rootPathProvider = new RootPathProviderImpl(this.getClass(), messageHandler);
            }
            // When
            String rootPath = rootPathProvider.getRootPath();
            // Then
            assertNotNull(rootPath, "Root path should not be null");
            assertTrue(rootPath.startsWith(System.getProperty("user.home")), "Root path does not start with user's home");
        }

        @Test
        @DisplayName("Should use specified directory when " + RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER + " is set")
        void shouldUseSpecifiedDirectoryWhenParameterIsSet() {
            // Given
            RootPathProvider rootPathProvider;
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, "/tmp");
                rootPathProvider = new RootPathProviderImpl(this.getClass(), messageHandler);
            }
            // When
            String rootPath = rootPathProvider.getRootPath();
            // Then
            assertNotNull(rootPath);
            assertTrue(rootPathProvider.getRootPath().startsWith("/tmp"));
        }

        @Test
        @DisplayName("Should warn if " + RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER + " is blank")
        void shouldWarnIfPropertyIsBlank() {
            synchronized (System.getProperties()) {
                // Given
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, "");
                // When
                new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertEquals(1, messageHandler.getAllExceptions().size());
            assertEquals(EmptyPathException.class, messageHandler.getAllExceptions().getFirst().getClass());
        }

        @Test
        @DisplayName("Should be invalid if " + RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER + " is blank")
        void providerIsInvalidIfInvalidPreferencesDirectoryParameter() {
            RootPathProvider sut;
            synchronized (System.getProperties()) {
                // Given
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, "  ");
                // When
                sut = new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertFalse(sut.isRootPathAccessible());
            assertNull(sut.getRootPath());
            assertFalse(sut.hasUnrecoverableErrors());
        }
    }

    @Nested
    @DisplayName("Parent directory")
    class ParentDirectory {
        @Test
        @DisplayName("Should be valid if readable and writeable")
        void shouldBeValidIfReadableAndWriteable() throws IOException {
            // Given
            File targetDir = new File(temporaryDirectory.getAbsolutePath()
                    + File.separator + "my"
                    + File.separator + "preferences");
            RootPathProvider sut;
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, targetDir.getAbsolutePath());
                // When
                sut = new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertTrue(sut.isRootPathAccessible());
            assertFalse(sut.hasUnrecoverableErrors());
        }

        @Test
        @DisplayName("Should warn if not writeable and target directory does not exist")
        void shouldWarnIfNotWriteableAndTargetDirectoryDoesNotExist() throws IOException {
            // Given
            applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_READ);
            File targetDir = new File(temporaryDirectory.getAbsolutePath() + File.separator + "preferences");
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, targetDir.getAbsolutePath());
                // When
                new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertEquals(1, messageHandler.getAllExceptions().size());
            assertEquals(ParentDirectoryNotWriteableException.class, messageHandler.getAllExceptions().getFirst().getClass());
        }

        @Test
        @DisplayName("Path should not accessible if parent not writeable and target directory does not exist")
        void isNotAccessibleIfParentNotWriteableAndTargetDirectoryDoesNotExist() throws IOException {
            // Given
            applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_READ);
            File targetDir = new File(temporaryDirectory.getAbsolutePath() + File.separator + "preferences");
            RootPathProvider sut;
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, targetDir.getAbsolutePath());
                // When
                sut = new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertFalse(sut.isRootPathAccessible());
            assertNull(sut.getRootPath());
            assertFalse(sut.hasUnrecoverableErrors());
        }

        @Test
        @DisplayName("Should warn if parent path is not readable")
        void shouldWarnsIfParentPathIsNotReadable() throws IOException {
            // Given
            applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_WRITE);
            File targetDir = new File(temporaryDirectory.getAbsolutePath() + File.separator + "preferences");
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, targetDir.getAbsolutePath());
                // When
                new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertEquals(1, messageHandler.getAllExceptions().size());
            assertEquals(ParentDirectoryNotReadableException.class, messageHandler.getAllExceptions().getFirst().getClass());
        }

        @Test
        @DisplayName("Path should not accessible if parent path is not readable")
        void checkParentPathIsNotReadable() throws IOException {
            // Given
            applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_WRITE);
            File targetDir = new File(temporaryDirectory.getAbsolutePath() + File.separator + "preferences");
            RootPathProvider sut;
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, targetDir.getAbsolutePath());
                // When
                sut = new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            assertFalse(sut.isRootPathAccessible());
            assertNull(sut.getRootPath());
            assertFalse(sut.hasUnrecoverableErrors());
        }
    }

    @Nested
    @DisplayName("Target directory")
    class TargetDirectory {
        @Test
        @DisplayName("Should be accessible if readable and writeable")
        void shouldBeAccessible() throws IOException {
            // Given
            RootPathProvider sut;
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, temporaryDirectory.getAbsolutePath());
                // When
                sut = new RootPathProviderImpl(this, messageHandler);
            }
            // Then
            String expectedRootPath = temporaryDirectory.getAbsolutePath() + File.separator + "." + this.getClass().getPackageName();
            assertTrue(sut.isRootPathAccessible());
            assertEquals(expectedRootPath, sut.getRootPath());
            assertFalse(sut.hasUnrecoverableErrors());
        }

        @Nested
        @DisplayName("Is write only")
        class WriteOnly {

            @Test
            @DisplayName("Should warn")
            void shouldWarn() throws IOException {
                // Given
                applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_WRITE);
                synchronized (System.getProperties()) {
                    System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, temporaryDirectory.getAbsolutePath());
                    // When
                    new RootPathProviderImpl(this, messageHandler);
                }
                // Then
                assertEquals(1, messageHandler.getAllExceptions().size());
                assertEquals(DirectoryNotReadableException.class, messageHandler.getAllExceptions().getFirst().getClass());
            }

            @Test
            @DisplayName("Path should not be accessible")
            void shouldNotBeAccessible() throws IOException {
                // Given
                applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_WRITE);
                RootPathProvider sut;
                synchronized (System.getProperties()) {
                    System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, temporaryDirectory.getAbsolutePath());
                    // When
                    sut = new RootPathProviderImpl(this, messageHandler);
                }
                // Then
                assertFalse(sut.isRootPathAccessible());
                assertNull(sut.getRootPath());
                assertFalse(sut.hasUnrecoverableErrors());
            }
        }

        @Nested
        @DisplayName("Is read only")
        class ReadOnly {
            @Test
            @DisplayName("Should warn")
            void shouldWarn() throws IOException {
                // Given
                applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_READ);
                synchronized (System.getProperties()) {
                    System.setProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER, temporaryDirectory.getAbsolutePath());
                    // When
                    new RootPathProviderImpl(this, messageHandler);
                }
                // Then
                assertEquals(1, messageHandler.getAllExceptions().size());
                assertEquals(DirectoryNotWriteableException.class, messageHandler.getAllExceptions().getFirst().getClass());
            }

            @Test
            @DisplayName("Path should not be accessible")
            void isNotAccessible() throws IOException {
                // Given
                applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_WRITE);
                RootPathProvider sut;
                synchronized (System.getProperties()) {
                    System.setProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER, temporaryDirectory.getAbsolutePath());
                    // When
                    sut = new RootPathProviderImpl(this, messageHandler);
                }
                // Then
                assertFalse(sut.isRootPathAccessible());
                assertNull(sut.getRootPath());
                assertFalse(sut.hasUnrecoverableErrors());
            }
        }

        @Nested
        @DisplayName("Is a file")
        class IsFile {
            @Test
            @DisplayName("Should warn")
            void shouldWarnStd(TestReporter report) throws IOException {
                // Given
                File tmp = FileUtils.createTemporaryDirectory();
                File file = new File(tmp.getAbsolutePath() + File.separator + "test.txt");
                if (!file.createNewFile()) {
                    fail("Can't create file " + file.getAbsolutePath());
                }
                synchronized (System.getProperties()) {
                    System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, file.getAbsolutePath());
                    // When
                    new RootPathProviderImpl(this, messageHandler);
                }
                // Then
                assertEquals(1, messageHandler.getAllExceptions().size());
                assertEquals(PathPointsToFileException.class, messageHandler.getAllExceptions().getFirst().getClass());
            }

            @Test
            @DisplayName("Path should not be accessible")
            void shouldNotBeAccessible(TestReporter report) throws IOException {
                File tmp = FileUtils.createTemporaryDirectory();
                File file = new File(tmp.getAbsolutePath() + File.separator + "test.txt");
                if (!file.createNewFile()) {
                    fail("Can't create file " + file.getAbsolutePath());
                }
                RootPathProvider sut;
                synchronized (System.getProperties()) {
                    System.setProperty(RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER, file.getAbsolutePath());
                    // When
                    sut = new RootPathProviderImpl(this, messageHandler);
                }
                // Then
                assertFalse(sut.isRootPathAccessible());
                assertNull(sut.getRootPath());
                assertFalse(sut.hasUnrecoverableErrors());
                assertEquals(PathPointsToFileException.class, messageHandler.getAllExceptions().getFirst().getClass());
            }
        }
    }
}
