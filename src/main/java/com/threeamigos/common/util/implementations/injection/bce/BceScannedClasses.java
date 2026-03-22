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
            Class<?> clazz = Class.forName(className);
            knowledgeBase.add(clazz);
            messageHandler.handleInfoMessage("[BCE] Added scanned class " + className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot add scanned class " + className, e);
        }
    }
}
