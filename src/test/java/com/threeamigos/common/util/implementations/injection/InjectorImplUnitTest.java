package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.Holder;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed1;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed2;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.subpackage.MultipleConcreteClassesNamed3;
import com.threeamigos.common.util.implementations.injection.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesAlternativeImplementation1;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesInterface;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesStandardImplementation;
import com.threeamigos.common.util.implementations.injection.circulardependencies.A;
import com.threeamigos.common.util.implementations.injection.circulardependencies.AWithBProvider;
import com.threeamigos.common.util.implementations.injection.circulardependencies.B;
import com.threeamigos.common.util.implementations.injection.circulardependencies.BWithAProvider;
import com.threeamigos.common.util.implementations.injection.fields.*;
import com.threeamigos.common.util.implementations.injection.generics.GenericsClass;
import com.threeamigos.common.util.implementations.injection.generics.Object1;
import com.threeamigos.common.util.implementations.injection.generics.Object2;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleAlternativesAlternativeImplementation;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsNamed2;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.MultipleImplementationsStandardImplementation;
import com.threeamigos.common.util.implementations.injection.methods.ClassWithMethodWithInvalidParameter;
import com.threeamigos.common.util.implementations.injection.methods.ClassWithMethodWithValidParameters;
import com.threeamigos.common.util.implementations.injection.methods.FirstMethodParameter;
import com.threeamigos.common.util.implementations.injection.methods.SecondMethodParameter;
import com.threeamigos.common.util.implementations.injection.misc.GenericService;
import com.threeamigos.common.util.implementations.injection.parameters.TestClassWithInvalidParametersInConstructor;
import com.threeamigos.common.util.implementations.injection.scopes.*;
import com.threeamigos.common.util.implementations.injection.superclasses.MyClass;
import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.interfaces.injection.ScopeHandler;
import org.atinject.tck.auto.*;
import org.atinject.tck.auto.accessories.SpareTire;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.enterprise.inject.InjectionException;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Any;
import java.lang.reflect.ParameterizedType;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@DisplayName("InjectorImpl unit tests")
@Execution(ExecutionMode.SAME_THREAD)
class InjectorImplUnitTest {

    private static final String TEST_PACKAGE_NAME = "com.threeamigos";

    private static FuelTank NEVER_INJECTED;

    @BeforeAll
    static void setUpClass() throws NoSuchFieldException, IllegalAccessException {
        Field field = Tire.class.getDeclaredField("NEVER_INJECTED");
        field.setAccessible(true);
        NEVER_INJECTED = (FuelTank) field.get(null);
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        resetStaticState();
    }

    private void resetStaticState() throws NoSuchFieldException, IllegalAccessException {
        // Reset TCK static fields if they exist.
        // The JSR-330 TCK classes often need their static state cleared between injector runs
        // because they use static booleans to track if injection happened.

        Field field = Tire.class.getDeclaredField("staticMethodInjectedBeforeStaticFields");
        field.setAccessible(true);
        field.set(null, false);

        field = Tire.class.getDeclaredField("subtypeStaticFieldInjectedBeforeSupertypeStaticMethods");
        field.setAccessible(true);
        field.set(null, false);

        field = Tire.class.getDeclaredField("subtypeStaticMethodInjectedBeforeSupertypeStaticMethods");
        field.setAccessible(true);
        field.set(null, false);

        field = SpareTire.class.getDeclaredField("staticFieldInjection");
        field.setAccessible(true);
        field.set(null, NEVER_INJECTED);

        field = SpareTire.class.getDeclaredField("staticMethodInjection");
        field.setAccessible(true);
        field.set(null, NEVER_INJECTED);
    }

    @TestFactory
    @DisplayName("JSR 330 Technology Compatibility Kit")
    Stream<DynamicTest> tck() throws NoSuchFieldException, IllegalAccessException {
        // Given
        InjectorImpl sut = new InjectorImpl("org.atinject.tck.auto");
        sut.clearState();
        sut.bind(Seat.class, Collections.singleton(AnnotationLiteral.of(Drivers.class)), DriversSeat.class);
        sut.bind(Tire.class, Collections.singleton(new NamedLiteral("spare")), SpareTire.class);
        resetStaticState();
        // When
        org.atinject.tck.auto.Car car = sut.inject(org.atinject.tck.auto.Car.class);
        junit.framework.Test junit3Suite = org.atinject.tck.Tck.testsFor(car, true, true);

        // Then - Bridge JUnit 3 to Dynamic Tests
        return flattenTestSuite(junit3Suite);
    }

