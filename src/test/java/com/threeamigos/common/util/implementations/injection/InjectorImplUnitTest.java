package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed1;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed2;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.subpackage.MultipleConcreteClassesNamed3;
import com.threeamigos.common.util.implementations.injection.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsNamed2;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsStandardImplementation;
import com.threeamigos.common.util.interfaces.injection.Injector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.*;

import javax.enterprise.inject.InjectionException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Any;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InjectorImpl unit tests")
class InjectorImplUnitTest {

    private static final String TEST_PACKAGE_NAME = "com.threeamigos";

    /**
     * The Injector can work on Interfaces, Abstract Classes, and Concrete Classes.
     * Other types of objects are not supported because the Injector could not instantiate them.
     */
    @Nested
    @DisplayName("Check class compatibility")
    class ClassCompatibilityTests {

        @Nested
        @DisplayName("Accept valid classes")
        class ValidClasses {

            @Test
            @DisplayName("Interfaces")
            void shouldNotThrowExceptionIfInjectingAnInterface() {
                // Given
                Injector sut = new InjectorImpl();
                // When
                Class<?> clazz = TestInterface.class;
                // Then
                assertDoesNotThrow(() -> sut.inject(clazz));
            }

            @Test
            @DisplayName("Abstract classes")
            void shouldNotThrowExceptionIfInjectingAnAbstractClass() {
                // Given
                Injector sut = new InjectorImpl();
                // When
                Class<?> clazz = AbstractClass.class;
                // Then
                assertDoesNotThrow(() -> sut.inject(clazz));
            }

            @Test
            @DisplayName("Concrete classes")
            void shouldNotThrowExceptionIfInjectingAConcreteClass() {
                // Given
                Injector sut = new InjectorImpl();
                // When
                Class<?> clazz = ConcreteClass.class;
                // Then
                assertDoesNotThrow(() -> sut.inject(clazz));
            }
        }

        @Nested
        @DisplayName("Fail with invalid classes")
        class InvalidClasses {

            @Test
            @DisplayName("Enums")
            void shouldThrowExceptionIfInjectingAnEnum() {
                assertThrows(InjectionException.class, () -> new InjectorImpl().inject(TestEnum.class));
            }

            @Test
            @DisplayName("Primitives")
            void shouldThrowExceptionIfInjectingAPrimitive() {
                assertThrows(InjectionException.class, () -> new InjectorImpl().inject(int.class));
            }

            @Test
            @DisplayName("Synthetic classes")
            void shouldThrowExceptionIfInjectingASyntheticClass() {
                // Given
                final Class<?> syntheticClass = ((Runnable)() -> {}).getClass();
                // Then
                assertTrue(syntheticClass.isSynthetic());
                assertThrows(InjectionException.class, () -> new InjectorImpl().inject(syntheticClass));
            }

            @Test
            @DisplayName("Local classes")
            void shouldThrowExceptionIfInjectingALocalClass() {
                // Given
                class MyLocalClass {}
                // Then
                assertTrue(MyLocalClass.class.isLocalClass());
                assertThrows(InjectionException.class, () -> new InjectorImpl().inject(MyLocalClass.class));
            }

            @Test
            @DisplayName("Anonymous classes")
            void shouldThrowExceptionIfInjectingAnAnonymousClass() {
                // Given
                /*
                 * Warning: Anonymous new Runnable() can be replaced with lambda. But if we do,
                 * that becomes a synthetic class, not an anonymous class. LEAVE IT AS IS!
                 */
                @SuppressWarnings("Convert2Lambda") Runnable anonymousRunnable = new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Hello from anonymous class!");
                    }
                };
                // Then
                assertTrue(anonymousRunnable.getClass().isAnonymousClass());
                assertThrows(InjectionException.class, () -> new InjectorImpl().inject(anonymousRunnable.getClass()));
            }

