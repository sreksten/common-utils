package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyDependentBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyPojo;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyPojoCreator;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyPojoDisposer;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.SyntheticBeanWithLookupExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticBeanWithLookupTest {

    @Test
    void test() {
        MyPojo.reset();
        MyPojoCreator.reset();
        MyPojoDisposer.reset();
        MyDependentBean.reset();

        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(SyntheticBeanWithLookupExtension.class.getName());
        syringe.initialize();
        syringe.start();
        try {
            Instance<Object> lookup = syringe.getBeanManager().createInstance();

            assertEquals(0, MyPojo.getCreatedCounter());
            assertEquals(0, MyPojo.getDestroyedCounter());
            assertEquals(0, MyPojoCreator.getCounter());
            assertEquals(0, MyPojoDisposer.getCounter());
            assertEquals(0, MyDependentBean.getCreatedCounter());
            assertEquals(0, MyDependentBean.getDestroyedCounter());

            Instance.Handle<MyPojo> bean = lookup.select(MyPojo.class).getHandle();
            assertEquals("Hello!", bean.get().hello());

            assertEquals(1, MyPojo.getCreatedCounter());
            assertEquals(0, MyPojo.getDestroyedCounter());
            assertEquals(1, MyPojoCreator.getCounter());
            assertEquals(0, MyPojoDisposer.getCounter());
            assertEquals(1, MyDependentBean.getCreatedCounter());
            assertEquals(0, MyDependentBean.getDestroyedCounter());

            bean.destroy();

            assertEquals(1, MyPojo.getCreatedCounter());
            assertEquals(1, MyPojo.getDestroyedCounter());
            assertEquals(1, MyPojoCreator.getCounter());
            assertEquals(1, MyPojoDisposer.getCounter());
            assertEquals(2, MyDependentBean.getCreatedCounter());
            assertEquals(2, MyDependentBean.getDestroyedCounter());
        } finally {
            syringe.shutdown();
        }
    }
}
