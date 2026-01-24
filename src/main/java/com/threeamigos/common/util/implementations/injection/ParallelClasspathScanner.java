package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;

import java.io.IOException;
import java.util.*;

public class ParallelClasspathScanner {

    private static final String ROOT_PACKAGE = "";

    void getAllClasses(ClassLoader classLoader, ClasspathScannerSink sink, ParallelTaskExecutor taskExecutor, String ... packageNames) {
        Collection<String> packagesToScan = new ArrayList<>();
        for (String pkg : packageNames) {
            if (pkg != null && !pkg.isEmpty()) {
                validatePackageName(pkg);
                packagesToScan.add(pkg);
            }
        }
        if (packagesToScan.isEmpty()) {
            packagesToScan.add(ROOT_PACKAGE);
        }

        try {
            for (String packageName : packagesToScan) {
                Runnable runnable = () -> {
                    try {
                        new ClasspathScanner(packageName).getAllClasses(classLoader);
                    } catch (ClassNotFoundException | IOException e) {
                        throw new RuntimeException(e);
                    }
                };
                taskExecutor.submit(runnable);
            }
            taskExecutor.awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void validatePackageName(String packageName) {
        if (!packageName.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*")) {
            throw new IllegalArgumentException("Invalid package name: " + packageName);
        }
    }

}
