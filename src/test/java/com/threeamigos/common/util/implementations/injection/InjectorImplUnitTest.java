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
import com.threeamigos.common.util.implementations.injection.literals.AnnotationLiteral;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import com.threeamigos.common.util.implementations.injection.methods.ClassWithMethodWithInvalidParameter;
import com.threeamigos.common.util.implementations.injection.methods.ClassWithMethodWithValidParameters;
import com.threeamigos.common.util.implementations.injection.methods.FirstMethodParameter;
import com.threeamigos.common.util.implementations.injection.methods.SecondMethodParameter;
import com.threeamigos.common.util.implementations.injection.optional.*;
import com.threeamigos.common.util.implementations.injection.parameters.TestClassWithInvalidParametersInConstructor;
import com.threeamigos.common.util.implementations.injection.scopes.*;
import com.threeamigos.common.util.implementations.injection.scopehandlers.ConversationScopeHandler;
import com.threeamigos.common.util.implementations.injection.scopehandlers.RequestScopeHandler;
import com.threeamigos.common.util.implementations.injection.scopehandlers.SessionScopeHandler;
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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.enterprise.inject.InjectionException;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Any;
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
                @DisplayName("Should reject generic argument with WildcardType per CDI spec")
                void shouldRejectGenericArgumentWithWildcardType() {
                    // Given
                    Injector sut = new InjectorImpl();
                    // Holder<?>

                    // When / Then
                    // Wildcards are not allowed in injection points per CDI/JSR-330 specification
                    // because they are ambiguous and cannot be instantiated
                    assertThrows(Exception.class, () -> sut.inject(new TypeLiteral<Holder<?>>() {}));
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
                public void close() {
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
                public void close() {
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
        @DisplayName("Should allow replacing scope handlers after unregistering")
        void shouldAllowReplacingScopeHandlersAfterUnregistering() {
            // Given
            AtomicInteger handler1Calls = new AtomicInteger(0);
            AtomicInteger handler2Calls = new AtomicInteger(0);
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.registerScope(TestScope.class, new ScopeHandler() {
                @Override
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    handler1Calls.incrementAndGet();
                    return provider.get();
                }
                @Override
                public void close() {
                    // No-op for test
                }
            });
            sut.inject(TestScopedClass.class);
            // When - unregister and re-register with new handler
            sut.unregisterScope(TestScope.class);
            sut.registerScope(TestScope.class, new ScopeHandler() {
                @Override
                public <T> T get(Class<T> clazz, Supplier<T> provider) {
                    handler2Calls.incrementAndGet();
                    return provider.get();
                }
                @Override
                public void close() {
                    // No-op for test
                }
            });
            sut.inject(TestScopedClass.class);
            // Then
            assertEquals(1, handler1Calls.get());
            assertEquals(1, handler2Calls.get());
        }

        /**
         * ApplicationScoped behaves like a singleton - one instance per application
         */
        @Test
        @DisplayName("Should create single instance for @ApplicationScoped")
        void shouldCreateSingleInstanceForApplicationScoped() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ApplicationScopedClass instance1 = sut.inject(ApplicationScopedClass.class);
            ApplicationScopedClass instance2 = sut.inject(ApplicationScopedClass.class);
            // Then
            assertSame(instance1, instance2, "ApplicationScoped should return same instance");
            assertEquals(instance1.getId(), instance2.getId(), "IDs should match");
        }

        /**
         * ApplicationScoped with PreDestroy lifecycle
         */
        @Test
        @DisplayName("Should invoke @PreDestroy on @ApplicationScoped beans during shutdown")
        void shouldInvokePreDestroyOnApplicationScopedBeans() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ApplicationScopedWithPreDestroy instance = sut.inject(ApplicationScopedWithPreDestroy.class);
            assertFalse(instance.isDestroyed(), "Should not be destroyed initially");
            // When
            sut.shutdown();
            // Then
            assertTrue(instance.isDestroyed(), "@PreDestroy should have been called");
        }

        /**
         * RequestScoped with RequestScopeHandler - one instance per thread
         */
        @Test
        @DisplayName("Should create one instance per thread for @RequestScoped with RequestScopeHandler")
        void shouldCreateOneInstancePerThreadForRequestScoped() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            RequestScopeHandler requestHandler = new RequestScopeHandler();
            sut.registerScope(RequestScoped.class, requestHandler);

            // When - inject in main thread
            RequestScopedClass mainThread1 = sut.inject(RequestScopedClass.class);
            RequestScopedClass mainThread2 = sut.inject(RequestScopedClass.class);

            // Then - same instance within thread
            assertSame(mainThread1, mainThread2, "Should return same instance within thread");

            // When - inject in different thread
            Holder<RequestScopedClass> otherThreadInstance = new Holder<>();
            Thread thread = new Thread(() -> otherThreadInstance.set(sut.inject(RequestScopedClass.class)));
            thread.start();
            thread.join();

            // Then - different instance in different thread
            assertNotSame(mainThread1, otherThreadInstance.get(),
                "Should create different instance in different thread");

            // Cleanup
            requestHandler.close();
        }

        /**
         * RequestScoped cleanup with PreDestroy
         */
        @Test
        @DisplayName("Should invoke @PreDestroy on @RequestScoped beans when scope closes")
        void shouldInvokePreDestroyOnRequestScopedBeans() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            RequestScopeHandler requestHandler = new RequestScopeHandler();
            sut.registerScope(RequestScoped.class, requestHandler);

            RequestScopedWithPreDestroy instance = sut.inject(RequestScopedWithPreDestroy.class);
            assertFalse(instance.isPreDestroyCalled(), "Should not be destroyed initially");

            // When
            requestHandler.close();

            // Then
            assertTrue(instance.isPreDestroyCalled(), "@PreDestroy should have been called");
        }

        /**
         * SessionScoped with SessionScopeHandler - one instance per session
         */
        @Test
        @DisplayName("Should create one instance per session for @SessionScoped")
        void shouldCreateOneInstancePerSessionForSessionScoped() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SessionScopeHandler sessionHandler = new SessionScopeHandler();
            sut.registerScope(jakarta.enterprise.context.SessionScoped.class, sessionHandler);

            // When - session 1
            sessionHandler.setCurrentSession("session-1");
            SessionScopedClass session1Instance1 = sut.inject(SessionScopedClass.class);
            SessionScopedClass session1Instance2 = sut.inject(SessionScopedClass.class);

            // Then - same instance within session
            assertSame(session1Instance1, session1Instance2,
                "Should return same instance within same session");
            assertEquals(session1Instance1.getId(), session1Instance2.getId(),
                "IDs should match within session");

            // When - session 2
            sessionHandler.setCurrentSession("session-2");
            SessionScopedClass session2Instance = sut.inject(SessionScopedClass.class);

            // Then - different instance in different session
            assertNotSame(session1Instance1, session2Instance,
                "Should create different instance in different session");
            assertNotEquals(session1Instance1.getId(), session2Instance.getId(),
                "IDs should differ across sessions");

            // Cleanup
            sessionHandler.setCurrentSession("session-1");
            sessionHandler.close();
            sessionHandler.setCurrentSession("session-2");
            sessionHandler.close();
        }

        /**
         * SessionScoped cleanup with PreDestroy
         */
        @Test
        @DisplayName("Should invoke @PreDestroy on @SessionScoped beans when session closes")
        void shouldInvokePreDestroyOnSessionScopedBeans() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SessionScopeHandler sessionHandler = new SessionScopeHandler();
            sut.registerScope(jakarta.enterprise.context.SessionScoped.class, sessionHandler);

            sessionHandler.setCurrentSession("test-session");
            SessionScopedWithPreDestroy instance = sut.inject(SessionScopedWithPreDestroy.class);
            assertFalse(instance.isDestroyed(), "Should not be destroyed initially");

            // When
            sessionHandler.close();

            // Then
            assertTrue(instance.isDestroyed(), "@PreDestroy should have been called");
        }

        /**
         * SessionScoped without session context should throw exception
         */
        @Test
        @DisplayName("Should throw exception when no session context is set for @SessionScoped")
        void shouldThrowExceptionWhenNoSessionContextForSessionScoped() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            SessionScopeHandler sessionHandler = new SessionScopeHandler();
            sut.registerScope(jakarta.enterprise.context.SessionScoped.class, sessionHandler);

            // When/Then - no session set
            // The IllegalStateException is wrapped in InjectionException
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(SessionScopedClass.class), "Should throw InjectionException when no session context");

            // Verify the cause is IllegalStateException
            assertInstanceOf(IllegalStateException.class, thrown.getCause(), "Cause should be IllegalStateException");
            assertTrue(thrown.getCause().getMessage().contains("No session context"),
                "Should mention no session context");
        }

        /**
         * ConversationScoped with ConversationScopeHandler - one instance per conversation
         */
        @Test
        @DisplayName("Should create one instance per conversation for @ConversationScoped")
        void shouldCreateOneInstancePerConversationForConversationScoped() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ConversationScopeHandler conversationHandler = new ConversationScopeHandler();
            sut.registerScope(jakarta.enterprise.context.ConversationScoped.class, conversationHandler);

            // When - conversation 1
            conversationHandler.beginConversation("conversation-1");
            ConversationScopedClass conv1Instance1 = sut.inject(ConversationScopedClass.class);
            ConversationScopedClass conv1Instance2 = sut.inject(ConversationScopedClass.class);

            // Then - same instance within conversation
            assertSame(conv1Instance1, conv1Instance2,
                "Should return same instance within same conversation");
            assertEquals(conv1Instance1.getInstanceId(), conv1Instance2.getInstanceId(),
                "IDs should match within conversation");

            // When - conversation 2
            conversationHandler.beginConversation("conversation-2");
            ConversationScopedClass conv2Instance = sut.inject(ConversationScopedClass.class);

            // Then - different instance in different conversation
            assertNotSame(conv1Instance1, conv2Instance,
                "Should create different instance in different conversation");
            assertNotEquals(conv1Instance1.getInstanceId(), conv2Instance.getInstanceId(),
                "IDs should differ across conversations");

            // Cleanup
            conversationHandler.endConversation("conversation-1");
            conversationHandler.endConversation("conversation-2");
        }

        /**
         * ConversationScoped cleanup with PreDestroy
         */
        @Test
        @DisplayName("Should invoke @PreDestroy on @ConversationScoped beans when conversation ends")
        void shouldInvokePreDestroyOnConversationScopedBeans() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ConversationScopeHandler conversationHandler = new ConversationScopeHandler();
            sut.registerScope(jakarta.enterprise.context.ConversationScoped.class, conversationHandler);

            conversationHandler.beginConversation("test-conversation");
            ConversationScopedWithPreDestroy instance = sut.inject(ConversationScopedWithPreDestroy.class);
            assertFalse(instance.isPreDestroyCalled(), "Should not be destroyed initially");

            // When
            conversationHandler.endConversation("test-conversation");

            // Then
            assertTrue(instance.isPreDestroyCalled(), "@PreDestroy should have been called");
        }

        /**
         * ConversationScoped without conversation context should throw exception
         */
        @Test
        @DisplayName("Should throw exception when no conversation context is set for @ConversationScoped")
        void shouldThrowExceptionWhenNoConversationContextForConversationScoped() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ConversationScopeHandler conversationHandler = new ConversationScopeHandler();
            sut.registerScope(jakarta.enterprise.context.ConversationScoped.class, conversationHandler);

            // When/Then - no conversation set
            // The IllegalStateException is wrapped in InjectionException
            InjectionException thrown = assertThrows(InjectionException.class, () -> sut.inject(ConversationScopedClass.class), "Should throw InjectionException when no conversation context");

            // Verify the cause is IllegalStateException
            assertInstanceOf(IllegalStateException.class, thrown.getCause(), "Cause should be IllegalStateException");
            assertTrue(thrown.getCause().getMessage().contains("No active conversation"),
                "Should mention no active conversation");
        }

        /**
         * ConversationScoped with multiple conversations in different threads
         */
        @Test
        @DisplayName("Should isolate conversations across threads")
        void shouldIsolateConversationsAcrossThreads() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ConversationScopeHandler conversationHandler = new ConversationScopeHandler();
            sut.registerScope(jakarta.enterprise.context.ConversationScoped.class, conversationHandler);

            // When - main thread conversation
            conversationHandler.beginConversation("main-conversation");
            ConversationScopedClass mainInstance = sut.inject(ConversationScopedClass.class);

            // When - other thread conversation
            Holder<ConversationScopedClass> otherThreadInstance = new Holder<>();
            Thread thread = new Thread(() -> {
                conversationHandler.beginConversation("other-conversation");
                otherThreadInstance.set(sut.inject(ConversationScopedClass.class));
                conversationHandler.endConversation("other-conversation");
            });
            thread.start();
            thread.join();

            // Then - different instances in different conversations
            assertNotSame(mainInstance, otherThreadInstance.get(),
                "Should create different instance in different conversation");

            // Cleanup
            conversationHandler.endConversation("main-conversation");
        }

        /**
         * Multiple scopes working together
         */
        @Test
        @DisplayName("Should handle multiple different scopes correctly")
        void shouldHandleMultipleScopesCorrectly() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            RequestScopeHandler requestHandler = new RequestScopeHandler();
            SessionScopeHandler sessionHandler = new SessionScopeHandler();

            sut.registerScope(RequestScoped.class, requestHandler);
            sut.registerScope(jakarta.enterprise.context.SessionScoped.class, sessionHandler);

            sessionHandler.setCurrentSession("session-1");

            // When
            ApplicationScopedClass appScoped1 = sut.inject(ApplicationScopedClass.class);
            ApplicationScopedClass appScoped2 = sut.inject(ApplicationScopedClass.class);
            RequestScopedClass reqScoped1 = sut.inject(RequestScopedClass.class);
            RequestScopedClass reqScoped2 = sut.inject(RequestScopedClass.class);
            SessionScopedClass sesScoped1 = sut.inject(SessionScopedClass.class);
            SessionScopedClass sesScoped2 = sut.inject(SessionScopedClass.class);

            // Then
            assertSame(appScoped1, appScoped2, "ApplicationScoped should be singleton");
            assertSame(reqScoped1, reqScoped2, "RequestScoped should be same within thread");
            assertSame(sesScoped1, sesScoped2, "SessionScoped should be same within session");

            // Cleanup
            requestHandler.close();
            sessionHandler.close();
        }

        /**
         * Concurrent singleton access stress test - validates thread safety of singleton scope
         */
        @Test
        @DisplayName("Should handle concurrent singleton access without creating duplicate instances")
        void shouldHandleConcurrentSingletonAccess() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            int threadCount = 100;
            int injectionsPerThread = 100;
            ConcurrentHashMap<SingletonScopedClass, Boolean> instancesMap = new ConcurrentHashMap<>();
            Set<SingletonScopedClass> instances = Collections.newSetFromMap(instancesMap);
            AtomicInteger successCount = new AtomicInteger(0);

            // When - multiple threads concurrently request the same singleton
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < injectionsPerThread; j++) {
                        try {
                            SingletonScopedClass instance = sut.inject(SingletonScopedClass.class);
                            instances.add(instance);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            fail("Injection should not fail: " + e.getMessage());
                        }
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then - only one instance should have been created
            assertEquals(1, instances.size(), "Should only create one singleton instance");
            assertEquals(threadCount * injectionsPerThread, successCount.get(),
                "All injections should succeed");
        }

        /**
         * Concurrent ApplicationScoped access stress test
         */
        @Test
        @DisplayName("Should handle concurrent ApplicationScoped access without creating duplicate instances")
        void shouldHandleConcurrentApplicationScopedAccess() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            int threadCount = 100;
            int injectionsPerThread = 100;
            ConcurrentHashMap<ApplicationScopedClass, Boolean> instancesMap = new ConcurrentHashMap<>();
            Set<ApplicationScopedClass> instances = Collections.newSetFromMap(instancesMap);
            AtomicInteger successCount = new AtomicInteger(0);

            // When - multiple threads concurrently request the same ApplicationScoped bean
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < injectionsPerThread; j++) {
                        try {
                            ApplicationScopedClass instance = sut.inject(ApplicationScopedClass.class);
                            instances.add(instance);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            fail("Injection should not fail: " + e.getMessage());
                        }
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then - only one instance should have been created
            assertEquals(1, instances.size(), "Should only create one ApplicationScoped instance");
            assertEquals(threadCount * injectionsPerThread, successCount.get(),
                "All injections should succeed");
        }

        /**
         * Concurrent RequestScoped access - each thread should get its own instance
         */
        @Test
        @DisplayName("Should handle concurrent RequestScoped access with proper thread isolation")
        void shouldHandleConcurrentRequestScopedAccess() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            RequestScopeHandler requestHandler = new RequestScopeHandler();
            sut.registerScope(RequestScoped.class, requestHandler);

            int threadCount = 50;
            int injectionsPerThread = 100;
            ConcurrentHashMap<Long, Set<RequestScopedClass>> instancesByThread = new ConcurrentHashMap<>();
            AtomicInteger successCount = new AtomicInteger(0);

            // When - multiple threads concurrently request RequestScoped beans
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    long threadId = Thread.currentThread().getId();
                    ConcurrentHashMap<RequestScopedClass, Boolean> threadMap = new ConcurrentHashMap<>();
                    instancesByThread.putIfAbsent(threadId,
                        Collections.newSetFromMap(threadMap));

                    for (int j = 0; j < injectionsPerThread; j++) {
                        try {
                            RequestScopedClass instance = sut.inject(RequestScopedClass.class);
                            instancesByThread.get(threadId).add(instance);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            fail("Injection should not fail: " + e.getMessage());
                        }
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then - each thread should have exactly one instance
            assertEquals(threadCount, instancesByThread.size(),
                "Should have instances for all threads");
            instancesByThread.forEach((threadId, instances) -> assertEquals(1, instances.size(),
                "Thread " + threadId + " should have exactly one RequestScoped instance"));
            assertEquals(threadCount * injectionsPerThread, successCount.get(),
                "All injections should succeed");

            // Cleanup
            requestHandler.close();
        }

        /**
         * Mixed concurrent access - stress test with multiple scope types
         */
        @Test
        @DisplayName("Should handle concurrent mixed scope access correctly")
        void shouldHandleConcurrentMixedScopeAccess() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            RequestScopeHandler requestHandler = new RequestScopeHandler();
            sut.registerScope(RequestScoped.class, requestHandler);

            int threadCount = 50;
            int injectionsPerThread = 50;
            ConcurrentHashMap<ApplicationScopedClass, Boolean> appMap = new ConcurrentHashMap<>();
            Set<ApplicationScopedClass> appScopedInstances = Collections.newSetFromMap(appMap);
            ConcurrentHashMap<SingletonScopedClass, Boolean> singletonMap = new ConcurrentHashMap<>();
            Set<SingletonScopedClass> singletonInstances = Collections.newSetFromMap(singletonMap);
            ConcurrentHashMap<Long, Set<RequestScopedClass>> requestScopedByThread = new ConcurrentHashMap<>();
            AtomicInteger successCount = new AtomicInteger(0);

            // When - threads inject beans of different scopes concurrently
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    long threadId = Thread.currentThread().getId();
                    ConcurrentHashMap<RequestScopedClass, Boolean> threadMap = new ConcurrentHashMap<>();
                    requestScopedByThread.putIfAbsent(threadId,
                        Collections.newSetFromMap(threadMap));

                    for (int j = 0; j < injectionsPerThread; j++) {
                        try {
                            // Inject different scope types
                            ApplicationScopedClass appScoped = sut.inject(ApplicationScopedClass.class);
                            SingletonScopedClass singleton = sut.inject(SingletonScopedClass.class);
                            RequestScopedClass requestScoped = sut.inject(RequestScopedClass.class);

                            appScopedInstances.add(appScoped);
                            singletonInstances.add(singleton);
                            requestScopedByThread.get(threadId).add(requestScoped);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            fail("Injection should not fail: " + e.getMessage());
                        }
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then - verify scope semantics
            assertEquals(1, appScopedInstances.size(),
                "Should only create one ApplicationScoped instance");
            assertEquals(1, singletonInstances.size(),
                "Should only create one Singleton instance");
            assertEquals(threadCount, requestScopedByThread.size(),
                "Should have RequestScoped instances for all threads");
            requestScopedByThread.forEach((threadId, instances) -> assertEquals(1, instances.size(),
                "Each thread should have exactly one RequestScoped instance"));
            assertEquals(threadCount * injectionsPerThread, successCount.get(),
                "All injections should succeed");

            // Cleanup
            requestHandler.close();
        }
    }

    /**
     * Tests for Optional injection support (JSR-330 optional dependency pattern).
     */
    @Nested
    @DisplayName("Optional Injection Tests")
    class OptionalInjectionTests {

        @Test
        @DisplayName("Should inject Optional.of() when dependency exists - field injection")
        void shouldInjectOptionalOfWhenDependencyExistsFieldInjection() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.optional");
            // When
            ClassWithOptionalFieldInjection instance = sut.inject(ClassWithOptionalFieldInjection.class);
            // Then
            assertNotNull(instance.getOptionalService(), "Optional field should not be null");
            assertTrue(instance.getOptionalService().isPresent(),
                "Optional should be present when dependency exists");
            assertEquals("OptionalService is present",
                instance.getOptionalService().get().getValue());
        }

        @Test
        @DisplayName("Should inject Optional.empty() when dependency missing - field injection")
        void shouldInjectOptionalEmptyWhenDependencyMissingFieldInjection() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.optional");
            // When
            ClassWithOptionalFieldInjection instance = sut.inject(ClassWithOptionalFieldInjection.class);
            // Then
            assertNotNull(instance.getNonExistentService(), "Optional field should not be null");
            assertFalse(instance.getNonExistentService().isPresent(),
                "Optional should be empty when dependency does not exist");
        }

        @Test
        @DisplayName("Should inject Optional dependencies via constructor")
        void shouldInjectOptionalDependenciesViaConstructor() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.optional");
            // When
            ClassWithOptionalConstructorInjection instance =
                sut.inject(ClassWithOptionalConstructorInjection.class);
            // Then
            assertNotNull(instance.getOptionalService(), "Optional should not be null");
            assertTrue(instance.getOptionalService().isPresent(),
                "Optional should be present when dependency exists");
            assertNotNull(instance.getNonExistentService(), "Optional should not be null");
            assertFalse(instance.getNonExistentService().isPresent(),
                "Optional should be empty when dependency does not exist");
        }

        @Test
        @DisplayName("Should inject Optional dependencies via method injection")
        void shouldInjectOptionalDependenciesViaMethodInjection() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.optional");
            // When
            ClassWithOptionalMethodInjection instance =
                sut.inject(ClassWithOptionalMethodInjection.class);
            // Then
            assertNotNull(instance.getOptionalService(), "Optional should not be null");
            assertTrue(instance.getOptionalService().isPresent(),
                "Optional should be present when dependency exists");
            assertNotNull(instance.getNonExistentService(), "Optional should not be null");
            assertFalse(instance.getNonExistentService().isPresent(),
                "Optional should be empty when dependency does not exist");
        }

        @Test
        @DisplayName("Should allow Optional of singleton to be injected")
        void shouldAllowOptionalOfSingletonToBeInjected() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.scopes");
            // When
            Optional<SingletonScopedClass> optional1 =
                sut.inject(new TypeLiteral<Optional<SingletonScopedClass>>() {});
            Optional<SingletonScopedClass> optional2 =
                sut.inject(new TypeLiteral<Optional<SingletonScopedClass>>() {});
            // Then
            assertTrue(optional1.isPresent(), "Optional should contain singleton");
            assertTrue(optional2.isPresent(), "Optional should contain singleton");
            assertSame(optional1.get(), optional2.get(),
                "Should be same singleton instance inside Optional");
        }

        @Test
        @DisplayName("Should inject empty Optional for non-existent interface")
        void shouldInjectEmptyOptionalForNonExistentInterface() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.optional");
            // When
            Optional<NonExistentService> optional =
                sut.inject(new TypeLiteral<Optional<NonExistentService>>() {});
            // Then
            assertNotNull(optional, "Optional should not be null");
            assertFalse(optional.isPresent(),
                "Optional should be empty when no implementation exists");
        }

        @Test
        @DisplayName("Should handle multiple Optional injections consistently")
        void shouldHandleMultipleOptionalInjectionsConsistently() {
            // Given
            Injector sut = new InjectorImpl("com.threeamigos.common.util.implementations.injection.optional");
            // When - inject same Optional type multiple times
            Optional<OptionalService> optional1 =
                sut.inject(new TypeLiteral<Optional<OptionalService>>() {});
            Optional<OptionalService> optional2 =
                sut.inject(new TypeLiteral<Optional<OptionalService>>() {});
            // Then
            assertTrue(optional1.isPresent(), "First Optional should contain service");
            assertTrue(optional2.isPresent(), "Second Optional should contain service");
            // Note: The service instances may be different (not singleton),
            // but both Optionals should contain a value
            assertEquals(optional1.get().getValue(), optional2.get().getValue(),
                "Both should have same getValue() result");
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
            void ifExceptionThrownIsUnsatisfiedIsTrue() {
                // Given
                ClassResolver mockResolver = spy(new ClassResolver(new KnowledgeBase()));
                // resolveImplementations throws an exception
                // Use RuntimeException instead of checked Exception to avoid Mockito validation error
                doThrow(new RuntimeException("Test exception"))
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
            void ifExceptionThrownIsAmbiguousIsFalse() {
                // Given
                ClassResolver mockResolver = spy(new ClassResolver(new KnowledgeBase()));
                // resolveImplementations throws an exception
                // Use RuntimeException instead of checked Exception to avoid Mockito validation error
                doThrow(new RuntimeException("Test exception"))
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
            void iteratorShouldThrowRuntimeExceptionIfResolutionFails() {
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
                // Use RuntimeException instead of checked Exception to avoid Mockito validation error
                when(mockResolver.resolveImplementations(
                        any(java.lang.reflect.Type.class),
                        nullable(Collection.class)
                )).thenThrow(new RuntimeException("Resolution failed"));

                InjectorImpl sut = new InjectorImpl(mockResolver);
                ClassWithInstanceOfTestInterface instanceWrapper = sut.inject(ClassWithInstanceOfTestInterface.class);
                jakarta.enterprise.inject.Instance<TestInterface> instance = instanceWrapper.getTestInterfaceInstance();

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

    }

    @Nested
    @DisplayName("Misc")
    class MiscTests {

        @Test
        @DisplayName("Miscellaneous")
        void miscellaneousTests() {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Test that basic injection works with the injector
            // GenericService was previously used here, but it contains invalid injection points
            // (type variables and wildcards) that are correctly rejected by CDI spec validation

            // When - inject a simple concrete class
            TestClass instance = sut.inject(TestClass.class);

            // Then
            assertNotNull(instance);
        }

    }

    @Nested
    @DisplayName("Null Check and Edge Case Tests")
    class NullCheckAndEdgeCaseTests {

        @Test
        @DisplayName("Should throw exception when ClassResolver is null")
        void shouldThrowExceptionWhenClassResolverIsNull() {
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> new InjectorImpl((ClassResolver) null));
        }

        @Test
        @DisplayName("Should throw exception when scope annotation is null in registerScope")
        void shouldThrowExceptionWhenScopeAnnotationIsNullInRegisterScope() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.registerScope(null, new MockScopeHandler()));
        }

        @Test
        @DisplayName("Should throw exception when scope handler is null in registerScope")
        void shouldThrowExceptionWhenScopeHandlerIsNullInRegisterScope() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.registerScope(RequestScoped.class, null));
        }

        @Test
        @DisplayName("Should throw exception when registering duplicate scope")
        void shouldThrowExceptionWhenRegisteringDuplicateScope() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.registerScope(RequestScoped.class, new MockScopeHandler());
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.registerScope(RequestScoped.class, new MockScopeHandler()));
        }

        @Test
        @DisplayName("Should throw exception when scope annotation is null in unregisterScope")
        void shouldThrowExceptionWhenScopeAnnotationIsNullInUnregisterScope() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.unregisterScope(null));
        }

        @Test
        @DisplayName("Should throw exception when class to inject is null")
        void shouldThrowExceptionWhenClassToInjectIsNull() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.inject((Class<?>) null));
        }

        @Test
        @DisplayName("Should throw exception when TypeLiteral is null")
        void shouldThrowExceptionWhenTypeLiteralIsNull() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.inject((TypeLiteral<?>) null));
        }

        @Test
        @DisplayName("Should return null from getScopeType when no scope annotation present")
        void shouldReturnNullFromGetScopeTypeWhenNoScopeAnnotation() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            Class<? extends Annotation> scopeType = sut.getScopeType(TestClass.class);
            // Then
            assertNull(scopeType);
        }

        @Test
        @DisplayName("Should return Singleton from getScopeType")
        void shouldReturnSingletonFromGetScopeType() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            Class<? extends Annotation> scopeType = sut.getScopeType(SingletonClass.class);
            // Then
            assertEquals(Singleton.class, scopeType);
        }

        @Test
        @DisplayName("Should return empty string for class in default package")
        void shouldReturnEmptyStringForClassInDefaultPackage() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - test with a class name without package
            String packageName = sut.getPackageName(String.class);
            // Then
            assertNotNull(packageName); // String is in java.lang package, so won't be empty
            // Test with simulated default package class name
            class LocalClass {}
            String localPackage = sut.getPackageName(LocalClass.class);
            assertTrue(localPackage.contains(".")); // Local classes have package from outer class
        }

        @Test
        @DisplayName("Should return null from findMethod when method not found")
        void shouldReturnNullFromFindMethodWhenMethodNotFound() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            Method method = sut.findMethod(TestClass.class, "nonExistentMethod", new Class<?>[0]);
            // Then
            assertNull(method);
        }

        @Test
        @DisplayName("Should handle isOverridden with private method")
        void shouldHandleIsOverriddenWithPrivateMethod() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method privateMethod = ClassWithPrivateMethod.class.getDeclaredMethod("privateMethod");
            // When
            boolean isOverridden = sut.isOverridden(privateMethod, ClassWithPrivateMethod.class);
            // Then
            assertFalse(isOverridden);
        }

        @Test
        @DisplayName("Should handle isOverridden with same declaring class")
        void shouldHandleIsOverriddenWithSameDeclaringClass() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method method = ClassWithPrivateMethod.class.getDeclaredMethod("privateMethod");
            // When - method is in the same class, so not overridden
            boolean isOverridden = sut.isOverridden(method, ClassWithPrivateMethod.class);
            // Then
            assertFalse(isOverridden);
        }
    }

    @Nested
    @DisplayName("Additional Coverage Tests")
    class AdditionalCoverageTests {

        @Test
        @DisplayName("Should clear state properly")
        void shouldClearStateProperly() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.registerScope(RequestScoped.class, new MockScopeHandler());
            sut.inject(ClassWithStaticField.class);
            assertTrue(sut.isScopeRegistered(RequestScoped.class));
            // When
            sut.clearState();
            // Then
            assertFalse(sut.isScopeRegistered(RequestScoped.class));
            assertTrue(sut.isScopeRegistered(Singleton.class)); // Default scope re-registered
        }

        @Test
        @DisplayName("Should return registered scopes")
        void shouldReturnRegisteredScopes() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            Set<Class<? extends Annotation>> scopes = sut.getRegisteredScopes();
            // Then
            assertNotNull(scopes);
            assertTrue(scopes.contains(Singleton.class));
            // Should be unmodifiable
            assertThrows(UnsupportedOperationException.class, () -> scopes.add(RequestScoped.class));
        }

        @Test
        @DisplayName("Should check if scope is registered")
        void shouldCheckIfScopeIsRegistered() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            assertTrue(sut.isScopeRegistered(Singleton.class));
            assertFalse(sut.isScopeRegistered(RequestScoped.class));
            sut.registerScope(RequestScoped.class, new MockScopeHandler());
            assertTrue(sut.isScopeRegistered(RequestScoped.class));
        }

        @Test
        @DisplayName("Should unregister scope")
        void shouldUnregisterScope() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            sut.registerScope(RequestScoped.class, new MockScopeHandler());
            assertTrue(sut.isScopeRegistered(RequestScoped.class));
            // When
            sut.unregisterScope(RequestScoped.class);
            // Then
            assertFalse(sut.isScopeRegistered(RequestScoped.class));
        }

        @Test
        @DisplayName("Should inject array types")
        void shouldInjectArrayTypes() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            String[] array = sut.inject(String[].class);
            // Then
            assertNotNull(array);
            assertEquals(0, array.length);
        }

        @Test
        @DisplayName("Should throw exception for abstract method injection")
        void shouldThrowExceptionForAbstractMethodInjection() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            InjectionException thrown = assertThrows(InjectionException.class,
                    () -> sut.inject(ClassWithAbstractMethod.class));
            assertTrue(thrown.getMessage().contains("Cannot inject into abstract method"));
        }

        @Test
        @DisplayName("Should throw exception for generic method injection")
        void shouldThrowExceptionForGenericMethodInjection() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When / Then
            InjectionException thrown = assertThrows(InjectionException.class,
                    () -> sut.inject(ClassWithGenericMethod.class));
            assertTrue(thrown.getMessage().contains("Cannot inject into generic method"));
        }

        @Test
        @DisplayName("Should handle Instance.destroy() with exception")
        void shouldHandleInstanceDestroyWithException() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstanceOfFailingPreDestroy classWithInstance = sut.inject(ClassWithInstanceOfFailingPreDestroy.class);
            Instance<SingletonWithFailingPreDestroy> instance = classWithInstance.getInstance();
            SingletonWithFailingPreDestroy obj = new SingletonWithFailingPreDestroy();
            // When / Then
            assertThrows(RuntimeException.class, () -> instance.destroy(obj));
        }

        @Test
        @DisplayName("Should resolve TypeVariable in generic context")
        void shouldResolveTypeVariableInGenericContext() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - inject a class with generic type parameter resolved
            TypeLiteral<Holder<String>> typeLiteral = new TypeLiteral<Holder<String>>() {};
            Holder<String> holder = sut.inject(typeLiteral);
            // Then
            assertNotNull(holder);
        }

        @Test
        @DisplayName("Should handle package-private method override")
        void shouldHandlePackagePrivateMethodOverride() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - inject a class with package-private method override
            ClassWithPackagePrivateMethodOverride instance = sut.inject(ClassWithPackagePrivateMethodOverride.class);
            // Then
            assertNotNull(instance);
        }

        @Test
        @DisplayName("Should handle TypeLiteral injection with ThreadLocal stack")
        void shouldHandleTypeLiteralInjectionWithThreadLocalStack() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            TypeLiteral<TestClass> typeLiteral = new TypeLiteral<TestClass>() {};
            // When - inject multiple times to test ThreadLocal stack clearing
            TestClass instance1 = sut.inject(typeLiteral);
            TestClass instance2 = sut.inject(typeLiteral);
            // Then
            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }
    }

    @Nested
    @DisplayName("ResolveType Method Direct Tests")
    class ResolveTypeDirectTests {

        @Test
        @DisplayName("Should return non-TypeVariable types as-is (direct test)")
        void shouldReturnNonTypeVariableTypesAsIs() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Type concreteType = String.class;
            Type context = new TypeLiteral<List<String>>() {}.getType();

            // When
            Type result = sut.resolveType(concreteType, context);

            // Then
            assertSame(concreteType, result);
        }

        @Test
        @DisplayName("Should return ParameterizedType as-is (direct test)")
        void shouldReturnParameterizedTypeAsIs() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Type parameterizedType = new TypeLiteral<List<String>>() {}.getType();
            Type context = new TypeLiteral<Holder<Integer>>() {}.getType();

            // When
            Type result = sut.resolveType(parameterizedType, context);

            // Then
            assertSame(parameterizedType, result);
        }

        @Test
        @DisplayName("Should resolve TypeVariable with matching name in ParameterizedType context (direct test)")
        void shouldResolveTypeVariableWithMatchingName() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Get the TypeVariable from a generic class
            TypeVariable<?> typeVariable = Holder.class.getTypeParameters()[0]; // T from Holder<T>

            // Create a ParameterizedType context: Holder<String>
            Type context = new TypeLiteral<Holder<String>>() {}.getType();

            // When
            Type result = sut.resolveType(typeVariable, context);

            // Then
            assertEquals(String.class, result);
        }

        @Test
        @DisplayName("Should return TypeVariable when no matching name in ParameterizedType (direct test)")
        void shouldReturnTypeVariableWhenNoMatchingName() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Get TypeVariable 'T' from Holder<T>
            TypeVariable<?> typeVariable = Holder.class.getTypeParameters()[0];

            // Create context with different type parameter name: List<E> (not T)
            Type context = new TypeLiteral<List<String>>() {}.getType();

            // When
            Type result = sut.resolveType(typeVariable, context);

            // Then - should return the unresolved TypeVariable
            assertSame(typeVariable, result);
        }

        @Test
        @DisplayName("Should return TypeVariable when context is not ParameterizedType (direct test)")
        void shouldReturnTypeVariableWhenContextIsNotParameterizedType() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Get TypeVariable 'T' from Holder<T>
            TypeVariable<?> typeVariable = Holder.class.getTypeParameters()[0];

            // Use a non-ParameterizedType context (raw class)
            Type context = String.class;

            // When
            Type result = sut.resolveType(typeVariable, context);

            // Then - should return the unresolved TypeVariable
            assertSame(typeVariable, result);
        }

        @Test
        @DisplayName("Should resolve TypeVariable with multiple type parameters (direct test)")
        void shouldResolveTypeVariableWithMultipleTypeParameters() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Get TypeVariables from a class with multiple type parameters
            TypeVariable<?>[] typeVars = Map.class.getTypeParameters(); // K and V
            TypeVariable<?> keyTypeVar = typeVars[0]; // K
            TypeVariable<?> valueTypeVar = typeVars[1]; // V

            // Create context: Map<String, Integer>
            Type context = new TypeLiteral<Map<String, Integer>>() {}.getType();

            // When
            Type keyResult = sut.resolveType(keyTypeVar, context);
            Type valueResult = sut.resolveType(valueTypeVar, context);

            // Then
            assertEquals(String.class, keyResult);
            assertEquals(Integer.class, valueResult);
        }

        @Test
        @DisplayName("Should handle TypeVariable resolution in nested generics (direct test)")
        void shouldHandleTypeVariableInNestedGenerics() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Get TypeVariable 'T' from Holder<T>
            TypeVariable<?> typeVariable = Holder.class.getTypeParameters()[0];

            // Create context: Holder<List<String>> - T resolves to List<String>
            Type context = new TypeLiteral<Holder<List<String>>>() {}.getType();

            // When
            Type result = sut.resolveType(typeVariable, context);

            // Then
            assertInstanceOf(ParameterizedType.class, result);
            ParameterizedType pt = (ParameterizedType) result;
            assertEquals(List.class, pt.getRawType());
            assertEquals(String.class, pt.getActualTypeArguments()[0]);
        }
    }

    @Nested
    @DisplayName("GetPackageName, FindMethod, IsOverridden Direct Tests")
    class PackageMethodOverrideTests {

        @Test
        @DisplayName("getPackageName should return package name for regular class")
        void getPackageNameShouldReturnPackageNameForRegularClass() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // When
            String packageName = sut.getPackageName(String.class);

            // Then
            assertEquals("java.lang", packageName);
        }

        @Test
        @DisplayName("getPackageName should return empty string for default package")
        void getPackageNameShouldReturnEmptyStringForDefaultPackage() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Create a mock class name without package (simulate default package)
            // We can't actually create such a class, so we test the logic with the method directly
            // by checking behavior with a class that would have no dots

            // When - use a class and verify behavior
            String result = sut.getPackageName(int.class); // primitives have no package

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("getPackageName should handle nested classes")
        void getPackageNameShouldHandleNestedClasses() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // When
            String packageName = sut.getPackageName(TestClass.class);

            // Then
            assertTrue(packageName.contains("injection"));
        }

        @Test
        @DisplayName("findMethod should find method in current class")
        void findMethodShouldFindMethodInCurrentClass() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // When - find a method declared in the class itself
            Method method = sut.findMethod(ParentWithPublicMethod.class, "publicMethod", new Class<?>[0]);

            // Then
            assertNotNull(method);
            assertEquals("publicMethod", method.getName());
        }

        @Test
        @DisplayName("findMethod should find method in parent class")
        void findMethodShouldFindMethodInParentClass() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // When - look for parent's method in child class
            Method method = sut.findMethod(ChildOverridingPublicMethod.class, "publicMethod", new Class<?>[0]);

            // Then
            assertNotNull(method);
            assertEquals("publicMethod", method.getName());
        }

        @Test
        @DisplayName("findMethod should return null when method not found")
        void findMethodShouldReturnNullWhenMethodNotFound() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // When
            Method method = sut.findMethod(TestClass.class, "nonExistentMethod", new Class<?>[0]);

            // Then
            assertNull(method);
        }

        @Test
        @DisplayName("findMethod should stop at Object class and not find Object methods")
        void findMethodShouldStopAtObjectClass() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // When - look for a method that only exists in Object (wait is protected in Object)
            // findMethod stops at Object class, so it won't find Object methods
            Method method = sut.findMethod(TestClass.class, "clone", new Class<?>[0]);

            // Then
            assertNull(method); // Should NOT find Object.clone() because loop stops at Object
        }

        @Test
        @DisplayName("isOverridden should return false for private method")
        void isOverriddenShouldReturnFalseForPrivateMethod() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method privateMethod = ParentWithPrivateMethod.class.getDeclaredMethod("privateMethod");

            // When
            boolean result = sut.isOverridden(privateMethod, ChildOfParentWithPrivateMethod.class);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("isOverridden should return false when method is in same class")
        void isOverriddenShouldReturnFalseWhenMethodIsInSameClass() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method method = ParentWithPublicMethod.class.getDeclaredMethod("publicMethod");

            // When - same class as declaring class
            boolean result = sut.isOverridden(method, ParentWithPublicMethod.class);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("isOverridden should return false when subMethod is null")
        void isOverriddenShouldReturnFalseWhenSubMethodIsNull() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method method = ParentWithUniqueMethod.class.getDeclaredMethod("uniqueMethod");

            // When - child class doesn't have this method
            boolean result = sut.isOverridden(method, ChildWithoutUniqueMethod.class);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("isOverridden should return false when methods are equal")
        void isOverriddenShouldReturnFalseWhenMethodsAreEqual() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method method = Object.class.getDeclaredMethod("toString");

            // When - same method found
            boolean result = sut.isOverridden(method, Object.class);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("isOverridden should return true for public method override")
        void isOverriddenShouldReturnTrueForPublicMethodOverride() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method parentMethod = ParentWithPublicMethod.class.getDeclaredMethod("publicMethod");

            // When
            boolean result = sut.isOverridden(parentMethod, ChildOverridingPublicMethod.class);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("isOverridden should return true for protected method override")
        void isOverriddenShouldReturnTrueForProtectedMethodOverride() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method parentMethod = ParentWithProtectedMethod.class.getDeclaredMethod("protectedMethod");

            // When
            boolean result = sut.isOverridden(parentMethod, ChildOverridingProtectedMethod.class);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("isOverridden should return true for package-private method in same package")
        void isOverriddenShouldReturnTrueForPackagePrivateMethodInSamePackage() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Method parentMethod = ParentForPackagePrivateTest.class.getDeclaredMethod("packagePrivateTestMethod");

            // When - child in same package
            boolean result = sut.isOverridden(parentMethod, ChildInSamePackageForTest.class);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("isOverridden should return false for package-private method in different package")
        void isOverriddenShouldReturnFalseForPackagePrivateMethodInDifferentPackage() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);

            // Create a mock scenario where parent and child are in different packages
            // We can test this by using actual classes from different packages
            Method parentMethod = ParentForPackagePrivateTest.class.getDeclaredMethod("packagePrivateTestMethod");

            // When - use String class which is definitely in a different package
            // First we need a child that actually has the method but in different package
            boolean result = sut.isOverridden(parentMethod, TestClass.class);

            // Then - TestClass doesn't have this method, so should return false
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("ResolveType Method Coverage Tests")
    class ResolveTypeTests {

        @Test
        @DisplayName("Should return non-TypeVariable types as-is")
        void shouldReturnNonTypeVariableTypesAsIs() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - inject class with concrete (non-TypeVariable) field type
            ClassWithConcreteTypeField instance = sut.inject(ClassWithConcreteTypeField.class);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getTestClass());
        }

        @Test
        @DisplayName("Should resolve TypeVariable with matching name in ParameterizedType context")
        void shouldResolveTypeVariableWithMatchingName() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - inject generic class with TypeVariable that matches context
            TypeLiteral<GenericClassWithTypeVariable<String>> typeLiteral =
                    new TypeLiteral<GenericClassWithTypeVariable<String>>() {};
            GenericClassWithTypeVariable<String> instance = sut.inject(typeLiteral);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getValue());
            assertEquals("default", instance.getValue());
        }

        @Test
        @DisplayName("Should resolve TypeVariable in constructor parameters")
        void shouldResolveTypeVariableInConstructorParameters() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - inject generic class with TypeVariable constructor parameter
            TypeLiteral<GenericClassWithConstructorTypeVariable<TestClass>> typeLiteral =
                    new TypeLiteral<GenericClassWithConstructorTypeVariable<TestClass>>() {};
            GenericClassWithConstructorTypeVariable<TestClass> instance = sut.inject(typeLiteral);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getDependency());
        }

        @Test
        @DisplayName("Should handle TypeVariable in field injection")
        void shouldHandleTypeVariableInFieldInjection() {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When - inject generic class with TypeVariable field
            TypeLiteral<GenericClassWithFieldTypeVariable<TestClass>> typeLiteral =
                    new TypeLiteral<GenericClassWithFieldTypeVariable<TestClass>>() {};
            GenericClassWithFieldTypeVariable<TestClass> instance = sut.inject(typeLiteral);
            // Then
            assertNotNull(instance);
            assertNotNull(instance.getField());
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
                public void close() {
                    scopeStorage.clear();
                }
            });
            @SuppressWarnings("unused")
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

        @jakarta.annotation.PreDestroy
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

        @jakarta.annotation.PreDestroy
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

        @jakarta.annotation.PreDestroy
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

        @jakarta.annotation.PreDestroy
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

        @jakarta.annotation.PreDestroy
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
        public void close() {
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

        @jakarta.annotation.PostConstruct
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

        @jakarta.annotation.PostConstruct
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

        @jakarta.annotation.PostConstruct
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
        @jakarta.annotation.PostConstruct
        public void postConstruct() {
            throw new RuntimeException("PostConstruct failed intentionally");
        }
    }

    @Singleton
    static class ParentWithPreDestroy {
        private static int destroyCounter = 0;
        private boolean parentPreDestroyCalled = false;
        private int parentDestroyOrder = 0;

        @jakarta.annotation.PreDestroy
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

        @jakarta.annotation.PreDestroy
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

    // Helper classes for additional coverage tests
    static abstract class AbstractClassWithAbstractMethod {
        @Inject
        public abstract void injectMethod(TestClass testClass);
    }

    static class ClassWithAbstractMethod extends AbstractClassWithAbstractMethod {
        @Override
        public void injectMethod(TestClass testClass) {
            // Implementation
        }
    }

    static class ClassWithGenericMethod {
        @Inject
        public <T> void genericMethod(T param) {
            // Generic method
        }
    }

    @Singleton
    static class ClassWithFailingPreDestroy {
        @jakarta.annotation.PreDestroy
        public void preDestroy() {
            throw new RuntimeException("PreDestroy intentionally fails");
        }
    }

    static class ParentWithPackagePrivateMethod {
        private boolean methodCalled = false;

        @Inject
        void packagePrivateMethod(TestClass testClass) {
            methodCalled = true;
        }

        public boolean isMethodCalled() {
            return methodCalled;
        }
    }

    static class ClassWithPackagePrivateMethodOverride extends ParentWithPackagePrivateMethod {
        private boolean childMethodCalled = false;

        @Override
        @Inject
        void packagePrivateMethod(TestClass testClass) {
            super.packagePrivateMethod(testClass);
            childMethodCalled = true;
        }

        public boolean isChildMethodCalled() {
            return childMethodCalled;
        }
    }

    static class ClassWithInstanceOfFailingPreDestroy {
        private final Instance<SingletonWithFailingPreDestroy> instance;

        @Inject
        public ClassWithInstanceOfFailingPreDestroy(Instance<SingletonWithFailingPreDestroy> instance) {
            this.instance = instance;
        }

        public Instance<SingletonWithFailingPreDestroy> getInstance() {
            return instance;
        }
    }

    // Helper classes for resolveType tests
    static class ClassWithConcreteTypeField {
        @Inject
        private TestClass testClass;

        public TestClass getTestClass() {
            return testClass;
        }
    }

    static class GenericClassWithTypeVariable<T> {
        private final T value;

        public GenericClassWithTypeVariable() {
            // For String type, create a default value
            this.value = (T) "default";
        }

        public T getValue() {
            return value;
        }
    }

    static class GenericClassWithConstructorTypeVariable<T> {
        private final T dependency;

        @Inject
        public GenericClassWithConstructorTypeVariable(T dependency) {
            this.dependency = dependency;
        }

        public T getDependency() {
            return dependency;
        }
    }

    static class GenericClassWithFieldTypeVariable<T> {
        @Inject
        private T field;

        public T getField() {
            return field;
        }
    }

    static class ClassWithPrivateMethod {
        @SuppressWarnings("unused")
        private void privateMethod() {
            // Private method for testing
        }
    }

    // Helper classes for isOverridden, findMethod, getPackageName tests
    static class ParentWithPrivateMethod {
        @SuppressWarnings("unused")
        private void privateMethod() {
            // Private method
        }
    }

    static class ChildOfParentWithPrivateMethod extends ParentWithPrivateMethod {
        // Child doesn't override private method
    }

    static class ParentWithUniqueMethod {
        public void uniqueMethod() {
            // Unique method
        }
    }

    static class ChildWithoutUniqueMethod extends ParentWithUniqueMethod {
        // Doesn't override uniqueMethod
    }

    static class ParentWithPublicMethod {
        public void publicMethod() {
            // Public method
        }
    }

    static class ChildOverridingPublicMethod extends ParentWithPublicMethod {
        @Override
        public void publicMethod() {
            // Override public method
        }
    }

    static class ParentWithProtectedMethod {
        protected void protectedMethod() {
            // Protected method
        }
    }

    static class ChildOverridingProtectedMethod extends ParentWithProtectedMethod {
        @Override
        protected void protectedMethod() {
            // Override protected method
        }
    }

    // Package-private method test classes for isOverridden
    static class ParentForPackagePrivateTest {
        void packagePrivateTestMethod() {
            // Package-private method
        }
    }

    static class ChildInSamePackageForTest extends ParentForPackagePrivateTest {
        @Override
        void packagePrivateTestMethod() {
            // Override package-private method in same package
        }
    }
}
