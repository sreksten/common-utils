package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return ois.readObject();
    }
}
