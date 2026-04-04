package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyComplexValue;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyEnum;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyPojo;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyPojoDisposer;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.SyntheticBeanExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticBeanTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.IMPLICIT);
        syringe.addBuildCompatibleExtension(SyntheticBeanExtension.class.getName());
        syringe.setup();
        try {
            MyPojoDisposer.resetDisposed();

            Instance<Object> lookup = syringe.getBeanManager().createInstance();
            Instance.Handle<MyService> handle = lookup.select(MyService.class).getHandle();
            MyService myService = handle.get();

            MyPojo unqualified = myService.getUnqualified();
            assertEquals("Hello World", unqualified.getText());
            assertMyComplexValue(unqualified.getAnn(), 42, MyEnum.YES, "yes", new byte[]{4, 5, 6});

            MyPojo qualified = myService.getQualified();
            assertEquals("Hello @MyQualifier Special", qualified.getText());
            assertMyComplexValue(qualified.getAnn(), 13, MyEnum.NO, "no", new byte[]{1, 2, 3});

            assertEquals(0, MyPojoDisposer.getDisposed().size());
            handle.destroy();
            assertEquals(2, MyPojoDisposer.getDisposed().size());
            assertTrue(MyPojoDisposer.getDisposed().contains("Hello World"));
            assertTrue(MyPojoDisposer.getDisposed().contains("Hello @MyQualifier Special"));
        } finally {
            syringe.shutdown();
        }
    }

    private static void assertMyComplexValue(MyComplexValue ann,
                                             int number,
                                             MyEnum enumeration,
                                             String nestedValue,
                                             byte[] nestedBytes) {
        assertNotNull(ann);
        assertEquals(number, ann.number());
        assertEquals(enumeration, ann.enumeration());
        assertEquals(MyEnum.class, ann.type());
        assertEquals(nestedValue, ann.nested().value());
        assertArrayEquals(nestedBytes, ann.nested().bytes());
    }
}
