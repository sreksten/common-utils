package com.threeamigos.common.util.ui.draganddrop;

import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DragAndDropSupportHelper unit test")
class DragAndDropSupportHelperUnitTest {

    @Test
    @DisplayName("Should support drag and drop of a list of files")
    void shouldSupportDragAndDropOfListOfFiles() {
        // Given
        File file1 = new File("first");
        File file2 = new File("second");
        File file3 = new File("third");
        List<File> files = new ArrayList<>();
        files.add(file1);
        files.add(file2);
        files.add(file3);

        Transferable transferable = new TransferableImpl(DataFlavor.javaFileListFlavor, files);
        DropTargetDropEvent mockEvent = mock(DropTargetDropEvent.class);
        when(mockEvent.getTransferable()).thenReturn(transferable);

        DndSupportClass sut = new DndSupportClass();
        DragAndDropSupportHelper.addJavaFileListSupport(sut);
        // When
        sut.getDropTarget().drop(mockEvent);
        // Then
        List<File> acceptedFiles = sut.getAcceptedFiles();
        assertThat(acceptedFiles, containsInAnyOrder(file1, file2, file3));
    }

    @Test
    @DisplayName("Should not support drag and drop of a list of strings")
    void shouldNotSupportDragAndDropOfListOfFiles() {
        // Given
        List<String> strings = new ArrayList<>();
        strings.add("First");
        strings.add("Second");
        strings.add("Third");

        Transferable transferable = new TransferableImpl(DataFlavor.stringFlavor, strings);
        DropTargetDropEvent mockEvent = mock(DropTargetDropEvent.class);
        when(mockEvent.getTransferable()).thenReturn(transferable);

        DndSupportClass sut = new DndSupportClass();
        DragAndDropSupportHelper.addJavaFileListSupport(sut);
        // When
        sut.getDropTarget().drop(mockEvent);
        // Then
        List<File> acceptedFiles = sut.getAcceptedFiles();
        assertEquals(0, acceptedFiles.size());
    }

    @Test
    @DisplayName("Should handle an exception")
    void shouldHandleException() {
        // Given
        Transferable transferable = new TransferableImpl(DataFlavor.javaFileListFlavor, null) {
            @Nonnull
            @Override
            public Object getTransferData(DataFlavor flavor) throws IOException {
                throw new IOException("Test I/O exception");
            }
        };
        DropTargetDropEvent mockEvent = mock(DropTargetDropEvent.class);
        when(mockEvent.getTransferable()).thenReturn(transferable);

        DndSupportClass sut = new DndSupportClass();
        DragAndDropSupportHelper.addJavaFileListSupport(sut);
        // When
        sut.getDropTarget().drop(mockEvent);
        // Then
        List<Exception> exceptions = sut.exceptionHandler.getAllExceptions();
        assertEquals(1, exceptions.size());
        assertEquals("Test I/O exception", exceptions.get(0).getMessage());
    }

    private static class DndSupportClass extends Component implements Consumer<List<File>>, ExceptionHandler {

        private final List<File> acceptedFiles;
        InMemoryMessageHandler exceptionHandler;

        DndSupportClass() {
            super();
            acceptedFiles = new ArrayList<>();
            exceptionHandler = new InMemoryMessageHandler();
        }

        @Override
        public void handleException(@Nonnull Exception exception) {
            exceptionHandler.handleException(exception);
        }

        @Override
        public void accept(List<File> files) {
            acceptedFiles.addAll(files);
        }

        public List<File> getAcceptedFiles() {
            return acceptedFiles;
        }
    }

    private static class TransferableImpl implements Transferable {

        private final DataFlavor flavor;
        private final Object object;

        TransferableImpl(DataFlavor flavor, Object object) {
            this.flavor = flavor;
            this.object = object;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{flavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return this.flavor == flavor;
        }

        @Override
        public @Nonnull Object getTransferData(DataFlavor flavor) throws IOException {
            return object;
        }
    }
}
