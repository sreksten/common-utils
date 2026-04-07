package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import org.jboss.cdi.tck.spi.Beans;

/**
 * Basic TCK Beans SPI implementation for Syringe.
 */
public class SyringeBeansImpl implements Beans {

    @Override
    public boolean isProxy(Object instance) {
        if (instance == null) {
            return false;
        }
        Class<?> clazz = instance.getClass();
        String name = clazz.getName();
        return name.contains("$$")
                || name.contains("$Proxy")
                || name.startsWith("com.sun.proxy.$Proxy");
    }

    @Override
    public byte[] passivate(Object instance) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(instance);
        oos.flush();
        return baos.toByteArray();
    }

    @Override
    public Object activate(byte[] bytes) throws IOException, ClassNotFoundException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader fallback = SyringeBeansImpl.class.getClassLoader();
        ClassLoader preferred = tccl != null ? tccl : fallback;
        ObjectInputStream ois = new TcclObjectInputStream(new ByteArrayInputStream(bytes), preferred, fallback);
        return ois.readObject();
    }

    private static final class TcclObjectInputStream extends ObjectInputStream {
        private final ClassLoader preferredClassLoader;
        private final ClassLoader fallbackClassLoader;

        private TcclObjectInputStream(ByteArrayInputStream inputStream,
                                      ClassLoader preferredClassLoader,
                                      ClassLoader fallbackClassLoader) throws IOException {
            super(inputStream);
            this.preferredClassLoader = preferredClassLoader;
            this.fallbackClassLoader = fallbackClassLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String className = desc.getName();
            try {
                return Class.forName(className, false, preferredClassLoader);
            } catch (ClassNotFoundException ignored) {
                if (fallbackClassLoader != null && fallbackClassLoader != preferredClassLoader) {
                    try {
                        return Class.forName(className, false, fallbackClassLoader);
                    } catch (ClassNotFoundException ignoredFallback) {
                        // Fall through to default JDK behavior.
                    }
                }
                return super.resolveClass(desc);
            }
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
            try {
                Class<?>[] interfaceClasses = new Class<?>[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    interfaceClasses[i] = Class.forName(interfaces[i], false, preferredClassLoader);
                }
                return Proxy.getProxyClass(preferredClassLoader, interfaceClasses);
            } catch (ClassNotFoundException | IllegalArgumentException ignored) {
                if (fallbackClassLoader != null && fallbackClassLoader != preferredClassLoader) {
                    try {
                        Class<?>[] interfaceClasses = new Class<?>[interfaces.length];
                        for (int i = 0; i < interfaces.length; i++) {
                            interfaceClasses[i] = Class.forName(interfaces[i], false, fallbackClassLoader);
                        }
                        return Proxy.getProxyClass(fallbackClassLoader, interfaceClasses);
                    } catch (ClassNotFoundException | IllegalArgumentException ignoredFallback) {
                        // Fall through to default JDK behavior.
                    }
                }
                return super.resolveProxyClass(interfaces);
            }
        }
    }
}