            @Test
            @DisplayName("Non-static inner classes")
            void shouldThrowExceptionIfInjectingNonStaticInnerClass() {
                assertThrows(InjectionException.class, () -> new InjectorImpl().inject(NonStaticInnerClass.class));
            }
        }
    }

    @Nested
    @DisplayName("Search Constructor tests")
    class SearchConstructorTests {

        /**
         * The Injector will look for a constructor annotated with @Inject.
         */
        @Test
        @DisplayName("Should return the only constructor annotated with @Inject")
        void shouldReturnConstructorAnnotatedWithInject() throws NoSuchMethodException {
            // Given
            class TestClass {
                @Inject
                public TestClass() { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When
            Constructor<TestClass> constructor = sut.getConstructor(TestClass.class);
            // Then
            assertNotNull(constructor);
        }

        /**
         * If more than one constructor is annotated with @Inject, IllegalStateException will be thrown.
         */
        @Test
        @DisplayName("Should throw IllegalStateException if more than one constructor is annotated with @Inject")
        void shouldThrowExceptionWhenMoreThanOneConstructorIsAnnotatedWithInject() {
            // Given
            @SuppressWarnings("unused")
            class TestClass {
                @Inject
                public TestClass(java.util.Date date) { }
                @Inject
                public TestClass(String s) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            assertThrows(IllegalStateException.class, () -> sut.getConstructor(TestClass.class));
        }

        /**
         * If more constructors are available, the Injector will choose the one annotated with @Inject.
         */
        @Test
        @DisplayName("Should return constructor marked with @Inject when available, preferring it to the no-arguments constructor")
        void shouldReturnConstructorAnnotatedWithInjectPreferringItToTheNoArgsConstructor() throws NoSuchMethodException {
            // Given
            @SuppressWarnings("unused")
            class TestClass {
                @Inject
                public TestClass(java.util.Date date) { }
                public TestClass() { }
                public TestClass(String s) { }
            }
            InjectorImpl sut = new InjectorImpl();
            Constructor<TestClass> constructor = sut.getConstructor(TestClass.class);
            // When
            int numberOfArguments = constructor.getParameterCount();
            // Then
            // Being TestClass a non-static inner class, the constructor has the class itself as the first argument
            assertEquals(2, numberOfArguments);
        }

        /**
         * If there is no constructor marked with @Inject, the Injector will use the no-args constructor.
         */
        @Test
        @DisplayName("Should use no-args constructor if no constructor annotated with @Inject is found")
        void shouldUseNoArgsConstructorIfNoCompatibleConstructorFound() throws NoSuchMethodException {
            // Given
            InjectorImpl sut = new InjectorImpl();
            Constructor<TestClass> constructor = sut.getConstructor(TestClass.class);
            // When
            int numberOfArguments = constructor.getParameterCount();
            // Then
            assertEquals(0, numberOfArguments);
        }

        /**
         * If no constructor is annotated with @Inject and a no-arguments constructor is not available, a
         * NoSuchMethodException will be thrown.
         */
        @Test
        @DisplayName("Should throw NoSuchMethodException if no constructor annotated with @Inject is found and no-arguments constructor is not available")
        void shouldThrowNoSuchMethodExceptionIFNoCompatibleConstructorFound() {
            // Given
            @SuppressWarnings("unused")
            class TestClass {
                TestClass(int i) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            assertThrows(NoSuchMethodException.class, () -> sut.getConstructor(TestClass.class));
        }

        /**
         * Once the constructor was identified, the parameters will be checked, and if any of them is not an interface,
         * an abstract class, or a concrete class, an InjectionException will be thrown.
         */
        @Test
        @DisplayName("Should throw InjectionException if any parameter is not an interface, an abstract or a concrete class")
        void shouldThrowIllegalArgumentExceptionIfInvalidParameter() {
            // Given
            @SuppressWarnings("unused")
            class TestClass {
                @Inject
                public TestClass(int i) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            assertThrows(InjectionException.class, () -> sut.inject(TestClass.class));
        }
    }

    @Nested
    @DisplayName("Instantiation tests")
    class InstantiationTests {

        /**
         * If no constructor is annotated with @Inject, the Injector will use the no-args constructor.
         */
        @Test
        @DisplayName("Should instantiate an object with a no-args constructor if no other annotated constructors are present")
        void shouldInstantiateAnObjectWithNoArgsConstructorIfNoOtherAnnotatedConstructorsArePresent() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl();
            // When
            TestClassWithNoArgsConstructor instance = sut.inject(TestClassWithNoArgsConstructor.class);
            // Then
            assertNotNull(instance);
        }

        /**
         * The no-args constructor can be annotated, but this is not necessary because if there is no other constructor
         * annotated with @Inject, the Injector will use the no-args constructor by default.
         */
        @Test
        @DisplayName("Should instantiate an object with annotated no-args constructor")
        void shouldInstantiateAnObjectWithAnnotatedNoArgsConstructor() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl();
            // When
            TestClassWithAnnotatedNoArgsConstructor instance = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            // Then
            assertNotNull(instance);
        }

        /**
         * Once the constructor marked with @Inject has been identified, the Injector will instantiate the object.
         * TestClassWithAnnotatedConstructor has a constructor that needs a TestClass as a dependency.
         * TestClass is a concrete class.
         */
        @Test
        @DisplayName("Should instantiate an object with an annotated constructor and a concrete dependency")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndConcreteDependency() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedConstructorAndConcreteDependency instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndConcreteDependency.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getTestClass());
            assertEquals(TestClass.class, instance.getTestClass().getClass());
        }

        /**
         * The class can, however, have a dependency on an interface or abstract class. In this case the Injector will
         * resolve the implementation based on the annotations on the class and its dependencies. Here we have only
         * one implementation.
         */
        @Test
        @DisplayName("Should instantiate an object with an annotated constructor and an abstract dependency with a single implementation")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependency() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedConstructorAndAbstractDependency instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndAbstractDependency.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getTestInterface());
            assertEquals(TestClass.class, instance.getTestInterface().getClass());
        }

        /**
         * If we have a dependency on an interface or abstract class that has more than one implementation, the Injector
         * will resolve the implementation based on the annotations on the class and its dependencies. Normally it uses
         * the default implementation (not annotated with {@link Named}).
         */
        @Test
        @DisplayName("Should instantiate an object with an annotated constructor and an abstract dependency with more implementations (get default)")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getMultipleAnnotatedImplementationsInterface());
            assertEquals(MultipleImplementationsStandardImplementation.class, instance.getMultipleAnnotatedImplementationsInterface().getClass());
        }

        /**
         * If we have a dependency on an interface or abstract class that has more than one implementation, the Injector
         * will resolve the implementation based on the annotations on the class and its dependencies. In this case
         * we specify to use a particular alternative implementation.
         */
        @Test
        @DisplayName("Should instantiate an object with an annotated constructor and an abstract dependency (get alternative implementation)")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getMultipleAnnotatedImplementationsInterface());
            assertEquals(MultipleImplementationsNamed2.class, instance.getMultipleAnnotatedImplementationsInterface().getClass());
        }

        /**
         * If a class is annotated with {@link Singleton}, this class will be instantiated only once. The same
         * instance will be passed to every object that needs it as a dependency.
         */
        @Test
        @DisplayName("Should instantiate a singleton class only once")
        void shouldInstantiateASingletonClass() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SingletonClass instance1 = sut.inject(SingletonClass.class);
            // When
            SingletonClass instance2 = sut.inject(SingletonClass.class);
            assertSame(instance1, instance2);
        }

        /**
         * A class that is not annotated with {@link Singleton} can be instantiated as many times as needed.
         */
        @Test
        @DisplayName("Should instantiate a non-singleton class as much as needed")
        void shouldInstantiateANonSingletonClassAsMuchAsNeeded() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedNoArgsConstructor instance1 = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            TestClassWithAnnotatedNoArgsConstructor instance2 = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            // Then
            assertNotSame(instance1, instance2);
        }

        /**
         * A class can be instantiated even if it has a private constructor.
         */
        @Nested
        @DisplayName("Private Constructor Tests")
        class PrivateConstructorTests {

            @Test
            @DisplayName("Should instantiate an object with private constructor and no dependencies")
            void shouldInstantiateObjectWithPrivateConstructorAndNoDependencies() throws Exception {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                // When
                ClassWithPrivateConstructor instance = sut.inject(ClassWithPrivateConstructor.class);
                // Then
                assertNotNull(instance);

            }

            @Test
            @DisplayName("Should instantiate an object with private constructor and dependencies")
            void shouldInstantiateObjectWithPrivateConstructorAndDependencies() throws Exception {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                // When
                ClassWithPrivateConstructorWithDependencies instance = sut.inject(ClassWithPrivateConstructorWithDependencies.class);
                // Then
                assertNotNull(instance);
                assertNotNull(instance.getAbstractClass());
                assertEquals(ConcreteClass.class, instance.getAbstractClass().getClass());
            }
        }
    }

    /**
     * Tests for the Instance Wrapper functionality.
     * The Instance allows resolving instances of interfaces or abstract classes.
     * Instance.get() returns the resolved instance of the interface or abstract class.
     * Instance.iterator(), if annotated with {@link Any}, returns an iterator over all the concrete implementations of
     * the interface or abstract class. Otherwise, the iterator will contain only one element (the default one, or
     * an {@link Named} if that option was specified).
     */
    @Nested
    @DisplayName("Instance Wrapper Tests")
    class InstanceWrapperTests {

        /**
         * ClassWithInstance has a dependency that is an Instance of TestInterface. The only implementation of
         * TestInterface is TestClass.
         */
        @Test
        @DisplayName("Instance with single implementation")
        void instanceWithSingleImplementation() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstanceOfTestInterface classWithInstanceOFTestInterface =
                    sut.inject(ClassWithInstanceOfTestInterface.class);
            // When
            Instance<TestInterface> instance = classWithInstanceOFTestInterface.getTestInterfaceInstance();
            // Then
            assertNotNull(instance);
            TestInterface resolvedInstance = instance.get();
            assertNotNull(resolvedInstance);
            assertInstanceOf(TestInterface.class, resolvedInstance);
            assertEquals(TestClass.class, resolvedInstance.getClass());
        }

        /**
         * ClassWithAnyInstanceAndSingleImplementation has a dependency that is an Instance of
         * SingleImplementationAbstractClass. Since we have only one implementation, the iterator will contain only
         * one element.
         */
        @Test
        @DisplayName("@Any Instance with single implementation")
        void anyInstanceWithSingleImplementation() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithAnyInstanceAndSingleImplementation classWithAnyInstance = sut.inject(ClassWithAnyInstanceAndSingleImplementation.class);
            // When
            Instance<SingleImplementationAbstractClass> instance = classWithAnyInstance.getInstance();
            // Then
            assertNotNull(instance);
            List<Class<? extends SingleImplementationAbstractClass>> implementations = new ArrayList<>();
            for (SingleImplementationAbstractClass multipleAnnotatedConcreteClassesAbstractClass : instance) {
                implementations.add(multipleAnnotatedConcreteClassesAbstractClass.getClass());
            }
            assertEquals(1, implementations.size());
            assertEquals(SingleImplementationConcreteClass.class, implementations.get(0));
        }

        /**
         * ClassWithAnyInstanceAndMultipleImplementations has a dependency that is an Instance of
         * MultipleAnnotatedConcreteClassesAbstractClass. This class is annotated with @Any, so the iterator will
         * contain all possible implementations of that dependency class. Note that
         * MultipleAnnotatedConcreteClassesAlternative3 is contained in another package than that of the
         * abstract class.
         */
        @Test
        @DisplayName("@Any Instance and multiple implementations")
        void anyInstanceAndMultipleImplementations() throws Exception {
            // Given
            List<Class<? extends MultipleConcreteClassesAbstractClass>> expectedImplementations = new ArrayList<>();
            expectedImplementations.add(MultipleConcreteClassesStandardClass.class);
            expectedImplementations.add(MultipleConcreteClassesNamed1.class);
            expectedImplementations.add(MultipleConcreteClassesNamed2.class);
            expectedImplementations.add(MultipleConcreteClassesNamed3.class);

            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithAnyInstanceAndMultipleImplementations classWithAnyInstanceAndMultipleImplementations = sut.inject(ClassWithAnyInstanceAndMultipleImplementations.class);
            // When
            Instance<MultipleConcreteClassesAbstractClass> instance = classWithAnyInstanceAndMultipleImplementations.getInstance();
            // Then
            assertNotNull(instance);
            List<Class<? extends MultipleConcreteClassesAbstractClass>> implementations = new ArrayList<>();
            for (MultipleConcreteClassesAbstractClass multipleConcreteClassesAbstractClass : instance) {
                implementations.add(multipleConcreteClassesAbstractClass.getClass());
            }
            expectedImplementations.sort(Comparator.comparing(Class::getName));
            implementations.sort(Comparator.comparing(Class::getName));
            // The iterator returned the same classes (possibly in a different order)
            assertEquals(expectedImplementations, implementations);
        }

        /**
         * When a dependency has multiple possible implementations, we can use the default one or an alternative one
         * annotated with @Alternative. This test checks that the default implementation is returned when no @Alternative
         * is specified.
         */
        @Test
        @DisplayName("Default implementation should be returned when no @Alternative is specified")
        void defaultImplementationShouldBeReturnedWhenNoAlternativeIsSpecified() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstanceAndMultipleImplementationsDefault classWithAnyInstanceAndMultipleImplementations = sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
            // When
            Instance<MultipleConcreteClassesAbstractClass> instance = classWithAnyInstanceAndMultipleImplementations.getInstance();
            // Then
            assertNotNull(instance);
            assertEquals(MultipleConcreteClassesStandardClass.class, instance.get().getClass());
        }

        /**
         * If, however, we specify an @Alternative annotation, the alternative implementation should be returned.
         */
        @Test
        @DisplayName("Alternative implementation should be returned when @Alternative is specified")
        void alternativeImplementationShouldBeReturnedWhenAlternativeIsSpecified() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstanceAndMultipleImplementationsAlternative classWithAnyInstanceAndMultipleImplementations = sut.inject(ClassWithInstanceAndMultipleImplementationsAlternative.class);
            // When
            Instance<MultipleConcreteClassesAbstractClass> instance = classWithAnyInstanceAndMultipleImplementations.getInstance();
            // Then
            assertNotNull(instance);
            assertEquals(MultipleConcreteClassesNamed2.class, instance.get().getClass());
        }

        /**
         * When we have to deal with an Instance of an abstract class or interface with no concrete implementations:
         */
        @Nested
        @DisplayName("Instance with no concrete implementations")
        class InstancesWithNoConcreteImplementations {

            @Nested
            @DisplayName("@Any Instance")
            class AnyInstance {

                /**
                 * Instance.get() throws an exception since we can't return any implementation.
                 */
                @Test
                @DisplayName("Instance.get() should fail")
                void instanceGetShouldFail() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithAnyInstanceButNoImplementation instance = sut.inject(ClassWithAnyInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().get());
                }

                /**
                 * Instance.iterator().hasNext() returns false since the list of all possible implementations is empty.
                 */
                @Test
                @DisplayName("Instance.iterator().hasNext() should return false")
                void instanceIteratorHasNextShouldReturnFalse() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithAnyInstanceButNoImplementation instance = sut.inject(ClassWithAnyInstanceButNoImplementation.class);
                    // Then
                    assertFalse(instance.getInstance().iterator().hasNext());
                }
            }

            @Nested
            @DisplayName("Instance (not @Any)")
            class Instance {

                /**
                 * Instance.get() throws an exception since we can't return any implementation.
                 */
                @Test
                @DisplayName("Instance.get() should fail")
                void instanceGetShouldFail() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithInstanceButNoImplementation instance = sut.inject(ClassWithInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().get());
                }

                /**
                 * Instance.iterator().hasNext() throws an exception since we should have a valid implementation,
                 * but we don't have one, and we did not request a complete list of all possible implementations.
                 */
                @Test
                @DisplayName("Instance.iterator().hasNext() should throw exception")
                void instanceIteratorNextShouldThrowException() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithInstanceButNoImplementation instance = sut.inject(ClassWithInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().iterator());
                }
            }
        }
    }

    // Test classes used above.

    // Valid classes
    interface TestInterface{}

    static class TestClass implements TestInterface { }

    static abstract class AbstractClass { }

    static class ConcreteClass extends AbstractClass { }

    // Invalid classes
    enum TestEnum{}

    // Constructor tests

    static class TestClassWithAnnotatedNoArgsConstructor {
        @Inject
        public TestClassWithAnnotatedNoArgsConstructor() { }
    }

    static class TestClassWithNoArgsConstructor {
        public TestClassWithNoArgsConstructor() { }
    }

    static class TestClassWithAnnotatedConstructorAndConcreteDependency {
        private final TestClass testClass;
        @Inject
        public TestClassWithAnnotatedConstructorAndConcreteDependency(TestClass testClass) {
            this.testClass = testClass;
        }
        public TestClass getTestClass() {
            return testClass;
        }
    }

    static class TestClassWithAnnotatedConstructorAndAbstractDependency {
        private final TestInterface testInterface;
        @Inject
        public TestClassWithAnnotatedConstructorAndAbstractDependency(TestInterface testInterface) {
            this.testInterface = testInterface;
        }
        public TestInterface getTestInterface() {
            return testInterface;
        }
    }

    static class TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault {
        private final MultipleImplementationsInterface testInterface;
        @Inject
        public TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault(MultipleImplementationsInterface testInterface) {
            this.testInterface = testInterface;
        }
        public MultipleImplementationsInterface getMultipleAnnotatedImplementationsInterface() {
            return testInterface;
        }
    }

    static class TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative {
        private final MultipleImplementationsInterface testInterface;
        @Inject
        public TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative(@Named("alternative2") MultipleImplementationsInterface testInterface) {
            this.testInterface = testInterface;
        }
        public MultipleImplementationsInterface getMultipleAnnotatedImplementationsInterface() {
            return testInterface;
        }
    }

    /*
     * Warning: Inner class "NonStaticInnerClass" bay be static.
     * But if we do, that would not be any longer a NON-STATIC inner class, and we need one for our tests.
     * LEAVE IT AS IS!
     */
    @SuppressWarnings("InnerClassMayBeStatic")
    class NonStaticInnerClass { }

    @Singleton static class SingletonClass { }

    static class ClassWithInstanceOfTestInterface {
        private final Instance<TestInterface> testInterfaceInstance;
        @Inject
        public ClassWithInstanceOfTestInterface(Instance<TestInterface> testInterfaceInstance) {
            this.testInterfaceInstance = testInterfaceInstance;
        }
        public Instance<TestInterface> getTestInterfaceInstance() {
            return testInterfaceInstance;
        }
    }

    static class ClassWithAnyInstanceAndSingleImplementation {
        private final Instance<SingleImplementationAbstractClass> instance;
        @Inject
        public ClassWithAnyInstanceAndSingleImplementation(Instance<SingleImplementationAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<SingleImplementationAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithAnyInstanceAndMultipleImplementations {
        private final Instance<MultipleConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithAnyInstanceAndMultipleImplementations(@Any Instance<MultipleConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<MultipleConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithInstanceAndMultipleImplementationsDefault{
        private final Instance<MultipleConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithInstanceAndMultipleImplementationsDefault(Instance<MultipleConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<MultipleConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithInstanceAndMultipleImplementationsAlternative {
        private final Instance<MultipleConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithInstanceAndMultipleImplementationsAlternative(@Named(value = "alternative2") Instance<MultipleConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<MultipleConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }


    static class ClassWithAnyInstanceButNoImplementation {
        private final Instance<NoConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithAnyInstanceButNoImplementation(@Any Instance<NoConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<NoConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithInstanceButNoImplementation {
        private final Instance<NoConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithInstanceButNoImplementation(Instance<NoConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<NoConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithPrivateConstructorWithDependencies {
        private final AbstractClass abstractClass;
        @Inject
        private ClassWithPrivateConstructorWithDependencies(AbstractClass abstractClass) {
            this.abstractClass = abstractClass;
        }
        public AbstractClass getAbstractClass() {
            return abstractClass;
        }
    }

    static class ClassWithPrivateConstructor {
        @Inject
        private ClassWithPrivateConstructor() { }
    }
}

