package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;

final class BceScannedClasses implements ScannedClasses {

    private final KnowledgeBase knowledgeBase;
    private final MessageHandler messageHandler;

    BceScannedClasses(KnowledgeBase knowledgeBase, MessageHandler messageHandler) {
        this.knowledgeBase = knowledgeBase;
        this.messageHandler = messageHandler;
    }

    @Override
    public void add(String className) {
        try {
            Class<?> clazz = resolveClass(className);
            knowledgeBase.add(clazz);
            messageHandler.handleInfoMessage("[BCE] Added scanned class " + className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot add scanned class " + className, e);
        }
    }

    private Class<?> resolveClass(String className) throws ClassNotFoundException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            try {
                return Class.forName(className, false, tccl);
            } catch (ClassNotFoundException ignored) {
                // fall through to the container/module class loader
            }
        }

        ClassLoader fallback = BceScannedClasses.class.getClassLoader();
        if (fallback != null && fallback != tccl) {
            return Class.forName(className, false, fallback);
        }
        return Class.forName(className);
    }
}
