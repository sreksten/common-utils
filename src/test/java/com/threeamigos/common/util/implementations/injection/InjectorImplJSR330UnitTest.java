package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.literals.AnnotationLiteral;
import org.atinject.tck.auto.*;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.RoundThing;
import org.atinject.tck.auto.accessories.SpareTire;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import jakarta.inject.Provider;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSR 330 Technology Compatibility Kit unit tests")
@Execution(ExecutionMode.SAME_THREAD)
public class InjectorImplJSR330UnitTest {

    private static FuelTank NEVER_INJECTED;

    @BeforeAll
    static void setUpClass() throws NoSuchFieldException, IllegalAccessException {
        NEVER_INJECTED = getStaticField(Tire.class, "NEVER_INJECTED");
    }

    private Convertible car;
    private Cupholder cupholder;
    private SpareTire spareTire;
    private Tire plainTire;
    private Engine engine;

    @BeforeEach
    void setUp() throws NoSuchFieldException , IllegalAccessException {
        // Given
        InjectorImpl injector = new InjectorImpl("org.atinject.tck.auto");
        injector.clearState();
        injector.bind(Seat.class, Collections.singleton(AnnotationLiteral.of(Drivers.class)), DriversSeat.class);
        injector.bind(Tire.class, Collections.singleton(new NamedLiteral("spare")), SpareTire.class);
        resetStaticState();

        // When
        car = (Convertible) injector.inject(Car.class);
        // All Convertible fields are private, so we have to access them
        cupholder = getField(car, "cupholder");
        spareTire = getField(car, "spareTire");
        plainTire = getField(car, "fieldPlainTire");
        Provider<Engine> engineProvider = getField(car, "engineProvider");
        engine = engineProvider.get();
    }

    private void resetStaticState() throws NoSuchFieldException, IllegalAccessException {
        // Reset TCK static fields if they exist.
        // The JSR-330 TCK classes often need their static state cleared between injector runs
        // because they use static booleans to track if injection happened.

        Tire.staticMethodInjectedBeforeStaticFields = false;
        Tire.subtypeStaticFieldInjectedBeforeSupertypeStaticMethods = false;
        Tire.subtypeStaticMethodInjectedBeforeSupertypeStaticMethods = false;

        Field field;

        field = Tire.class.getDeclaredField("staticFieldInjection");
        field.setAccessible(true);
        field.set(null, NEVER_INJECTED);

        field = Tire.class.getDeclaredField("staticMethodInjection");
        field.setAccessible(true);
        field.set(null, NEVER_INJECTED);

        field = SpareTire.class.getDeclaredField("staticFieldInjection");
        field.setAccessible(true);
        field.set(null, NEVER_INJECTED);

        field = SpareTire.class.getDeclaredField("staticMethodInjection");
        field.setAccessible(true);
        field.set(null, NEVER_INJECTED);
    }

    @Nested
    @DisplayName("Smoke tests: if these fail all bets are off")
    class SmokeTests {

        @Test
        public void testFieldsInjected() {
            assertTrue(cupholder != null && spareTire != null);
        }

        @Test
        public void testProviderReturnedValues() {
            assertNotNull(engine);
        }
    }

    @Nested
    @DisplayName("Injecting different kinds of members")
    class InjectingDifferentKindsOfMembers {