    @SuppressWarnings("unchecked")
    private Stream<DynamicTest> flattenTestSuite(junit.framework.Test test) {
        if (test instanceof junit.framework.TestSuite) {
            junit.framework.TestSuite suite = (junit.framework.TestSuite) test;
            return Collections.list(suite.tests()).stream()
                    .flatMap(t -> flattenTestSuite((junit.framework.Test) t));
        } else if (test instanceof junit.framework.TestCase) {
            junit.framework.TestCase testCase = (junit.framework.TestCase) test;
            return Stream.of(DynamicTest.dynamicTest(testCase.getName(), () -> {
                junit.framework.TestResult result = new junit.framework.TestResult();
                testCase.run(result);
                if (!result.wasSuccessful()) {
                    // Throw the first failure found
                    junit.framework.TestFailure failure = (junit.framework.TestFailure) (result.failures().hasMoreElements()
                                                ? result.failures().nextElement()
                                                : result.errors().nextElement());
                    throw failure.thrownException();
                }
            }));
        }
        return Stream.empty();
    }


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
                // Given
                Injector sut = new InjectorImpl();
                Class<?> enumClass = TestEnum.class;
                assertTrue(enumClass.isEnum());
                // When / Then
                InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(enumClass));
                assertTrue(thrown.getMessage().endsWith("Cannot inject an enum"), "Should end with 'Cannot inject an enum': " + thrown.getMessage());
            }

            @Test
            @DisplayName("Primitives")
            void shouldThrowExceptionIfInjectingAPrimitive() {
                // Given
                Injector sut = new InjectorImpl();
                Class<?> intClass = int.class;
                assertTrue(intClass.isPrimitive());
                // When / Then
                InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(intClass));
                assertTrue(thrown.getMessage().endsWith("Cannot inject a primitive"), "Should end with 'Cannot inject a primitive': " + thrown.getMessage());
            }

            @Test
            @DisplayName("Synthetic classes")
            void shouldThrowExceptionIfInjectingASyntheticClass() {
                // Given
                Injector sut = new InjectorImpl();
                Class<?> syntheticClass = ((Runnable)() -> {}).getClass();
                assertTrue(syntheticClass.isSynthetic());
                // When / Then
                InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(syntheticClass));
                assertTrue(thrown.getMessage().endsWith("Cannot inject a synthetic class"), "Should end with 'Cannot inject a synthetic class': " + thrown.getMessage());
            }

            @Test
            @DisplayName("Local classes")
            void shouldThrowExceptionIfInjectingALocalClass() {
                // Given
                Injector sut = new InjectorImpl();
                class MyLocalClass {}
                Class<?> localClass = MyLocalClass.class;
                assertTrue(localClass.isLocalClass());
                // Then
                InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(localClass));
                assertTrue(thrown.getMessage().endsWith("Cannot inject a local class"), "Should end with 'Cannot inject a local class': " + thrown.getMessage());
            }

            @Test
            @DisplayName("Anonymous classes")
            void shouldThrowExceptionIfInjectingAnAnonymousClass() {
                // Given
                Injector sut = new InjectorImpl();
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
                Class<?> anonymousClass = anonymousRunnable.getClass();
                assertTrue(anonymousRunnable.getClass().isAnonymousClass());
                // When / Then
                InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(anonymousClass));
                assertTrue(thrown.getMessage().endsWith("Cannot inject an anonymous class"), "Should end with 'Cannot inject an anonymous class': " + thrown.getMessage());
            }

            @Test
            @DisplayName("Non-static inner classes")
            void shouldThrowExceptionIfInjectingNonStaticInnerClass() {
                // Given
                Injector sut = new InjectorImpl();
                Class<?> nonStaticInnerClass = NonStaticInnerClass.class;
                assertTrue(nonStaticInnerClass.isMemberClass());
                // When / Then
                InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(nonStaticInnerClass));
                assertTrue(thrown.getMessage().endsWith("Cannot inject a non-static inner class"), "Should end with 'Cannot inject a non-static inner class': " + thrown.getMessage());
            }

            @Nested
            @DisplayName("Recursive validation for Parameterized Types")
            class ParameterizedTypeValidityTests {

                @Test
                @DisplayName("Should throw exception if generic argument is an enum")
                void shouldThrowExceptionIfGenericArgumentIsInvalid() {
                    // Given
                    Injector sut = new InjectorImpl();
                    TypeLiteral<Holder<TestEnum>> typeLiteral = new TypeLiteral<Holder<TestEnum>>() {};

                    // When / Then
                    InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(typeLiteral));
                    assertTrue(thrown.getMessage().contains("Cannot inject an enum"), "Should contain 'Cannot inject an enum': " + thrown.getMessage());
                }

                @Test
                @DisplayName("Should pass if generic argument is a nested ParameterizedType")
                void shouldPassIfGenericArgumentIsNestedParameterizedType() {
                    // Given
                    Injector sut = new InjectorImpl();
                    TypeLiteral<Holder<Holder<String>>> typeLiteral = new TypeLiteral<Holder<Holder<String>>>() {};

                    // When / Then
                    assertDoesNotThrow(() -> sut.inject(typeLiteral));
                }

                @Test
                @DisplayName("Should pass if generic argument is a WildcardType (ignored by validation)")
                void shouldPassIfGenericArgumentIsWildcardType() {
                    // Given
                    Injector sut = new InjectorImpl();
                    // Holder<?>
                    ParameterizedType wildcardType = (ParameterizedType) new TypeLiteral<Holder<?>>() {}.getType();

                    // When / Then
                    // We can't use sut.inject(wildcardType) directly because it expects TypeLiteral or Class
                    // but sut.inject(TypeLiteral) calls checkClassValidity(type)
                    assertDoesNotThrow(() -> sut.inject(new TypeLiteral<Holder<?>>() {}));
                }

                @Test
                @DisplayName("Should pass if generic argument is a GenericArrayType (ignored by validation)")
                void shouldPassIfGenericArgumentIsGenericArrayType() {
                    // Given
                    Injector sut = new InjectorImpl();
                    // Holder<String[]> - actually String[] is a Class, not GenericArrayType.
                    // We need something like Holder<List<String>[]> to get a GenericArrayType arg
                    
                    // When / Then
                    assertDoesNotThrow(() -> sut.inject(new TypeLiteral<Holder<List<String>[]>>() {}));
                }
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
        void shouldReturnConstructorAnnotatedWithInject() {
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
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.getConstructor(TestClass.class));
            assertTrue(thrown.getMessage().startsWith("More than one constructor annotated with @Inject in class"), "Should start with 'More than one constructor annotated with @Inject in class': " + thrown.getMessage());
        }

        /**
         * If more constructors are available, the Injector will choose the one annotated with @Inject.
         */
        @Test
        @DisplayName("Should return constructor marked with @Inject when available, preferring it to the no-arguments constructor")
        void shouldReturnConstructorAnnotatedWithInjectPreferringItToTheNoArgsConstructor() {
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
        void shouldUseNoArgsConstructorIfNoCompatibleConstructorFound() {
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
        @DisplayName("Should throw InjectionException if no constructor annotated with @Inject is found and no-arguments constructor is not available")
        void shouldThrowInjectionExceptionIfNoCompatibleConstructorFound() {
            // Given
            @SuppressWarnings("unused")
            class TestClass {
                TestClass(int i) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.getConstructor(TestClass.class));
            assertTrue(thrown.getMessage().startsWith("No empty constructor or a constructor annotated with @Inject in class"), "No empty constructor or a constructor annotated with @Inject in class': " + thrown.getMessage());
        }

        /**
         * Once the constructor was identified, the parameters will be checked, and if any of them is not an interface,
         * an abstract class, or a concrete class, an InjectionException will be thrown.
         */
        @Test
        @DisplayName("Should throw InjectionException if any parameter is not an interface, an abstract or a concrete class")
        void shouldThrowIllegalArgumentExceptionIfInvalidParameter() {
            // Given
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(TestClassWithInvalidParametersInConstructor.class));
            assertTrue(thrown.getMessage().endsWith("Cannot inject a primitive"), "Should end with 'Cannot inject a primitive': " + thrown.getMessage());
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
        void shouldInstantiateAnObjectWithNoArgsConstructorIfNoOtherAnnotatedConstructorsArePresent() {
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
        void shouldInstantiateAnObjectWithAnnotatedNoArgsConstructor() {
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
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndConcreteDependency() {
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
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependency() {
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
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault() {
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
         * will resolve the implementation based on the annotations on the class and its dependencies. Normally it uses
         * the default implementation (not annotated with {@link Named}).
         */
        @Test
        @DisplayName("Should instantiate an object with an annotated constructor and an abstract dependency with more implementations (get alternative)")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.enableAlternative(MultipleAlternativesAlternativeImplementation.class);
            // When
            TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsDefault.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getMultipleAnnotatedImplementationsInterface());
            assertEquals(MultipleAlternativesAlternativeImplementation.class, instance.getMultipleAnnotatedImplementationsInterface().getClass());
        }

        /**
         * If we have a dependency on an interface or abstract class that has more than one implementation, the Injector
         * will resolve the implementation based on the annotations on the class and its dependencies. In this case
         * we specify to use a particular alternative implementation.
         */
        @Test
        @DisplayName("Should instantiate an object with an annotated constructor and an abstract dependency (get named2 implementation)")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsNamed2() {
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
        void shouldInstantiateASingletonClass() {
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
        void shouldInstantiateANonSingletonClassAsMuchAsNeeded() {
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
            void shouldInstantiateObjectWithPrivateConstructorAndNoDependencies() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                // When
                ClassWithPrivateConstructor instance = sut.inject(ClassWithPrivateConstructor.class);
                // Then
                assertNotNull(instance);

            }

            @Test
            @DisplayName("Should instantiate an object with private constructor and dependencies")
            void shouldInstantiateObjectWithPrivateConstructorAndDependencies() {
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

    @Nested
    @DisplayName("Field injection")
    class FieldInjectionTests {

        @Test
        @DisplayName("Should throw InjectionException if trying to inject an invalid field")
        void shouldThrowInjectionExceptionIfInjectingAnInvalidField() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // Then
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(ClassWithPrimitiveType.class));
            assertTrue(thrown.getMessage().endsWith("Cannot inject a primitive"), "Should end with 'Cannot inject a primitive': " + thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw InjectionException if trying to inject a final field")
        void shouldThrowInjectionExceptionIfInjectingAFinalField() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // Then
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(ClassWithFinalField.class));
            assertTrue(thrown.getMessage().contains("Cannot inject into final field"), "Should contain 'Cannot inject into final field': " + thrown.getMessage());
        }

        @Test
        @DisplayName("Should inject a static field (discouraged)")
        void shouldInjectAStaticField() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ClassWithStaticField instance = sut.inject(ClassWithStaticField.class);
            // Then
            assertNotNull(instance.getStaticField());
        }

        @Test
        @DisplayName("Should inject static fields only once")
        void shouldInjectStaticFieldOnlyOnce() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithStaticField instance = sut.inject(ClassWithStaticField.class);
            instance.setStaticField(null);
            // When
            sut.inject(ClassWithStaticField.class);
            // Then
            assertNull(instance.getStaticField());
        }

        @Test
        @DisplayName("Should inject fields")
        void shouldInjectFields() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ClassWithCorrectDependencies instance = sut.inject(ClassWithCorrectDependencies.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getFirstDependency());
            assertInstanceOf(ClassFirstDependency.class, instance.getFirstDependency());
            assertNotNull(instance.getSecondDependency());
            assertInstanceOf(ClassSecondDependency.class, instance.getSecondDependency());
        }
    }

    @Nested
    @DisplayName("Method injection")
    class MethodInjectionTests {

        @Test
        @DisplayName("Should throw InjectionException if trying to inject an invalid parameter")
        void shouldThrowExceptionIfInjectingAnInvalidField() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // Then
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(ClassWithMethodWithInvalidParameter.class));
            assertTrue(thrown.getMessage().endsWith("Cannot inject a primitive"), "Should end with 'Cannot inject a primitive': " + thrown.getMessage());
        }

        @Test
        @DisplayName("Should inject a method")
        void shouldInjectAMethod() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ClassWithMethodWithValidParameters instance = sut.inject(ClassWithMethodWithValidParameters.class);
            FirstMethodParameter firstMethodParameter = instance.getFirstMethodParameter();
            SecondMethodParameter secondMethodParameter = instance.getSecondMethodParameter();
            // Then
            assertNotNull(instance);
            assertNotNull(firstMethodParameter);
            assertNotNull(secondMethodParameter);
        }

        @Test
        @DisplayName("Should inject a static method (discouraged)")
        void shouldInjectAStaticMethod() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            sut.inject(ClassWithStaticMethod.class);
            // Then
            assertNotNull(ClassWithStaticMethod.getStaticField());
        }

        @Test
        @DisplayName("Should inject static methods only once")
        @SuppressWarnings("all")
        void shouldInjectStaticMethodsOnlyOnce() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithStaticMethod instance = sut.inject(ClassWithStaticMethod.class);
            instance.setStaticField(null);
            // When
            sut.inject(ClassWithStaticMethod.class);
            // Then
            assertNull(instance.getStaticField());
        }

    }

    @Nested
    @DisplayName("Inheritance Scanning Tests")
    class InheritanceScanningTests {

        @Test
        @DisplayName("Should scan for and inject methods from parent classes")
        void shouldScanForAndInjectMethodsFromParentClasses() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            MyClass instance = sut.inject(MyClass.class);
            // Then
            assertNotNull(instance.getGrandparentFieldClassInjectedByMethod());
            assertNotNull(instance.getParentFieldClassInjectedByMethod());
            assertNotNull(instance.getFieldClassInjectedByMethod());
        }

        @Test
        @DisplayName("Should scan for and inject fields from parent classes")
        void shouldScanForAndInjectFieldsFromParentClasses() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            MyClass instance = sut.inject(MyClass.class);
            // Then
            assertNotNull(instance.getGrandparentFieldClass());
            assertNotNull(instance.getParentFieldClass());
            assertNotNull(instance.getFieldClass());
        }

    }

    @Nested
    @DisplayName("Generics Tests")
    class GenericsTests {

        @Test
        @DisplayName("Should inject generics")
        void shouldInjectGenerics() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Object1 object1 = new Object1();
            Object2 object2 = new Object2();
            // When
            GenericsClass instance = sut.inject(GenericsClass.class);
            assertNotNull(instance);
            assertNotNull(instance.getHolder1());
            assertNotNull(instance.getHolder2());
            instance.getHolder1().set(object1);
            instance.getHolder2().set(object2);
            assertEquals(object1, instance.getHolder1().get());
            assertEquals(object2, instance.getHolder2().get());
        }

        @Test
        @DisplayName("Should inject generics using TypeLiteral")
        void shouldInjectGenericsUsingTypeLiteral() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            TypeLiteral<Holder<Object1>> typeLiteral = new TypeLiteral<Holder<Object1>>() {};
            Object1 obj = new Object1();
            // When
            Holder<Object1> holder = sut.inject(typeLiteral);
            // Then
            assertNotNull(holder);
            holder.set(obj);
            assertEquals(obj, holder.get());
        }
    }

    /**
     * Alternatives
     */
    @Nested
    @DisplayName("Alternative Tests")
    class AlternativeTests {

        @Test
        @DisplayName("Should instantiate an object with standard implementation when alternatives are disabled")
        void shouldInstantiateAnObjectWithStandardImplementationWhenAlternativesAreDisabled() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedConstructorAndAlternativeDependency instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndAlternativeDependency.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getAlternativesTestInterface());
            assertEquals(AlternativesStandardImplementation.class, instance.getAlternativesTestInterface().getClass());
        }

        @Test
        @DisplayName("Should instantiate an object with alternative implementation when alternatives are enabled")
        void shouldInstantiateAnObjectWithAlternativeImplementationWhenAlternativesAreEnabled() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            TestClassWithAnnotatedConstructorAndAlternativeDependency instance =
                    sut.inject(TestClassWithAnnotatedConstructorAndAlternativeDependency.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getAlternativesTestInterface());
            assertEquals(AlternativesAlternativeImplementation1.class, instance.getAlternativesTestInterface().getClass());
        }
    }

    /**
     * Scopes
     */
    @Nested
    @DisplayName("Scope Tests")
    class ScopeTests {

        /**
         * Custom scope registration and usage - Verifies scope handlers are called and manage instances correctly.
         */
        @Test
        @DisplayName("Should register and use a custom scope")
        void shouldRegisterAndUseCustomScope() {
            // Given
            Map<Class<?>, Object> scopeStorage = new HashMap<>();
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            sut.registerScope(RequestScoped.class, new ScopeHandler() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    return (T) scopeStorage.computeIfAbsent(clazz, c -> provider.get());
                }
                @Override
                public void close() throws Exception {
                    // No-op for test
                }
            });
            RequestScopedClass instance1 = sut.inject(RequestScopedClass.class);
            RequestScopedClass instance2 = sut.inject(RequestScopedClass.class);
            // Then
            assertSame(instance1, instance2, "Should return same instance within scope");
            // Simulate scope end
            scopeStorage.clear();
            RequestScopedClass instance3 = sut.inject(RequestScopedClass.class);
            assertNotSame(instance1, instance3, "Should create new instance after scope cleared");
        }

        /**
         * Singleton precedence - Ensures the built-in Singleton scope works correctly
         */
        @Test
        @DisplayName("Should respect Singleton scope over custom scope")
        void shouldRespectSingletonScopeOverCustomScope() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            AtomicInteger callCount = new AtomicInteger(0);
            sut.registerScope(CustomScope.class, new ScopeHandler() {
                @Override
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    callCount.incrementAndGet();
                    return provider.get();
                }
                @Override
                public void close() throws Exception {
                    // No-op for test
                }
            });
            // When
            SingletonScopedClass instance1 = sut.inject(SingletonScopedClass.class);
            SingletonScopedClass instance2 = sut.inject(SingletonScopedClass.class);
            // Then
            assertSame(instance1, instance2);
            assertEquals(0, callCount.get(), "Custom scope should not be called for Singleton");
        }

        /**
         * Transitive scoped dependencies - Scoped dependencies maintain their scope even when injected into
         * non-scoped classes.
         */
        @Test
        @DisplayName("Should handle scoped dependencies transitively")
        void shouldHandleScopedDependenciesTransitively() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            NonScopedClass instance1 = sut.inject(NonScopedClass.class);
            NonScopedClass instance2 = sut.inject(NonScopedClass.class);
            // Then
            assertNotSame(instance1, instance2, "Non-scoped classes should be different");
            assertSame(instance1.getDependency(), instance2.getDependency(),
                    "Singleton dependencies should be same");
        }

        /**
         * Unregistered scope behavior - Falls back to creating new instances.
         */
        @Test
        @DisplayName("Should create new instances when no scope is registered")
        void shouldCreateNewInstancesWhenNoScopeRegistered() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            UnregisteredScopedClass instance1 = sut.inject(UnregisteredScopedClass.class);
            UnregisteredScopedClass instance2 = sut.inject(UnregisteredScopedClass.class);
            // Then
            assertNotSame(instance1, instance2,
                    "Should create new instances when scope handler not registered");
        }

        /**
         * Scope handler override - Allows re-registering scope handlers
         */
        @Test
        @DisplayName("Should allow overriding scope handlers")
        void shouldAllowOverridingScopeHandlers() {
            // Given
            AtomicInteger handler1Calls = new AtomicInteger(0);
            AtomicInteger handler2Calls = new AtomicInteger(0);
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.registerScope(TestScope.class, new ScopeHandler() {
                @Override
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    handler1Calls.incrementAndGet();
                    return provider.get();
                }
                @Override
                public void close() throws Exception {
                    // No-op for test
                }
            });
            sut.inject(TestScopedClass.class);
            // When - override handler
            sut.registerScope(TestScope.class, new ScopeHandler() {
                @Override
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    handler2Calls.incrementAndGet();
                    return provider.get();
                }
                @Override
                public void close() throws Exception {
                    // No-op for test
                }
            });
            sut.inject(TestScopedClass.class);
            // Then
            assertEquals(1, handler1Calls.get());
            assertEquals(1, handler2Calls.get());
        }
    }

    /**
     * Tests for circular dependency detection.
     */
    @Nested
    @DisplayName("Circular Dependency Tests")
    class CircularDependencyTests {

        @Test
        @DisplayName("Should detect a circular dependency")
        void shouldDetectCircularDependency() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When/Then
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(A.class));
            String containedMessage = A.class.getName() + " -> " + B.class.getName() + " -> " + A.class.getName();
            assertTrue(thrown.getMessage().contains(containedMessage), "Should contain message: " + containedMessage);
        }

        @Test
        @DisplayName("Should bypass a circular dependency with Providers")
        void shouldBypassCircularDependencyWithProviders() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            AWithBProvider a = sut.inject(AWithBProvider.class);
            BWithAProvider b = a.getB();
            // Then
            assertNotNull(a);
            assertNotNull(b);
        }
    }

    /**
     * Tests for the Instance functionality.
     * The Instance allows resolving interfaces or abstract classes.
     * Instance.get() returns the resolved instance of the interface or abstract class.
     * Instance.iterator(), if annotated with {@link Any}, returns an iterator over all the concrete implementations of
     * the interface or abstract class. Otherwise, the iterator will contain only one element (the default one, or
     * a {@link Named} if that option was specified).
     */
    @Nested
    @DisplayName("Instance Tests")
    class InstanceTests {

        /**
         * ClassWithInstance has a dependency that is an Instance of TestInterface. The only implementation of
         * TestInterface is TestClass.
         */
        @Test
        @DisplayName("Instance with single implementation")
        void instanceWithSingleImplementation() {
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
        void anyInstanceWithSingleImplementation() {
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
        void anyInstanceAndMultipleImplementations() {
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
         * annotated with {@link Named}. This test checks that the default implementation is returned when no @Named
         * is specified.
         */
        @Test
        @DisplayName("Default implementation should be returned when no @Named is specified")
        void defaultImplementationShouldBeReturnedWhenNoNamedIsSpecified() {
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
         * If, however, we specify a {@link Named} annotation, the alternative implementation should be returned.
         */
        @Test
        @DisplayName("Named implementation should be returned when @Named is specified")
        void alternativeImplementationShouldBeReturnedWhenNamedIsSpecified() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstanceAndMultipleImplementationsAlternative classWithAnyInstanceAndMultipleImplementations = sut.inject(ClassWithInstanceAndMultipleImplementationsAlternative.class);
            // When
            Instance<MultipleConcreteClassesAbstractClass> instance = classWithAnyInstanceAndMultipleImplementations.getInstance();
            // Then
            assertNotNull(instance);
            assertEquals(MultipleConcreteClassesNamed2.class, instance.get().getClass());
        }

        @Test
        @DisplayName("If resolved, Instance.isUnresolved() is false")
        void isUnresolvedIsFalse() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstanceOfTestInterface classWithInstanceOFTestInterface =
                    sut.inject(ClassWithInstanceOfTestInterface.class);
            // When
            Instance<TestInterface> instance = classWithInstanceOFTestInterface.getTestInterfaceInstance();
            // Then
            assertNotNull(instance);
            assertFalse(instance.isUnsatisfied());
        }

        @Nested
        @DisplayName("isUnresolved()")
        class IsUnresolvedTests {

            @Test
            @DisplayName("If resolved, Instance.isUnresolved() is false")
            void isUnresolvedIsFalse() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstanceOFTestInterface =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                // When
                Instance<TestInterface> instance = classWithInstanceOFTestInterface.getTestInterfaceInstance();
                // Then
                assertNotNull(instance);
                assertFalse(instance.isUnsatisfied());
            }

            /**
             * Instance.isUnresolved() is true.
             */
            @Test
            @DisplayName("If not resolved, Instance.isUnresolved() is true")
            void isUnresolvedIsTrue() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithAnyInstanceButNoImplementation instance = sut.inject(ClassWithAnyInstanceButNoImplementation.class);
                // Then
                assertTrue(instance.getInstance().isUnsatisfied());
            }

            @Test
            @DisplayName("If an exception is thrown, isUnsatisfied() is true")
            void ifExceptionThrownIsUnsatisfiedIsTrue() throws Exception {
                // Given
                ClassResolver mockResolver = spy(new ClassResolver());
                // resolveImplementations throws an exception
                doThrow(new Exception("Test exception"))
                        .when(mockResolver).resolveImplementations(any(Class.class));
                // resolveImplementation calls the real method
                doCallRealMethod()
                        .when(mockResolver).resolveImplementation(any(Class.class), any());
                doCallRealMethod()
                        .when(mockResolver).resolveImplementations(any(ClassLoader.class), any());
                Injector sut = new InjectorImpl(mockResolver);
                // When
                ClassWithAnyInstanceAndMultipleImplementations instance = sut.inject(ClassWithAnyInstanceAndMultipleImplementations.class);
                // Then
                assertTrue(instance.getInstance().isUnsatisfied());            }
        }

        @Nested
        @DisplayName("isAmbiguous()")
        class IsAmbiguousTests {

            @Test
            @DisplayName("If resolved to a single class, isAmbiguous() is false")
            void ifResolvedToASingleClassIsFalse() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstanceOFTestInterface =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                // When
                Instance<TestInterface> instance = classWithInstanceOFTestInterface.getTestInterfaceInstance();
                // Then
                assertNotNull(instance);
                assertFalse(instance.isAmbiguous());
            }

            @Test
            @DisplayName("If resolved to multiple classes, isAmbiguous() is true")
            void ifResolvedToMultipleClassesISAmbiguousIsTrue() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithAnyInstanceAndMultipleImplementations classWithAnyInstanceAndMultipleImplementations = sut.inject(ClassWithAnyInstanceAndMultipleImplementations.class);
                // When
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithAnyInstanceAndMultipleImplementations.getInstance();
                // Then
                assertNotNull(instance);
                assertTrue(instance.isAmbiguous());
            }

            @Test
            @DisplayName("If an exception is thrown, isAmbiguous() is false")
            void ifExceptionThrownIsAmbiguousIsFalse() throws Exception{
                // Given
                ClassResolver mockResolver = spy(new ClassResolver());
                // resolveImplementations throws an exception
                doThrow(new Exception("Test exception"))
                        .when(mockResolver).resolveImplementations(any(Class.class));
                // resolveImplementation calls the real method
                doCallRealMethod()
                        .when(mockResolver).resolveImplementation(any(Class.class), any());
                doCallRealMethod()
                        .when(mockResolver).resolveImplementation(any(ClassLoader.class), any(Class.class), any());
                Injector sut = new InjectorImpl(mockResolver);
                // When
                ClassWithAnyInstanceAndMultipleImplementations instance = sut.inject(ClassWithAnyInstanceAndMultipleImplementations.class);
                // Then
                assertFalse(instance.getInstance().isAmbiguous());
            }
        }

        @Nested
        @DisplayName("Instance.select() Tests")
        class InstanceSelectTests {

            @Test
            @DisplayName("Should select with annotation qualifier")
            void shouldSelectWithAnnotationQualifier() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsDefault classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When - select with Named annotation
                Instance<MultipleConcreteClassesAbstractClass> selected =
                        instance.select(new NamedLiteral("name2"));
                // Then
                assertNotNull(selected);
                assertEquals(MultipleConcreteClassesNamed2.class, selected.get().getClass());
            }

            @Test
            @DisplayName("Should select without annotation (keep current qualifier)")
            void shouldSelectWithoutAnnotation() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsAlternative classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsAlternative.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When - select without annotation (should keep @Named("name2"))
                Instance<MultipleConcreteClassesAbstractClass> selected = instance.select();
                // Then
                assertNotNull(selected);
                assertEquals(MultipleConcreteClassesNamed2.class, selected.get().getClass());
            }

            @Test
            @DisplayName("Should select with subtype and no annotation")
            void shouldSelectWithSubtypeNoAnnotation() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                // When - select concrete subtype
                Instance<TestClass> selected = instance.select(TestClass.class);
                // Then
                assertNotNull(selected);
                assertInstanceOf(TestClass.class, selected.get());
            }

            @Test
            @DisplayName("Should select with subtype and annotation")
            void shouldSelectWithSubtypeAndAnnotation() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsDefault classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When - select a specific subtype with annotation
                Instance<MultipleConcreteClassesNamed2> selected =
                        instance.select(MultipleConcreteClassesNamed2.class, new NamedLiteral("name2"));
                // Then
                assertNotNull(selected);
                assertEquals(MultipleConcreteClassesNamed2.class, selected.get().getClass());
            }

            @Test
            @DisplayName("Should chain multiple select calls")
            void shouldChainMultipleSelectCalls() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsDefault classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When - chain select calls
                Instance<MultipleConcreteClassesAbstractClass> selected = instance
                        .select(new NamedLiteral("name1"))
                        .select();  // Keep the qualifier from previous select
                // Then
                assertNotNull(selected);
                assertEquals(MultipleConcreteClassesNamed1.class, selected.get().getClass());
            }

            @Test
            @DisplayName("Should override qualifier in chained select")
            void shouldOverrideQualifierInChainedSelect() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsDefault classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When - chain select with different qualifiers
                Instance<MultipleConcreteClassesAbstractClass> selected = instance
                        .select(new NamedLiteral("name1"))
                        .select(new NamedLiteral("name2"));  // Override previous qualifier
                // Then
                assertNotNull(selected);
                assertEquals(MultipleConcreteClassesNamed2.class, selected.get().getClass());
            }

            @Test
            @DisplayName("Should select with TypeLiteral")
            void shouldSelectWithTypeLiteral() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                TypeLiteral<TestClass> typeLiteral = new TypeLiteral<TestClass>() {};
                // When
                Instance<TestClass> testClassInstance = instance.select(typeLiteral);
                // Then
                assertNotNull(testClassInstance);
            }

            @Test
            @DisplayName("Should find no matches with TypeLiteral")
            void shouldFindNoMatchesWithTypeLiteral() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                TypeLiteral<TestClass> typeLiteral = new TypeLiteral<TestClass>() {};
                // When
                Instance<TestClass> testClassInstance = instance.select(typeLiteral, new NamedLiteral("name1"), new NamedLiteral("name2"));
                // Then
                assertNotNull(testClassInstance);
            }

            @Test
            @DisplayName("Should fail when selecting non-existent implementation")
            void shouldFailWhenSelectingNonExistentImplementation() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsDefault classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When
                Instance<MultipleConcreteClassesAbstractClass> selected =
                        instance.select(new NamedLiteral("nonExistent"));
                // Then
                assertThrows(RuntimeException.class, selected::get);
            }

            @Test
            @DisplayName("Should maintain @Any behavior after select")
            void shouldMaintainAnyBehaviorAfterSelect() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithAnyInstanceAndMultipleImplementations classWithInstance =
                        sut.inject(ClassWithAnyInstanceAndMultipleImplementations.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // When - select on @Any instance
                Instance<MultipleConcreteClassesAbstractClass> selected =
                        instance.select(new NamedLiteral("name1"));
                // Then
                assertNotNull(selected);
                assertEquals(MultipleConcreteClassesNamed1.class, selected.get().getClass());
            }
        }

        @Nested
        @DisplayName("Instance.destroy() Tests")
        class InstanceDestroyTests {

            @Test
            @DisplayName("Should not throw exception when destroying instance")
            void shouldNotThrowExceptionWhenDestroyingInstance() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                TestInterface obj = instance.get();
                // When/Then
                assertDoesNotThrow(() -> instance.destroy(obj));
            }

            @Test
            @DisplayName("Should accept null in destroy method")
            void shouldAcceptNullInDestroyMethod() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                // Then
                assertDoesNotThrow(() -> instance.destroy(null));
            }

            @Test
            @DisplayName("Should not affect instance after destroy is called")
            void shouldNotAffectInstanceAfterDestroyIsCalled() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                TestInterface obj1 = instance.get();
                // When
                instance.destroy(obj1);
                // Then - can still get new instances after destruction
                TestInterface obj2 = instance.get();
                assertNotNull(obj2);
                assertNotSame(obj1, obj2, "Should create new instance after destroy");
            }

            @Test
            @DisplayName("Should allow destroying same instance multiple times")
            void shouldAllowDestroyingSameInstanceMultipleTimes() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceOfTestInterface classWithInstance =
                        sut.inject(ClassWithInstanceOfTestInterface.class);
                Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
                TestInterface obj = instance.get();
                // When/Then - multiple destroy calls should not throw
                assertDoesNotThrow(() -> {
                    instance.destroy(obj);
                    instance.destroy(obj);
                    instance.destroy(obj);
                });
            }

            @Test
            @DisplayName("Should not affect singleton instances after destroy")
            void shouldNotAffectSingletonInstancesAfterDestroy() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                SingletonDependency obj1 = sut.inject(SingletonDependency.class);
                // Create instance wrapper manually for testing
                ClassWithInstanceOfSingleton classWithInstance =
                        sut.inject(ClassWithInstanceOfSingleton.class);
                Instance<SingletonDependency> instance = classWithInstance.getInstance();
                // When
                instance.destroy(obj1);
                // Then - singleton should still return the same instance
                SingletonDependency obj2 = sut.inject(SingletonDependency.class);
                assertSame(obj1, obj2, "Singleton should remain same after destroy");
            }

            @Test
            @DisplayName("Should allow destroying instance from different implementation")
            void shouldAllowDestroyingInstanceFromDifferentImplementation() {
                // Given
                Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                ClassWithInstanceAndMultipleImplementationsDefault classWithInstance =
                        sut.inject(ClassWithInstanceAndMultipleImplementationsDefault.class);
                Instance<MultipleConcreteClassesAbstractClass> instance = classWithInstance.getInstance();
                // Create different implementation directly
                MultipleConcreteClassesNamed1 differentImpl = new MultipleConcreteClassesNamed1();
                // When/Then - should not throw even if the instance wasn't created by this Instance
                assertDoesNotThrow(() -> instance.destroy(differentImpl));
            }
        }

        /**
         * When we have to deal with an Instance of an abstract class or interface with no concrete implementations:
         */
        @Nested
        @DisplayName("Instance with no concrete implementations")
        class InstancesWithNoConcreteImplementations {

            @Test
            @DisplayName("Instance.iterator() should throw RuntimeException if resolution fails")
            @SuppressWarnings("unchecked")
            void iteratorShouldThrowRuntimeExceptionIfResolutionFails() throws Exception {
                // Given
                ClassResolver mockResolver = mock(ClassResolver.class);

                // Ensure the initial injection of the test class succeeds.
                // resolveImplementation(Type, String, Collection)
                when(mockResolver.resolveImplementation(
                        any(java.lang.reflect.Type.class),
                        nullable(Collection.class)
                )).thenAnswer(invocation -> invocation.getArgument(0));

                // Force failure on resolveImplementations(Type, String, Collection)
                // We use explicit types to avoid ambiguity with resolveImplementations(ClassLoader, Type, String)
                when(mockResolver.resolveImplementations(
                        any(java.lang.reflect.Type.class),
                        nullable(Collection.class)
                )).thenThrow(new Exception("Resolution failed"));

                InjectorImpl sut = new InjectorImpl(mockResolver);
                ClassWithInstanceOfTestInterface instanceWrapper = sut.inject(ClassWithInstanceOfTestInterface.class);
                javax.enterprise.inject.Instance<TestInterface> instance = instanceWrapper.getTestInterfaceInstance();

                // When / Then
                assertThrows(RuntimeException.class, instance::iterator);
            }

            @Nested
            @DisplayName("@Any Instance")
            class AnyInstance {

                /**
                 * Instance.get() throws an exception since we can't return any implementation.
                 */
                @Test
                @DisplayName("Instance.get() should fail")
                void instanceGetShouldFail() {
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
                void instanceIteratorHasNextShouldReturnFalse() {
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
                void instanceGetShouldFail() {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithInstanceButNoImplementation instance = sut.inject(ClassWithInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().get());
                }

                /**
                 * Instance.iterator().hasNext() returns false since we don't have a valid implementation.
                 */
                @Test
                @DisplayName("Instance.iterator().hasNext() should return false")
                void instanceIteratorNextShouldThrowException() {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithInstanceButNoImplementation instance = sut.inject(ClassWithInstanceButNoImplementation.class);
                    Iterator<NoConcreteClassesAbstractClass> iterator = instance.getInstance().iterator();
                    // Then
                    assertFalse(iterator.hasNext());
                }
            }
        }
    }

    @Nested
    @DisplayName("Inner workings tests")
    class InnerWorkingsTests {

        @Nested
        @DisplayName("getQualifiers")
        class GetQualifiersTests {

            @Test
            @DisplayName("Should return DefaultLiteral if field has no qualifiers")
            @SuppressWarnings("unchecked")
            void shouldReturnDefaultLiteralIfFieldHasNoQualifiers() throws Exception {
                class Test { @Inject ClassFirstDependency s; }
                InjectorImpl sut = new InjectorImpl();
                java.lang.reflect.Field field = Test.class.getDeclaredField("s");

                java.lang.reflect.Method getQualifiers = InjectorImpl.class.getDeclaredMethod("getQualifiers", java.lang.reflect.Field.class);
                getQualifiers.setAccessible(true);

                Collection<Annotation> qualifiers = (Collection<Annotation>) getQualifiers.invoke(sut, field);
                assertEquals(1, qualifiers.size());
                assertInstanceOf(DefaultLiteral.class, qualifiers.iterator().next());
            }

            @Test
            @DisplayName("Should return specific qualifier if field is annotated")
            @SuppressWarnings("unchecked")
            void shouldReturnSpecificQualifierIfFieldIsAnnotated() throws Exception {
                class Test { @Inject @Named("test") ClassFirstDependency s; }
                InjectorImpl sut = new InjectorImpl();
                java.lang.reflect.Field field = Test.class.getDeclaredField("s");

                java.lang.reflect.Method getQualifiers = InjectorImpl.class.getDeclaredMethod("getQualifiers", java.lang.reflect.Field.class);
                getQualifiers.setAccessible(true);

                Collection<Annotation> qualifiers = (Collection<Annotation>) getQualifiers.invoke(sut, field);
                assertEquals(1, qualifiers.size());
                assertEquals("test", ((Named) qualifiers.iterator().next()).value());
            }

            @Test
            @DisplayName("Should return DefaultLiteral if parameter has no qualifiers")
            @SuppressWarnings("unchecked")
            void shouldReturnDefaultLiteralIfParameterHasNoQualifiers() throws Exception {
                class Test { @Inject Test(ClassFirstDependency s) {} }
                InjectorImpl sut = new InjectorImpl();
                java.lang.reflect.Parameter param = Test.class.getDeclaredConstructors()[0].getParameters()[0];

                java.lang.reflect.Method getQualifiers = InjectorImpl.class.getDeclaredMethod("getQualifiers", java.lang.reflect.Parameter.class);
                getQualifiers.setAccessible(true);

                Collection<Annotation> qualifiers = (Collection<Annotation>) getQualifiers.invoke(sut, param);
                assertEquals(1, qualifiers.size());
                assertInstanceOf(DefaultLiteral.class, qualifiers.iterator().next());
            }
        }
        @Nested
        @DisplayName("mergeQualifiers")
        class MergeQualifiersTests {

            @Test
            @DisplayName("mergeQualifiers should return existing if newAnnotations is null")
            void mergeQualifiersShouldReturnExistingIfNewAnnotationsIsNull() {
                // Given
                Collection<Annotation> existing = Collections.singletonList(new NamedLiteral("test"));
                Annotation[] newAnnotations = null;
                // When
                Collection<Annotation> merged = new InjectorImpl(TEST_PACKAGE_NAME).mergeQualifiers(existing, newAnnotations);
                // Then
                assertSame(existing, merged);
            }

            @Test
            @DisplayName("mergeQualifiers should return existing if newAnnotations is empty")
            void mergeQualifiersShouldReturnExistingIfNewAnnotationsIsEmpty() {
                // Given
                Collection<Annotation> existing = Collections.singletonList(new NamedLiteral("test"));
                Annotation[] newAnnotations = {};
                // When
                Collection<Annotation> merged = new InjectorImpl(TEST_PACKAGE_NAME).mergeQualifiers(existing, newAnnotations);
                // Then
                assertSame(existing, merged);
            }

            @Test
            @DisplayName("mergeQualifiers should merge existing qualifiers with new ones")
            void mergeQualifiersShouldMergeExistingQualifiersWithNewOnes() {
                // Given
                Collection<Annotation> existing = Collections.singletonList(new NamedLiteral("test"));
                Annotation[] newAnnotations = {new NamedLiteral("test2")};
                // When
                Collection<Annotation> merged = new InjectorImpl(TEST_PACKAGE_NAME).mergeQualifiers(existing, newAnnotations);
                // Then
                assertEquals(1, merged.size());
                assertFalse(merged.contains(new NamedLiteral("test")));
                assertTrue(merged.contains(new NamedLiteral("test2")));
            }

            @Test
            @DisplayName("mergeQualifiers should remove Default")
            void mergeQualifiersShouldRemoveDefault() {
                // Given
                Collection<Annotation> existing = Collections.singletonList(new DefaultLiteral());
                Annotation[] newAnnotations = {new NamedLiteral("test2")};
                // When
                Collection<Annotation> merged = new InjectorImpl(TEST_PACKAGE_NAME).mergeQualifiers(existing, newAnnotations);
                // Then
                assertEquals(1, merged.size());
                assertFalse(merged.contains(new DefaultLiteral()));
                assertTrue(merged.contains(new NamedLiteral("test2")));
            }

        }

    }

    @Nested
    @DisplayName("Misc")
    class MiscTests {

        @Test
        @DisplayName("Miscellaneous")
        void miscellaneousTests() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // List is an interface, so we must bind it to a concrete implementation
            sut.bind(new TypeLiteral<List<? extends Number>>() {}.getType(),
                    Collections.singleton(new DefaultLiteral()),
                    ArrayList.class);

            // GenericService<String> is what we want
            TypeLiteral<GenericService<String>> typeLiteral = new TypeLiteral<GenericService<String>>() {};

            // When
            GenericService<String> service = sut.inject(typeLiteral);

            // Then
            assertNotNull(service);
            assertDoesNotThrow(service::run);
        }

    }

    @Nested
    @DisplayName("Shutdown and Lifecycle Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Should call @PreDestroy on all singletons during shutdown")
        void shouldCallPreDestroyOnAllSingletonsDuringShutdown() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SingletonWithPreDestroy singleton1 = sut.inject(SingletonWithPreDestroy.class);
            AnotherSingletonWithPreDestroy singleton2 = sut.inject(AnotherSingletonWithPreDestroy.class);
            // When
            sut.shutdown();
            // Then
            assertTrue(singleton1.isPreDestroyCalled());
            assertTrue(singleton2.isPreDestroyCalled());
        }

        @Test
        @DisplayName("Should handle singletons without @PreDestroy gracefully")
        void shouldHandleSingletonsWithoutPreDestroyGracefully() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.inject(SingletonClass.class); // No @PreDestroy method
            // When/Then
            assertDoesNotThrow(sut::shutdown);
        }

        @Test
        @DisplayName("Should continue shutdown even if @PreDestroy throws exception")
        void shouldContinueShutdownEvenIfPreDestroyThrowsException() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SingletonWithFailingPreDestroy failing = sut.inject(SingletonWithFailingPreDestroy.class);
            SingletonWithPreDestroy working = sut.inject(SingletonWithPreDestroy.class);
            // When
            sut.shutdown();
            // Then - Both @PreDestroy methods should have been called despite exception
            assertTrue(failing.isPreDestroyCalled());
            assertTrue(working.isPreDestroyCalled());
        }

        @Test
        @DisplayName("Should clear singleton instances after shutdown")
        void shouldClearSingletonInstancesAfterShutdown() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SingletonWithPreDestroy singleton1 = sut.inject(SingletonWithPreDestroy.class);
            // When
            sut.shutdown();
            SingletonWithPreDestroy singleton2 = sut.inject(SingletonWithPreDestroy.class);
            // Then
            assertNotSame(singleton1, singleton2, "Should create new singleton after shutdown");
        }

        @Test
        @DisplayName("Should call close() on custom scope handlers")
        void shouldCallCloseOnCustomScopeHandlers() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            MockScopeHandler mockHandler = new MockScopeHandler();
            sut.registerScope(RequestScoped.class, mockHandler);
            // When
            sut.shutdown();
            // Then
            assertTrue(mockHandler.isCloseCalled());
        }

        @Test
        @DisplayName("Should handle null scope handlers gracefully")
        void shouldHandleNullScopeHandlersGracefully() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // Register and then don't use the scope
            // When/Then
            assertDoesNotThrow(sut::shutdown);
        }

        @Test
        @DisplayName("Should continue closing scopes even if one throws exception")
        void shouldContinueClosingScopesEvenIfOneThrowsException() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            TrackedScopeHandler handler1 = new TrackedScopeHandler(true); // Will throw
            TrackedScopeHandler handler2 = new TrackedScopeHandler(false); // Won't throw
            sut.registerScope(RequestScoped.class, handler1);
            sut.registerScope(TestScope.class, handler2);
            // When
            sut.shutdown();
            // Then
            assertTrue(handler1.isCloseCalled());
            assertTrue(handler2.isCloseCalled());
        }

        @Test
        @DisplayName("Should handle singletons with dependencies during shutdown")
        void shouldHandleSingletonsWithDependenciesDuringShutdown() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SingletonWithPreDestroyAndDependencies singleton = sut.inject(SingletonWithPreDestroyAndDependencies.class);
            // When
            sut.shutdown();
            // Then
            assertTrue(singleton.isPreDestroyCalled());
        }

        @Test
        @DisplayName("Should allow multiple shutdown() calls safely")
        void shouldAllowMultipleShutdownCallsSafely() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.inject(SingletonWithPreDestroy.class);
            // When/Then
            assertDoesNotThrow(() -> {
                sut.shutdown();
                sut.shutdown();
                sut.shutdown();
            });
        }

        @Test
        @DisplayName("Should register shutdown hook on construction")
        void shouldRegisterShutdownHookOnConstruction() {
            // Given/When
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // Then - Can't directly verify hook registration, but ensure method exists and doesn't throw
            assertDoesNotThrow(sut::addShutdownHook);
        }

        @Test
        @DisplayName("Should cleanup custom scope with @PreDestroy via close()")
        void shouldCleanupCustomScopeWithPreDestroyViaClose() {
            // Given
            Map<Class<?>, Object> scopeStorage = new HashMap<>();
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.registerScope(RequestScoped.class, new ScopeHandler() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    return (T) scopeStorage.computeIfAbsent(clazz, c -> provider.get());
                }
                @Override
                public void close() throws Exception {
                    scopeStorage.clear();
                }
            });
            RequestScopedWithPreDestroy instance = sut.inject(RequestScopedWithPreDestroy.class);
            // When
            sut.shutdown();
            // Then
            assertTrue(scopeStorage.isEmpty(), "Scope storage should be cleared");
        }

        @Test
        @DisplayName("Should call @PostConstruct after injection")
        void shouldCallPostConstructAfterInjection() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ClassWithPostConstruct instance = sut.inject(ClassWithPostConstruct.class);
            // Then
            assertTrue(instance.isPostConstructCalled());
        }

        @Test
        @DisplayName("Should call @PostConstruct on parent class before child class")
        void shouldCallPostConstructOnParentBeforeChild() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ChildWithPostConstruct instance = sut.inject(ChildWithPostConstruct.class);
            // Then
            assertTrue(instance.isParentPostConstructCalled());
            assertTrue(instance.isChildPostConstructCalled());
            assertTrue(instance.getParentCallOrder() < instance.getChildCallOrder(),
                    "Parent @PostConstruct should be called before child");
        }

        @Test
        @DisplayName("Should handle @PostConstruct throwing exception")
        void shouldHandlePostConstructThrowingException() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When/Then
            assertThrows(Exception.class, () -> sut.inject(ClassWithFailingPostConstruct.class));
        }

        @Test
        @DisplayName("Should call @PreDestroy on parent class before child class")
        void shouldCallPreDestroyOnParentBeforeChild() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ChildWithPreDestroy instance = sut.inject(ChildWithPreDestroy.class);
            // When
            sut.shutdown();
            // Then
            assertTrue(instance.isParentPreDestroyCalled());
            assertTrue(instance.isChildPreDestroyCalled());
            assertTrue(instance.getParentDestroyOrder() < instance.getChildDestroyOrder(),
                    "Parent @PreDestroy should be called before child");
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
        @SuppressWarnings("all")
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
        public TestClassWithAnnotatedConstructorAndAbstractDependencyWithMultipleImplementationsAlternative(@Named("name2") MultipleImplementationsInterface testInterface) {
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
        public ClassWithInstanceAndMultipleImplementationsAlternative(@Named(value = "name2") Instance<MultipleConcreteClassesAbstractClass> instance) {
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

    static class TestClassWithAnnotatedConstructorAndAlternativeDependency {
        private final AlternativesInterface testInterface;
        @Inject
        public TestClassWithAnnotatedConstructorAndAlternativeDependency(AlternativesInterface testInterface) {
            this.testInterface = testInterface;
        }
        public AlternativesInterface getAlternativesTestInterface() {
            return testInterface;
        }
    }

    static class ClassWithPrivateConstructor {
        @Inject
        private ClassWithPrivateConstructor() { }
    }

    // Helper classes for shutdown tests
    @Singleton
    static class SingletonWithPreDestroy {
        private boolean preDestroyCalled = false;

        @javax.annotation.PreDestroy
        public void preDestroy() {
            preDestroyCalled = true;
        }

        public boolean isPreDestroyCalled() {
            return preDestroyCalled;
        }
    }

    @Singleton
    static class AnotherSingletonWithPreDestroy {
        private boolean preDestroyCalled = false;

        @javax.annotation.PreDestroy
        public void preDestroy() {
            preDestroyCalled = true;
        }

        public boolean isPreDestroyCalled() {
            return preDestroyCalled;
        }
    }

    @Singleton
    static class SingletonWithFailingPreDestroy {
        private boolean preDestroyCalled = false;

        @javax.annotation.PreDestroy
        public void preDestroy() {
            preDestroyCalled = true;
            throw new RuntimeException("PreDestroy failed intentionally");
        }

        public boolean isPreDestroyCalled() {
            return preDestroyCalled;
        }
    }

    @Singleton
    static class SingletonWithPreDestroyAndDependencies {
        private final TestClass dependency;
        private boolean preDestroyCalled = false;

        @Inject
        public SingletonWithPreDestroyAndDependencies(TestClass dependency) {
            this.dependency = dependency;
        }

        @javax.annotation.PreDestroy
        public void preDestroy() {
            preDestroyCalled = true;
        }

        public boolean isPreDestroyCalled() {
            return preDestroyCalled;
        }

        public TestClass getDependency() {
            return dependency;
        }
    }

    @RequestScoped
    static class RequestScopedWithPreDestroy {
        private boolean preDestroyCalled = false;

        @javax.annotation.PreDestroy
        public void preDestroy() {
            preDestroyCalled = true;
        }

        public boolean isPreDestroyCalled() {
            return preDestroyCalled;
        }
    }

    static class MockScopeHandler implements ScopeHandler {
        private boolean closeCalled = false;

        @Override
        public <T> T get(Class<T> clazz, Supplier<T> provider) {
            return provider.get();
        }

        @Override
        public void close() throws Exception {
            closeCalled = true;
        }

        public boolean isCloseCalled() {
            return closeCalled;
        }
    }

    static class TrackedScopeHandler implements ScopeHandler {
        private boolean closeCalled = false;
        private final boolean throwOnClose;

        public TrackedScopeHandler(boolean throwOnClose) {
            this.throwOnClose = throwOnClose;
        }

        @Override
        public <T> T get(Class<T> clazz, Supplier<T> provider) {
            return provider.get();
        }

        @Override
        public void close() throws Exception {
            closeCalled = true;
            if (throwOnClose) {
                throw new Exception("Close failed intentionally");
            }
        }

        public boolean isCloseCalled() {
            return closeCalled;
        }
    }

    @Singleton
    static class ClassWithInstanceOfSingleton {
        private final Instance<SingletonDependency> instance;

        @Inject
        public ClassWithInstanceOfSingleton(Instance<SingletonDependency> instance) {
            this.instance = instance;
        }

        public Instance<SingletonDependency> getInstance() {
            return instance;
        }
    }

    @Singleton
    static class SingletonDependency {
    }

    // Helper classes for @PostConstruct tests
    static class ClassWithPostConstruct {
        private boolean postConstructCalled = false;

        @javax.annotation.PostConstruct
        public void postConstruct() {
            postConstructCalled = true;
        }

        public boolean isPostConstructCalled() {
            return postConstructCalled;
        }
    }

    static class ParentWithPostConstruct {
        private static int callCounter = 0;
        private boolean parentPostConstructCalled = false;
        private int parentCallOrder = 0;

        @javax.annotation.PostConstruct
        public void parentPostConstruct() {
            parentPostConstructCalled = true;
            parentCallOrder = callCounter++;
        }

        public boolean isParentPostConstructCalled() {
            return parentPostConstructCalled;
        }

        public int getParentCallOrder() {
            return parentCallOrder;
        }
    }

    static class ChildWithPostConstruct extends ParentWithPostConstruct {
        private boolean childPostConstructCalled = false;
        private int childCallOrder = 0;

        @javax.annotation.PostConstruct
        public void childPostConstruct() {
            childPostConstructCalled = true;
            childCallOrder = ParentWithPostConstruct.callCounter++;
        }

        public boolean isChildPostConstructCalled() {
            return childPostConstructCalled;
        }

        public int getChildCallOrder() {
            return childCallOrder;
        }
    }

    static class ClassWithFailingPostConstruct {
        @javax.annotation.PostConstruct
        public void postConstruct() {
            throw new RuntimeException("PostConstruct failed intentionally");
        }
    }

    @Singleton
    static class ParentWithPreDestroy {
        private static int destroyCounter = 0;
        private boolean parentPreDestroyCalled = false;
        private int parentDestroyOrder = 0;

        @javax.annotation.PreDestroy
        public void parentPreDestroy() {
            parentPreDestroyCalled = true;
            parentDestroyOrder = destroyCounter++;
        }

        public boolean isParentPreDestroyCalled() {
            return parentPreDestroyCalled;
        }

        public int getParentDestroyOrder() {
            return parentDestroyOrder;
        }
    }

    @Singleton
    static class ChildWithPreDestroy extends ParentWithPreDestroy {
        private boolean childPreDestroyCalled = false;
        private int childDestroyOrder = 0;

        @javax.annotation.PreDestroy
        public void childPreDestroy() {
            childPreDestroyCalled = true;
            childDestroyOrder = ParentWithPreDestroy.destroyCounter++;
        }

        public boolean isChildPreDestroyCalled() {
            return childPreDestroyCalled;
        }

        public int getChildDestroyOrder() {
            return childDestroyOrder;
        }
    }
}
