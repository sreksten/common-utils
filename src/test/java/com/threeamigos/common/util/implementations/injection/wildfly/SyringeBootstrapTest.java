package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class SyringeBootstrapTest {

    @jakarta.enterprise.context.Dependent
    public static class MyBean {
        public String hello() {
            return "hello";
        }
    }

    @Test
    public void testManagedBootstrap() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(MyBean.class);

        SyringeBootstrap bootstrap = new SyringeBootstrap(classes, Thread.currentThread().getContextClassLoader());
        Syringe syringe = bootstrap.bootstrap();

        assertNotNull(syringe);
        BeanManager bm = syringe.getBeanManager();
        assertNotNull(bm);

        MyBean bean = syringe.getBeanManager().createInstance().select(MyBean.class).get();
        assertNotNull(bean);
        assertEquals("hello", bean.hello());

        bootstrap.shutdown();
    }

    @Test
    public void testStandaloneSECompatibility() {
        // Test that Syringe still works in SE mode with the package constructor
        Syringe syringe = new Syringe(MyBean.class.getPackage().getName());
        syringe.setup();

        MyBean bean = syringe.getBeanManager().createInstance().select(MyBean.class).get();
        assertNotNull(bean);
        assertEquals("hello", bean.hello());

        syringe.shutdown();
    }
}
