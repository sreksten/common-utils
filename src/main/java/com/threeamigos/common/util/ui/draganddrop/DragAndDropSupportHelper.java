package com.threeamigos.common.util.ui.draganddrop;

import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * A helper class to add drag-and-drop support for files to a {@link java.awt.Component}.
 * This component must be a {@link Consumer} for a {@link List} of {@link File}s.
 * If the component is not also an {@link ExceptionHandler}, one must be provided
 * that will be used if problems arise during the drag-and-drop operation.
 *
 * @author Stefano Reksten.
 */
public class DragAndDropSupportHelper {

    private DragAndDropSupportHelper() {
    }

    public static <T extends Component & Consumer<List<File>> & ExceptionHandler> void addJavaFileListSupport(
            T component) {
        addJavaFileListSupport(component, component);
    }

    public static <T extends Component & Consumer<List<File>>> void addJavaFileListSupport(T component,
                                                                                           ExceptionHandler exceptionHandler) {
        component.setDropTarget(new DropTarget() {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    if (evt.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) evt.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        component.accept(droppedFiles);
                    }
                    evt.dropComplete(true);
                } catch (Exception e) {
                    exceptionHandler.handleException(e);
                }
            }
        });
    }

}
