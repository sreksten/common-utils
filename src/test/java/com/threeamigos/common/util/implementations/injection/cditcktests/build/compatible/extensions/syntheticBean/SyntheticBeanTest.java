package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyComplexValue;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyEnum;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyPojo;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyPojoDisposer;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.MyService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBean.syntheticbeantest.test.SyntheticBeanExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void testTypesOfClassUsesTcclForDeploymentOnlyClasses() throws Exception {
        String className = "org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.MyEnum";
        Path tempRoot = Files.createTempDirectory("syringe-bce-types-ofclass");
        compileTcclOnlyEnum(tempRoot, className);

        ClassLoader previousTccl = Thread.currentThread().getContextClassLoader();
        URLClassLoader deploymentClassLoader = new URLClassLoader(new URL[]{tempRoot.toUri().toURL()}, previousTccl);
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TcclRootBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        TcclTypesLookupExtension.className = className;
        syringe.addBuildCompatibleExtension(TcclTypesLookupExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            assertDoesNotThrow(syringe::setup);
        } finally {
            Thread.currentThread().setContextClassLoader(previousTccl);
            deploymentClassLoader.close();
            syringe.shutdown();
        }
    }

    private static void compileTcclOnlyEnum(Path outputRoot, String className) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java compiler is unavailable");
        }

        int lastDot = className.lastIndexOf('.');
        String packageName = className.substring(0, lastDot);
        String simpleName = className.substring(lastDot + 1);
        Path sourceFile = outputRoot.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(sourceFile.getParent());
        String source = "package " + packageName + ";\n" +
                "public enum " + simpleName + " {\n" +
                "    YES,\n" +
                "    NO;\n" +
                "}\n";
        Files.write(sourceFile, source.getBytes(StandardCharsets.UTF_8));

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);
        try {
            Iterable compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                    Collections.singletonList(sourceFile.toFile()));
            String classPath = System.getProperty("java.class.path");
            boolean compiled = Boolean.TRUE.equals(compiler.getTask(
                    null,
                    fileManager,
                    null,
                    Arrays.asList("-classpath", classPath, "-d", outputRoot.toString()),
                    null,
                    compilationUnits
            ).call());
            assertTrue(compiled, "Failed to compile " + className + " for TCCL regression test");
        } finally {
            fileManager.close();
        }
    }

    public static class TcclTypesLookupExtension implements BuildCompatibleExtension {
        static String className;

        @Synthesis
        public void synthesize(SyntheticComponents syntheticComponents, Types types) {
            types.ofClass(className);
        }
    }

    @Dependent
    public static class TcclRootBean {
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