        @Test
        public void testMethodWithZeroParametersInjected() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(car, "methodWithZeroParamsInjected"));
        }

        @Test
        public void testMethodWithMultipleParametersInjected() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(car, "methodWithMultipleParamsInjected"));
        }

        @Test
        public void testNonVoidMethodInjected() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(car, "methodWithNonVoidReturnInjected"));
        }

        @Test
        public void testPublicNoArgsConstructorInjected() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(engine, Engine.class, "publicNoArgsConstructorInjected"));
        }

        @Test
        public void testSubtypeFieldsInjected() {
            assertTrue(spareTire.hasSpareTireBeenFieldInjected());
        }

        @Test
        public void testSubtypeMethodsInjected() {
            assertTrue(spareTire.hasSpareTireBeenMethodInjected());
        }

        @Test
        public void testSupertypeFieldsInjected() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            assertTrue(getBooleanMethod(spareTire, Tire.class, "hasTireBeenFieldInjected"));
        }

        @Test
        public void testSupertypeMethodsInjected() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            assertTrue(getBooleanMethod(spareTire, Tire.class, "hasTireBeenMethodInjected"));
        }

        @Test
        public void testTwiceOverriddenMethodInjectedWhenMiddleLacksAnnotation() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(engine, Engine.class, "overriddenTwiceWithOmissionInMiddleInjected"));
        }
    }

    @Nested
    @DisplayName("Injected values")
    class InjectedValues {

        @Test
        public void testQualifiersNotInheritedFromOverriddenMethod() {
            assertFalse(engine.qualifiersInheritedFromOverriddenMethod);
        }

        @Nested
        @DisplayName("testConstructorInjectionWithValues")
        class ConstructorInjectionWithValuesTests {

            @Test
            public void testConstructorInjectionWithValues1() throws NoSuchFieldException, IllegalAccessException {
                Seat constructorPlainSeat = getField(car, "constructorPlainSeat");
                assertFalse(constructorPlainSeat instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testConstructorInjectionWithValues2() throws NoSuchFieldException, IllegalAccessException {
                Tire constructorPlainTire = getField(car, "constructorPlainTire");
                assertFalse(constructorPlainTire instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testConstructorInjectionWithValues3() throws NoSuchFieldException, IllegalAccessException {
                Seat constructorDriversSeat = getField(car, "constructorDriversSeat");
                assertInstanceOf(DriversSeat.class, constructorDriversSeat, "Expected qualified value");
            }

            @Test
            public void testConstructorInjectionWithValues4() throws NoSuchFieldException, IllegalAccessException {
                Tire constructorSpareTire = getField(car, "constructorSpareTire");
                assertInstanceOf(SpareTire.class, constructorSpareTire, "Expected qualified value");
            }
        }

        @Nested
        @DisplayName("testFieldInjectionWithValues")
        class FieldInjectionWithValuesTests {

            @Test
            public void testFieldInjectionWithValues1() throws NoSuchFieldException, IllegalAccessException {
                Seat fieldPlainSeat = getField(car, "fieldPlainSeat");
                assertFalse(fieldPlainSeat instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testFieldInjectionWithValues2() throws NoSuchFieldException, IllegalAccessException {
                Tire fieldPlainTire = getField(car, "fieldPlainTire");
                assertFalse(fieldPlainTire instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testFieldInjectionWithValues3() throws NoSuchFieldException, IllegalAccessException {
                Seat fieldDriversSeat = getField(car, "fieldDriversSeat");
                assertInstanceOf(DriversSeat.class, fieldDriversSeat, "Expected qualified value");
            }

            @Test
            public void testFieldInjectionWithValues4() throws NoSuchFieldException, IllegalAccessException {
                Tire fieldSpareTire = getField(car, "fieldSpareTire");
                assertInstanceOf(SpareTire.class, fieldSpareTire, "Expected qualified value");
            }
        }

        @Nested
        @DisplayName("testMethodInjectionWithValues")
        class MethodInjectionWithValuesTests {

            @Test
            public void testMethodInjectionWithValues1() throws NoSuchFieldException, IllegalAccessException {
                Seat methodPlainSeat = getField(car, "methodPlainSeat");
                assertFalse(methodPlainSeat instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testMethodInjectionWithValues2() throws NoSuchFieldException, IllegalAccessException {
                Tire methodPlainTire = getField(car, "methodPlainTire");
                assertFalse(methodPlainTire instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testMethodInjectionWithValues3() throws NoSuchFieldException, IllegalAccessException {
                Seat methodDriversSeat = getField(car, "methodDriversSeat");
                assertInstanceOf(DriversSeat.class, methodDriversSeat, "Expected qualified value");
            }

            @Test
            public void testMethodInjectionWithValues4() throws NoSuchFieldException, IllegalAccessException {
                Tire methodSpareTire = getField(car, "methodSpareTire");
                assertInstanceOf(SpareTire.class, methodSpareTire, "Expected qualified value");
            }
        }
    }

    @Nested
    @DisplayName("Injected providers")
    class InjectedProviders {

        @Nested
        @DisplayName("testConstructorInjectionWithProviders")
        class ConstructorInjectionWithProvidersTests {

            @Test
            public void testConstructorInjectionWithProviders1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> constructorPlainSeatProvider = getField(car, "constructorPlainSeatProvider");
                assertFalse(constructorPlainSeatProvider.get() instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testConstructorInjectionWithProviders2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> constructorPlainTireProvider = getField(car, "constructorPlainTireProvider");
                assertFalse(constructorPlainTireProvider.get() instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testConstructorInjectionWithProviders3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> constructorDriversSeatProvider = getField(car, "constructorDriversSeatProvider");
                assertInstanceOf(DriversSeat.class, constructorDriversSeatProvider.get(), "Expected qualified value");
            }

            @Test
            public void testConstructorInjectionWithProviders4() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> constructorSpareTireProvider = getField(car, "constructorSpareTireProvider");
                assertInstanceOf(SpareTire.class, constructorSpareTireProvider.get(), "Expected qualified value");
            }
        }

        @Nested
        @DisplayName("testFieldInjectionWithProviders")
        class FieldInjectionWithProvidersTests {

            @Test
            public void testFieldInjectionWithProviders1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> fieldPlainSeatProvider = getField(car, "fieldPlainSeatProvider");
                assertFalse(fieldPlainSeatProvider.get() instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testFieldInjectionWithProviders2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> fieldPlainTireProvider = getField(car, "fieldPlainTireProvider");
                assertFalse(fieldPlainTireProvider.get() instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testFieldInjectionWithProviders3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> fieldDriversSeatProvider = getField(car, "fieldDriversSeatProvider");
                assertInstanceOf(DriversSeat.class, fieldDriversSeatProvider.get(), "Expected qualified value");
            }

            @Test
            public void testFieldInjectionWithProviders4() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> fieldSpareTireProvider = getField(car, "fieldSpareTireProvider");
                assertInstanceOf(SpareTire.class, fieldSpareTireProvider.get(), "Expected qualified value");
            }
        }

        @Nested
        @DisplayName("testMethodInjectionWithProviders")
        class MethodInjectionWithProvidersTests {

            @Test
            public void testMethodInjectionWithProviders1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> methodPlainSeatProvider = getField(car, "methodPlainSeatProvider");
                assertFalse(methodPlainSeatProvider.get() instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testMethodInjectionWithProviders2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> methodPlainTireProvider = getField(car, "methodPlainTireProvider");
                assertFalse(methodPlainTireProvider.get() instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testMethodInjectionWithProviders3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> methodDriversSeatProvider = getField(car, "methodDriversSeatProvider");
                assertInstanceOf(DriversSeat.class, methodDriversSeatProvider.get(), "Expected qualified value");
            }

            @Test
            public void testMethodInjectionWithProviders4() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> methodSpareTireProvider = getField(car, "methodSpareTireProvider");
                assertInstanceOf(SpareTire.class, methodSpareTireProvider.get(), "Expected qualified value");
            }
        }
    }

    @Nested
    @DisplayName("Singletons")
    class SingletonsTests {

        @Test
        public void testConstructorInjectedProviderYieldsSingleton() throws NoSuchFieldException, IllegalAccessException {
            Provider<Seat> constructorPlainSeatProvider = getField(car, "constructorPlainSeatProvider");
            assertSame(constructorPlainSeatProvider.get(), constructorPlainSeatProvider.get(), "Expected same value");
        }

        @Test
        public void testFieldInjectedProviderYieldsSingleton() throws NoSuchFieldException, IllegalAccessException {
            Provider<Seat> fieldPlainSeatProvider = getField(car, "fieldPlainSeatProvider");
            assertSame(fieldPlainSeatProvider.get(), fieldPlainSeatProvider.get(), "Expected same value");
        }

        @Test
        public void testMethodInjectedProviderYieldsSingleton() throws NoSuchFieldException, IllegalAccessException {
            Provider<Seat> methodPlainSeatProvider = getField(car, "methodPlainSeatProvider");
            assertSame(methodPlainSeatProvider.get(), methodPlainSeatProvider.get(), "Expected same value");
        }

        @Test
        public void testCircularlyDependentSingletons() {
            // uses provider.get() to get around circular deps
            assertSame(cupholder.seatProvider.get().getCupholder(), cupholder);
        }
    }

    @Nested
    @DisplayName("Non-Singletons")
    class NonSingletonsTests {

        @Test
        public void testSingletonAnnotationNotInheritedFromSupertype() throws NoSuchFieldException, IllegalAccessException {
            Seat driversSeatA = getField(car, "driversSeatA");
            Seat driversSeatB = getField(car, "driversSeatB");
            assertNotSame(driversSeatA, driversSeatB);
        }

        @Nested
        @DisplayName("testConstructorInjectedProviderYieldsDistinctValues")
        class ConstructorInjectedProviderYieldsDistinctValues {

            @Test
            public void testConstructorInjectedProviderYieldsDistinctValues1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> constructorDriversSeatProvider = getField(car, "constructorDriversSeatProvider");
                assertNotSame(constructorDriversSeatProvider.get(), constructorDriversSeatProvider.get(), "Expected distinct values");
            }

            @Test
            public void testConstructorInjectedProviderYieldsDistinctValues2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> constructorPlainTireProvider = getField(car, "constructorPlainTireProvider");
                assertNotSame(constructorPlainTireProvider.get(), constructorPlainTireProvider.get(), "Expected distinct values");
            }

            @Test
            public void testConstructorInjectedProviderYieldsDistinctValues3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> constructorSpareTireProvider = getField(car, "constructorSpareTireProvider");
                assertNotSame(constructorSpareTireProvider.get(), constructorSpareTireProvider.get(), "Expected distinct values");
            }

        }

        @Nested
        @DisplayName("testFieldInjectedProviderYieldsDistinctValues")
        class FieldInjectedProviderYieldsDistinctValues {

            @Test
            public void testFieldInjectedProviderYieldsDistinctValues1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> fieldDriversSeatProvider = getField(car, "fieldDriversSeatProvider");
                assertNotSame(fieldDriversSeatProvider.get(), fieldDriversSeatProvider.get(), "Expected distinct values");
            }

            @Test
            public void testFieldInjectedProviderYieldsDistinctValues2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> fieldPlainTireProvider = getField(car, "fieldPlainTireProvider");
                assertNotSame(fieldPlainTireProvider.get(), fieldPlainTireProvider.get(), "Expected distinct values");
            }

            @Test
            public void testFieldInjectedProviderYieldsDistinctValues3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> fieldSpareTireProvider = getField(car, "fieldSpareTireProvider");
                assertNotSame(fieldSpareTireProvider.get(), fieldSpareTireProvider.get(), "Expected distinct values");
            }
        }

        @Nested
        @DisplayName("testMethodInjectedProviderYieldsDistinctValues")
        class MethodInjectedProviderYieldsDistinctValues {

            @Test
            public void testMethodInjectedProviderYieldsDistinctValues1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> methodDriversSeatProvider = getField(car, "methodDriversSeatProvider");
                assertNotSame(methodDriversSeatProvider.get(), methodDriversSeatProvider.get(), "Expected distinct values");
            }

            @Test
            public void testMethodInjectedProviderYieldsDistinctValues2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> methodPlainTireProvider = getField(car, "methodPlainTireProvider");
                assertNotSame(methodPlainTireProvider.get(), methodPlainTireProvider.get(), "Expected distinct values");
            }

            @Test
            public void testMethodInjectedProviderYieldsDistinctValues3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> methodSpareTireProvider = getField(car, "methodSpareTireProvider");
                assertNotSame(methodSpareTireProvider.get(), methodSpareTireProvider.get(), "Expected distinct values");
            }
        }
    }

    @Nested
    @DisplayName("Mix inheritance + visibility")
    class InheritanceAndVisibilityMixTests {

        @Nested
        @DisplayName("testPackagePrivateMethodInjectedDifferentPackages")
        class PackagePrivateMethodInjectedDifferentPackages {

            @Test
            public void testPackagePrivateMethodInjectedDifferentPackages1() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, Tire.class, "subPackagePrivateMethodInjected"));
            }

            @Test
            public void testPackagePrivateMethodInjectedDifferentPackages2() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, Tire.class, "superPackagePrivateMethodInjected"));
            }
        }

        @Nested
        @DisplayName("testOverriddenProtectedMethodInjection")
        class OverriddenProtectedMethodInjection {

            @Test
            public void testOverriddenProtectedMethodInjection1() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, Tire.class, "subProtectedMethodInjected"));
            }

            @Test
            public void testOverriddenProtectedMethodInjection2() throws NoSuchFieldException, IllegalAccessException {
                assertFalse(getBooleanField(spareTire, Tire.class, "superProtectedMethodInjected"));
            }
        }

        @Nested
        @DisplayName("testOverriddenPublicMethodNotInjected")
        class OverriddenPublicMethodNotInjected {

            @Test
            public void testOverriddenPublicMethodNotInjected1() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, Tire.class, "subPublicMethodInjected"));
            }

            @Test
            public void testOverriddenPublicMethodNotInjected2() throws NoSuchFieldException, IllegalAccessException {
                assertFalse(getBooleanField(spareTire, Tire.class, "superPublicMethodInjected"));
            }
        }
    }

    @Nested
    @DisplayName("Inject in order")
    class InjectInOrderTests {

        @Test
        public void testFieldsInjectedBeforeMethods() {
            assertFalse(spareTire.methodInjectedBeforeFields);
        }

        @Test
        public void testSupertypeMethodsInjectedBeforeSubtypeFields() {
            assertFalse(spareTire.subtypeFieldInjectedBeforeSupertypeMethods);
        }

        @Test
        public void testSupertypeMethodInjectedBeforeSubtypeMethods() {
            assertFalse(spareTire.subtypeMethodInjectedBeforeSupertypeMethods);
        }
    }

    @Nested
    @DisplayName("Necessary injections occur")
    class NecessaryInjectionsOccurTests {

        @Test
        public void testPackagePrivateMethodInjectedEvenWhenSimilarMethodLacksAnnotation() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(spareTire, Tire.class, "subPackagePrivateMethodForOverrideInjected"));
        }
    }

    @Nested
    @DisplayName("Override or similar method without @Inject")
    class OverrideOrSimilarMethodWithoutInjectTests {

        @Test
        public void testPrivateMethodNotInjectedWhenSupertypeHasAnnotatedSimilarMethod() throws NoSuchFieldException, IllegalAccessException {
            assertFalse(getBooleanField(spareTire, Tire.class, "superPrivateMethodForOverrideInjected"));
        }

        @Nested
        @DisplayName("testPackagePrivateMethodNotInjectedWhenOverrideLacksAnnotation")
        class PackagePrivateMethodNotInjectedWhenOverrideLacksAnnotation {

            @Test
            public void testPackagePrivateMethodNotInjectedWhenOverrideLacksAnnotation1() throws NoSuchFieldException, IllegalAccessException {
                assertFalse(getBooleanField(engine, Engine.class, "subPackagePrivateMethodForOverrideInjected"));
            }

            @Test
            public void testPackagePrivateMethodNotInjectedWhenOverrideLacksAnnotation2() throws NoSuchFieldException, IllegalAccessException {
                assertFalse(getBooleanField(engine, Engine.class, "superPackagePrivateMethodForOverrideInjected"));
            }
        }

        @Test
        public void testPackagePrivateMethodNotInjectedWhenSupertypeHasAnnotatedSimilarMethod() throws NoSuchFieldException, IllegalAccessException {
            assertFalse(getBooleanField(spareTire, Tire.class, "superPackagePrivateMethodForOverrideInjected"));
        }

        @Test
        public void testProtectedMethodNotInjectedWhenOverrideNotAnnotated() throws NoSuchFieldException, IllegalAccessException {
            assertFalse(getBooleanField(spareTire, Tire.class, "protectedMethodForOverrideInjected"));
        }

        @Test
        public void testPublicMethodNotInjectedWhenOverrideNotAnnotated() throws NoSuchFieldException, IllegalAccessException {
            assertFalse(getBooleanField(spareTire, Tire.class, "publicMethodForOverrideInjected"));
        }

        @Test
        public void testTwiceOverriddenMethodNotInjectedWhenOverrideLacksAnnotation() throws NoSuchFieldException, IllegalAccessException {
            assertFalse(getBooleanField(engine, Engine.class, "overriddenTwiceWithOmissionInSubclassInjected"));
        }

        @Nested
        @DisplayName("testOverriddingMixedWithPackagePrivate2")
        class OverridingMixedWithPackagePrivate2Tests {

            @Test
            public void testOverridingMixedWithPackagePrivate21() {
                assertTrue(spareTire.packagePrivateMethod2Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate22() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, "packagePrivateMethod2Injected"));
            }

            @Test
            public void testOverridingMixedWithPackagePrivate23() {
                assertFalse(((RoundThing) spareTire).packagePrivateMethod2Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate24() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(plainTire, "packagePrivateMethod2Injected"));
            }

            @Test
            public void testOverridingMixedWithPackagePrivate25() {
                assertTrue(((RoundThing) plainTire).packagePrivateMethod2Injected);
            }
        }

        @Nested
        @DisplayName("testOverriddingMixedWithPackagePrivate3")
        class OverridingMixedWithPackagePrivate3Tests {

            @Test
            public void testOverridingMixedWithPackagePrivate31() {
                assertFalse(spareTire.packagePrivateMethod3Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate32() {
                assertTrue(((Tire) spareTire).packagePrivateMethod3Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate33() {
                assertFalse(((RoundThing) spareTire).packagePrivateMethod3Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate34() {
                assertTrue(plainTire.packagePrivateMethod3Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate35() {
                assertTrue(((RoundThing) plainTire).packagePrivateMethod3Injected);
            }
        }

        @Nested
        @DisplayName("testOverriddingMixedWithPackagePrivate4")
        class OverridingMixedWithPackagePrivate4Tests {

            @Test
            public void testOverridingMixedWithPackagePrivate41() {
                assertFalse(plainTire.packagePrivateMethod4Injected);
            }

            @Test
            public void testOverridingMixedWithPackagePrivate42() {
                assertFalse(plainTire.packagePrivateMethod4Injected);
            }
        }
    }

    @Nested
    @DisplayName("Inject only once")
    class InjectOnlyOnce {

        @Test
        public void testOverriddenPackagePrivateMethodInjectedOnlyOnce() {
            assertFalse(engine.overriddenPackagePrivateMethodInjectedTwice);
        }

        @Test
        public void testSimilarPackagePrivateMethodInjectedOnlyOnce() {
            assertFalse(spareTire.similarPackagePrivateMethodInjectedTwice);
        }

        @Test
        public void testOverriddenProtectedMethodInjectedOnlyOnce() {
            assertFalse(spareTire.overriddenProtectedMethodInjectedTwice);
        }

        @Test
        public void testOverriddenPublicMethodInjectedOnlyOnce() {
            assertFalse(spareTire.overriddenPublicMethodInjectedTwice);
        }
    }

    @Nested
    @DisplayName("Static tests")
    //@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StaticTests {

        @Test
        public void testSubtypeStaticFieldsInjected() {
            assertTrue(SpareTire.hasBeenStaticFieldInjected());
        }

        @Test
        public void testSubtypeStaticMethodsInjected() {
            assertTrue(SpareTire.hasBeenStaticMethodInjected());
        }

        @Test
        public void testSupertypeStaticFieldsInjected() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            assertTrue(getStaticBooleanMethod(Tire.class, "hasBeenStaticFieldInjected"));
        }

        @Test
        public void testSupertypeStaticMethodsInjected() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            assertTrue(getStaticBooleanMethod(Tire.class, "hasBeenStaticMethodInjected"));
        }

        @Nested
        @DisplayName("testStaticFieldInjectionWithValues")
        class StaticFieldInjectionWithValuesTests {

            @Test
            public void testStaticFieldInjectionWithValues1() throws NoSuchFieldException, IllegalAccessException {
                Seat staticFieldPlainSeat = getStaticField(Convertible.class, "staticFieldPlainSeat");
                assertFalse(staticFieldPlainSeat instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testStaticFieldInjectionWithValues2() throws NoSuchFieldException, IllegalAccessException {
                Tire staticFieldPlainTire = getStaticField(Convertible.class, "staticFieldPlainTire");
                assertFalse(staticFieldPlainTire instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testStaticFieldInjectionWithValues3() throws NoSuchFieldException, IllegalAccessException {
                Seat staticFieldDriversSeat = getStaticField(Convertible.class, "staticFieldDriversSeat");
                assertInstanceOf(DriversSeat.class, staticFieldDriversSeat, "Expected qualified value");
            }

            @Test
            public void testStaticFieldInjectionWithValues4() throws NoSuchFieldException, IllegalAccessException {
                Tire staticFieldSpareTire = getStaticField(Convertible.class, "staticFieldSpareTire");
                assertInstanceOf(SpareTire.class, staticFieldSpareTire, "Expected qualified value");
            }
        }

        @Nested
        @DisplayName("testStaticMethodInjectionWithValues")
        class TestStaticMethodInjectionWithValues {

            @Test
            public void testStaticMethodInjectionWithValues1() throws NoSuchFieldException, IllegalAccessException {
                Seat staticMethodPlainSeat = getStaticField(Convertible.class, "staticMethodPlainSeat");
                assertFalse(staticMethodPlainSeat instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testStaticMethodInjectionWithValues2() throws NoSuchFieldException, IllegalAccessException {
                Tire staticMethodPlainTire = getStaticField(Convertible.class, "staticMethodPlainTire");
                assertFalse(staticMethodPlainTire instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testStaticMethodInjectionWithValues3() throws NoSuchFieldException, IllegalAccessException {
                Seat staticMethodDriversSeat = getStaticField(Convertible.class, "staticMethodDriversSeat");
                assertInstanceOf(DriversSeat.class, staticMethodDriversSeat, "Expected qualified value");
            }

            @Test
            public void testStaticMethodInjectionWithValues4() throws NoSuchFieldException, IllegalAccessException {
                Tire staticMethodSpareTire = getStaticField(Convertible.class, "staticMethodSpareTire");
                assertInstanceOf(SpareTire.class, staticMethodSpareTire, "Expected qualified value");
            }
        }

        @Test
        public void testStaticFieldsInjectedBeforeMethods() {
            assertFalse(SpareTire.staticMethodInjectedBeforeStaticFields);
        }

        @Test
        public void testSupertypeStaticMethodsInjectedBeforeSubtypeStaticFields() {
            assertFalse(SpareTire.subtypeStaticFieldInjectedBeforeSupertypeStaticMethods);
        }

        @Test
        public void testSupertypeStaticMethodsInjectedBeforeSubtypeStaticMethods() {
            assertFalse(SpareTire.subtypeStaticMethodInjectedBeforeSupertypeStaticMethods);
        }

        @Nested
        @DisplayName("testStaticFieldInjectionWithProviders")
        class StaticFieldInjectionWithProvidersTests {

            @Test
            public void testStaticFieldInjectionWithProviders1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> staticFieldPlainSeatProvider = getStaticField(Convertible.class, "staticFieldPlainSeatProvider");
                assertFalse(staticFieldPlainSeatProvider.get() instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testStaticFieldInjectionWithProviders2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> staticFieldPlainTireProvider = getStaticField(Convertible.class, "staticFieldPlainTireProvider");
                assertFalse(staticFieldPlainTireProvider.get() instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testStaticFieldInjectionWithProviders3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> staticFieldDriversSeatProvider = getStaticField(Convertible.class, "staticFieldDriversSeatProvider");
                assertInstanceOf(DriversSeat.class, staticFieldDriversSeatProvider.get(), "Expected qualified value");
            }

            @Test
            public void testStaticFieldInjectionWithProviders4() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> staticFieldSpareTireProvider = getStaticField(Convertible.class, "staticFieldSpareTireProvider");
                assertInstanceOf(SpareTire.class, staticFieldSpareTireProvider.get(), "Expected qualified value");
            }
        }

        @Nested
        @DisplayName("testStaticMethodInjectionWithProviders")
        class StaticMethodInjectionWithProvidersTests {

            @Test
            public void testStaticMethodInjectionWithProviders1() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> staticMethodPlainSeatProvider = getStaticField(Convertible.class, "staticMethodPlainSeatProvider");
                assertFalse(staticMethodPlainSeatProvider.get() instanceof DriversSeat, "Expected unqualified value");
            }

            @Test
            public void testStaticMethodInjectionWithProviders2() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> staticMethodPlainTireProvider = getStaticField(Convertible.class, "staticMethodPlainTireProvider");
                assertFalse(staticMethodPlainTireProvider.get() instanceof SpareTire, "Expected unqualified value");
            }

            @Test
            public void testStaticMethodInjectionWithProviders3() throws NoSuchFieldException, IllegalAccessException {
                Provider<Seat> staticMethodDriversSeatProvider = getStaticField(Convertible.class, "staticMethodDriversSeatProvider");
                assertInstanceOf(DriversSeat.class, staticMethodDriversSeatProvider.get(), "Expected qualified value");
            }

            @Test
            public void testStaticMethodInjectionWithProviders4() throws NoSuchFieldException, IllegalAccessException {
                Provider<Tire> staticMethodSpareTireProvider = getStaticField(Convertible.class, "staticMethodSpareTireProvider");
                assertInstanceOf(SpareTire.class, staticMethodSpareTireProvider.get(), "Expected qualified value");
            }
        }
    }

    @Nested
    @DisplayName("Private tests")
    class PrivateTests {

        @Nested
        @DisplayName("testSupertypePrivateMethodInjected")
        class SupertypePrivateMethodInjected {

            @Test
            public void testSupertypePrivateMethodInjected1() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, Tire.class, "superPrivateMethodInjected"));
            }

            @Test
            public void testSupertypePrivateMethodInjected2() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(spareTire, Tire.class, "subPrivateMethodInjected"));
            }
        }

        @Nested
        @DisplayName("testPackagePrivateMethodInjectedSamePackage")
        class PackagePrivateMethodInjectedSamePackage {

            @Test
            public void testPackagePrivateMethodInjectedSamePackage1() throws NoSuchFieldException, IllegalAccessException {
                assertTrue(getBooleanField(engine, Engine.class, "subPackagePrivateMethodInjected"));
            }

            @Test
            public void testPackagePrivateMethodInjectedSamePackage2() throws NoSuchFieldException, IllegalAccessException {
                assertFalse(getBooleanField(engine, Engine.class, "superPackagePrivateMethodInjected"));
            }
        }

        @Test
        public void testPrivateMethodInjectedEvenWhenSimilarMethodLacksAnnotation() throws NoSuchFieldException, IllegalAccessException {
            assertTrue(getBooleanField(spareTire, Tire.class, "subPrivateMethodForOverrideInjected"));
        }

        @Test
        public void testSimilarPrivateMethodInjectedOnlyOnce() {
            assertFalse(spareTire.similarPrivateMethodInjectedTwice);
        }
    }

    private boolean getBooleanField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean) field.get(object);
    }

    private boolean getBooleanField(Object object, Class<?> clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean) field.get(object);
    }

    @SuppressWarnings({"SameParameterValue", "unchecked"})
    private static <T> T getStaticField(Class<?> clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean getStaticBooleanMethod(Class<?> clazz, String methodName) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method method = clazz.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (boolean) method.invoke(null);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean getBooleanMethod(Object object, Class<?> clazz, String methodName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (boolean) method.invoke(object);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }
}
