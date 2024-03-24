import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.persistence.file.rootpathprovider.EmptyPackageException;
import com.threeamigos.common.util.implementations.persistence.file.rootpathprovider.RootPathProviderImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RootPathProviderImpl Unit Test for no-package class")
@Tag("unit")
@Tag("persistence")
class RootPathProviderNoPackageUnitTest {

    @Test
    @DisplayName("Should have unrecoverable errors for a class without package")
    void shouldHaveUnrecoverableErrorsForClassWithoutPackage() {
        // Given
        MessageHandler messageHandler = new InMemoryMessageHandler();
        Class<?> clazz = TestClassWithoutPackage.class;
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
    @DisplayName("Should output a warning for a class without package")
    void shouldOutputWarningForClassWithoutPackage() {
        // Given
        InMemoryMessageHandler messageHandler = new InMemoryMessageHandler();
        Class<?> clazz = TestClassWithoutPackage.class;
        // When
        RootPathProvider sut = new RootPathProviderImpl(clazz, messageHandler);
        // Then
        assertEquals(1, messageHandler.getAllExceptions().size());
        assertEquals(EmptyPackageException.class, messageHandler.getAllExceptions().getFirst().getClass());
    }
}
