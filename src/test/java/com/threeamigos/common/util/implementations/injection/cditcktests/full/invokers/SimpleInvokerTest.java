package com.threeamigos.common.util.implementations.injection.cditcktests.full.invokers;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.invoke.Invoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
class SimpleInvokerTest {

    @Test
    void test() throws Exception {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                MyService.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(TestExtension.class.getName());

        try {
            syringe.setup();

            MyService service = syringe.getBeanManager().createInstance().select(MyService.class).get();
            Invoker<MyService, String> hello = syringe.getBeanManager().getExtension(TestExtension.class).getInvoker("hello");
            assertEquals("foobar1[]", hello.invoke(service, new Object[]{1, Collections.<String>emptyList()}));
        } finally {
            syringe.shutdown();
        }
    }

    public static class TestExtension implements Extension {
        private final Map<String, Invoker<?, ?>> invokers = new HashMap<String, Invoker<?, ?>>();

        public void myServiceRegistration(@Observes ProcessManagedBean<MyService> pmb) {
            pmb.getAnnotatedBeanClass()
                    .getMethods()
                    .stream()
                    .filter(it -> "hello".equals(it.getJavaMember().getName()))
                    .forEach(it -> invokers.put(it.getJavaMember().getName(), pmb.createInvoker(it).build()));
        }

        @SuppressWarnings("unchecked")
        public <T, R> Invoker<T, R> getInvoker(String name) {
            return (Invoker<T, R>) invokers.get(name);
        }
    }

    @ApplicationScoped
    public static class MyService {
        public String hello(int param1, List<String> param2) {
            return "foobar" + param1 + param2;
        }
    }
}
