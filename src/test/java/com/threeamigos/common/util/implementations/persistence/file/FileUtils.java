package com.threeamigos.common.util.implementations.persistence.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class FileUtils {
    public static final String DISK_FULL_OR_ANY_OTHER_CAUSE = "Disk full (or any other cause)";
    public static final String FILE_CORRUPTED_OR_ANY_OTHER_CAUSE = "File corrupted (or any other cause)";

    public static File createTemporaryDirectory() throws IOException {
        Path tmpPath = Files.createTempDirectory("temporaryDirectory");
        File tmpFile = tmpPath.toFile();
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    public static File createTemporaryDirectory(PosixFilePermission permission) throws IOException {
        FileAttribute<Set<PosixFilePermission>> fileAttribute = createFileAttributes(permission);
        Path tmpPath = Files.createTempDirectory("temporaryDirectory", fileAttribute);
        File tmpFile = tmpPath.toFile();
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    public static void applyFileAttributes(File file, PosixFilePermission... filePermissions) throws IOException {
        applyFileAttributes(file.toPath(), filePermissions);
    }

    public static void applyFileAttributes(Path path, PosixFilePermission... filePermissions) throws IOException {
        Files.setPosixFilePermissions(path, new HashSet<>(List.of(filePermissions)));
    }

    public static FileAttribute<Set<PosixFilePermission>> createFileAttributes(PosixFilePermission... filePermissions) {
        return PosixFilePermissions.asFileAttribute(new HashSet<>(List.of(filePermissions)));
    }

    public static OutputStream createFailingOutputStream() throws IOException {
        OutputStream outputStream = mock(OutputStream.class);
        doThrow(new IOException(DISK_FULL_OR_ANY_OTHER_CAUSE)).when(outputStream).write(anyInt());
        doThrow(new IOException(DISK_FULL_OR_ANY_OTHER_CAUSE)).when(outputStream).write(any(byte[].class));
        doThrow(new IOException(DISK_FULL_OR_ANY_OTHER_CAUSE)).when(outputStream).write(any(byte[].class), anyInt(), anyInt());
        return outputStream;
    }

    public static InputStream createFailingInputStream() throws IOException {
        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read()).thenThrow(new IOException(FILE_CORRUPTED_OR_ANY_OTHER_CAUSE));
        when(inputStream.read(any(byte[].class))).thenThrow(new IOException(FILE_CORRUPTED_OR_ANY_OTHER_CAUSE));
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException(FILE_CORRUPTED_OR_ANY_OTHER_CAUSE));
        return inputStream;
    }

    public static String readTextFileContent(File file) throws IOException {
        return Files.readAllLines(file.toPath()).stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
