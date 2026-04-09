package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyDependentBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyPojo;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyPojoCreator;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.MyPojoDisposer;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test.SyntheticBeanWithLookupExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testScannedClassesUsesTcclForDeploymentOnlyClasses() throws Exception {
        String className = "org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.MyDependentBean";
        Path tempRoot = Files.createTempDirectory("syringe-bce-scanned-classes");
        compileTcclOnlyDependentBean(tempRoot, className);

        ClassLoader previousTccl = Thread.currentThread().getContextClassLoader();
        URLClassLoader deploymentClassLoader = new URLClassLoader(new URL[]{tempRoot.toUri().toURL()}, previousTccl);
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TcclRootBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        TcclScannedClassExtension.className = className;
        syringe.addBuildCompatibleExtension(TcclScannedClassExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            syringe.setup();
            Class<?> scannedClass = Class.forName(className, false, deploymentClassLoader);
            assertNotNull(syringe.inject(scannedClass));
        } finally {
            Thread.currentThread().setContextClassLoader(previousTccl);
            deploymentClassLoader.close();
            syringe.shutdown();
        }
    }

    @Test
    void testExternalDiscoveryDuplicateAfterBceDiscoveryIsIdempotent() {
        MyDependentBean.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TcclRootBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(SyntheticBeanWithLookupExtension.class.getName());
        try {
            syringe.initialize();
            assertDoesNotThrow(() -> syringe.addExternallyDiscoveredClass(
                    MyDependentBean.class, BeanArchiveMode.EXPLICIT));
            syringe.start();
            assertNotNull(syringe.inject(MyDependentBean.class));
        } finally {
            syringe.shutdown();
        }
    }

    private static void compileTcclOnlyDependentBean(Path outputRoot, String className) throws IOException {
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
                "import jakarta.enterprise.context.Dependent;\n" +
                "@Dependent\n" +
                "public class " + simpleName + " {\n" +
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

    public static class TcclScannedClassExtension implements BuildCompatibleExtension {
        static String className;

        @Discovery
        public void discovery(ScannedClasses scannedClasses) {
            scannedClasses.add(className);
        }
    }

    @Dependent
    public static class TcclRootBean {
    }
}
