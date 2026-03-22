package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par56programmaticlookup;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.resolution.InstanceImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.literal.QualifierLiteral;
import jakarta.enterprise.inject.literal.SingletonLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("5.6 - Programmatic lookup")
public class ProgrammaticLookupTest {

    @Test
    @DisplayName("select() creates child Instance and applies implicit @Default before typesafe resolution")
    void shouldCreateChildInstanceAndApplyDefaultQualifierRules() {
        Syringe syringe = createProgrammaticLookupSyringe();

        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertEquals("default", consumer.resolveDefaultFromParent());
        assertEquals("special", consumer.resolveSpecialFromAnyChildSelect());
        assertTrue(consumer.isSpecialUnsatisfiedWhenSelectedFromDefaultParent());
    }

    @Test
    @DisplayName("Built-in Instance/Provider is injectable for legal bean type with any qualifiers")
    void shouldInjectBuiltInInstanceAndProviderForLegalTypeAndQualifiers() {
        Syringe syringe = createBuiltInInstanceSyringe();
        BuiltInInstanceConsumer consumer = syringe.inject(BuiltInInstanceConsumer.class);

        assertTrue(consumer.hasAllBuiltInLookupsInjected());
        assertEquals("default", consumer.defaultInstanceValue());
        assertEquals("default", consumer.defaultProviderValue());
        assertEquals("special", consumer.specialInstanceValue());
        assertEquals("special", consumer.specialProviderValue());
    }

    @Test
    @DisplayName("Built-in Instance bean has @Dependent scope semantics")
    void shouldExposeDependentScopeSemanticsForBuiltInInstanceBean() {
        Syringe syringe = createBuiltInInstanceSyringe();
        BuiltInInstanceConsumer consumer = syringe.inject(BuiltInInstanceConsumer.class);

        assertTrue(consumer.dependentBuiltInBeanScopeLooksDependent());
    }

    @Test
    @DisplayName("AnnotationLiteral simplifies select() with qualifiers including qualifier members")
    void shouldSelectUsingAnnotationLiteralQualifiers() {
        Syringe syringe = createLiteralSelectionSyringe();
        LiteralSelectionConsumer consumer = syringe.inject(LiteralSelectionConsumer.class);

        assertEquals("sync-cheque", consumer.getSynchronousPaymentProcessor(PaymentMethod.CHEQUE));
    }

    @Test
    @DisplayName("TypeLiteral simplifies select() with parameterized type and actual type arguments")
    void shouldSelectUsingTypeLiteralForParameterizedType() {
        Syringe syringe = createLiteralSelectionSyringe();
        LiteralSelectionConsumer consumer = syringe.inject(LiteralSelectionConsumer.class);

        assertEquals("generic-cheque", consumer.getChequePaymentProcessorViaTypeLiteral());
    }

    @Test
    @DisplayName("5.6.4 - CDI built-in annotation literals are available via nested Literal classes")
    void shouldProvideCdiBuiltInAnnotationLiterals() throws Exception {
        List<Class<? extends Annotation>> builtIns = Arrays.asList(
                Any.class,
                Default.class,
                Specializes.class,
                Vetoed.class,
                Nonbinding.class,
                Initialized.class,
                Destroyed.class,
                RequestScoped.class,
                SessionScoped.class,
                ApplicationScoped.class,
                Dependent.class,
                ConversationScoped.class,
                Alternative.class,
                Typed.class
        );
        for (Class<? extends Annotation> annotationType : builtIns) {
            assertHasNestedLiteralClass(annotationType);
        }

        assertInstanceConstant(Any.Literal.class, Any.class);
        assertInstanceConstant(Default.Literal.class, Default.class);
        assertInstanceConstant(Specializes.Literal.class, Specializes.class);
        assertInstanceConstant(Vetoed.Literal.class, Vetoed.class);
        assertInstanceConstant(Nonbinding.Literal.class, Nonbinding.class);
        assertInstanceConstant(RequestScoped.Literal.class, RequestScoped.class);
        assertInstanceConstant(SessionScoped.Literal.class, SessionScoped.class);
        assertInstanceConstant(ApplicationScoped.Literal.class, ApplicationScoped.class);
        assertInstanceConstant(Dependent.Literal.class, Dependent.class);
        assertInstanceConstant(ConversationScoped.Literal.class, ConversationScoped.class);
        assertInstanceConstant(Alternative.Literal.class, Alternative.class);

        Default defaultLiteral = new Default.Literal();
        assertEquals(Default.class, defaultLiteral.annotationType());
        RequestScoped requestScopedLiteral = RequestScoped.Literal.INSTANCE;
        assertEquals(RequestScoped.class, requestScopedLiteral.annotationType());

        Initialized initializedForRequest = Initialized.Literal.of(RequestScoped.class);
        Destroyed destroyedForSession = Destroyed.Literal.of(SessionScoped.class);
        assertEquals(RequestScoped.class, initializedForRequest.value());
        assertEquals(SessionScoped.class, destroyedForSession.value());

        Typed typedLiteral = Typed.Literal.of(new Class<?>[]{String.class, Integer.class});
        assertArrayEquals(new Class<?>[]{String.class, Integer.class}, typedLiteral.value());
    }

    @Test
    @DisplayName("5.6.4 - JSR-330 annotation literals are available")
    void shouldProvideJsr330AnnotationLiterals() {
        Inject injectLiteral = InjectLiteral.INSTANCE;
        Qualifier qualifierLiteral = QualifierLiteral.INSTANCE;
        Singleton singletonLiteral = SingletonLiteral.INSTANCE;
        Named namedLiteral = NamedLiteral.of("orders");

        assertEquals(Inject.class, injectLiteral.annotationType());
        assertEquals(Qualifier.class, qualifierLiteral.annotationType());
        assertEquals(Singleton.class, singletonLiteral.annotationType());
        assertEquals("orders", namedLiteral.value());
    }

    @Test
    @DisplayName("Instance.get() throws AmbiguousResolutionException for unresolvable ambiguity")
    void shouldThrowAmbiguousResolutionExceptionWhenGetIsAmbiguous() {
        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertThrows(AmbiguousResolutionException.class, consumer::getFromAmbiguousAnyInstance);
    }

    @Test
    @DisplayName("Instance.get() throws UnsatisfiedResolutionException for unsatisfied dependency")
    void shouldThrowUnsatisfiedResolutionExceptionWhenGetIsUnsatisfied() {
        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertThrows(UnsatisfiedResolutionException.class, consumer::getFromUnsatisfiedChildInstance);
    }

    @Test
    @DisplayName("isUnsatisfied(), isAmbiguous() and isResolvable() report resolution state correctly")
    void shouldReportUnsatisfiedAmbiguousAndResolvableStates() {
        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertTrue(consumer.isUnsatisfiedForMissingQualifier());
        assertTrue(consumer.isAmbiguousForAnyProcessors());
        assertTrue(!consumer.isResolvableForAnyProcessors());

        assertTrue(!consumer.isUnsatisfiedForDefaultProcessors());
        assertTrue(consumer.isResolvableForDefaultProcessors());
    }

    @Test
    @DisplayName("Raw Instance injection point is a definition error")
    void shouldRejectRawInstanceInjectionPoint() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), RawInstanceInjectionPointBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    @Test
    @DisplayName("select() with duplicate non-repeating qualifier type throws IllegalArgumentException")
    void shouldRejectDuplicateNonRepeatingQualifierInSelect() {
        Syringe syringe = createProgrammaticLookupSyringe();

        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);
        assertThrows(IllegalArgumentException.class, consumer::selectWithDuplicateNonRepeatingQualifier);
    }

    @Test
    @DisplayName("select() with non-qualifier annotation throws IllegalArgumentException")
    void shouldRejectNonQualifierAnnotationInSelect() {
        Syringe syringe = createProgrammaticLookupSyringe();

        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);
        assertThrows(IllegalArgumentException.class, consumer::selectWithNonQualifierAnnotation);
    }

    @Test
    @DisplayName("iterator() returns all candidate contextual references when ambiguous and no alternatives exist")
    void shouldIterateAllCandidatesWhenAmbiguousWithoutAlternatives() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        java.util.List<String> ids = consumer.iterateAnyCandidateIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("plain-a"));
        assertTrue(ids.contains("plain-b"));
    }

    @Test
    @DisplayName("iterator() returns empty set for unsatisfied dependency")
    void shouldReturnEmptyIteratorWhenUnsatisfied() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        assertEquals(0, consumer.iterateUnsatisfiedCandidateCount());
    }

    @Test
    @DisplayName("iterator() applies ambiguity elimination and keeps non-eliminated alternatives")
    void shouldIterateOnlyNonEliminatedAlternativesWhenAlternativesPresent() {
        Syringe syringe = createIteratorSyringeWithAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        java.util.List<String> ids = consumer.iterateAnyCandidateIds();
        assertEquals(1, ids.size());
        assertEquals("alt", ids.get(0));
    }

    @Test
    @DisplayName("stream() returns all candidate contextual references when ambiguous and no alternatives exist")
    void shouldStreamAllCandidatesWhenAmbiguousWithoutAlternatives() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        java.util.List<String> ids = consumer.streamAnyCandidateIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("plain-a"));
        assertTrue(ids.contains("plain-b"));
    }

    @Test
    @DisplayName("stream() returns empty set for unsatisfied dependency")
    void shouldReturnEmptyStreamWhenUnsatisfied() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        assertEquals(0L, consumer.streamUnsatisfiedCandidateCount());
    }

    @Test
    @DisplayName("stream() applies ambiguity elimination and streams non-eliminated alternatives")
    void shouldStreamOnlyNonEliminatedAlternativesWhenAlternativesPresent() {
        Syringe syringe = createIteratorSyringeWithAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        java.util.List<String> ids = consumer.streamAnyCandidateIds();
        assertEquals(1, ids.size());
        assertEquals("alt", ids.get(0));
    }

    @Test
    @DisplayName("handles() iterates contextual reference handles for all beans produced by iterator()/stream()")
    void shouldReturnHandlesForAllBeansProducedByIteratorAndStream() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        java.util.List<String> iteratorIds = consumer.iterateAnyCandidateIds();
        java.util.List<String> streamIds = consumer.streamAnyCandidateIds();
        java.util.List<String> handleIds = consumer.handleAnyCandidateIds();

        assertEquals(iteratorIds.size(), handleIds.size());
        assertTrue(handleIds.containsAll(iteratorIds));
        assertTrue(handleIds.containsAll(streamIds));
    }

    @Test
    @DisplayName("handles() returns stateless Iterable: each iterator() yields a new handle set")
    void shouldProvideStatelessHandlesIterable() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        assertTrue(consumer.handlesIterableCreatesNewHandleSetPerIteratorCall());
    }

    @Test
    @DisplayName("handlesStream() is equivalent to handles()")
    void shouldProvideHandlesStreamEquivalentToHandles() {
        Syringe syringe = createIteratorSyringeWithoutAlternatives();
        ProgrammaticLookupIteratorConsumer consumer = syringe.inject(ProgrammaticLookupIteratorConsumer.class);

        java.util.List<String> handlesIds = consumer.handleAnyCandidateIds();
        java.util.List<String> handlesStreamIds = consumer.handleStreamAnyCandidateIds();

        assertEquals(handlesIds.size(), handlesStreamIds.size());
        assertTrue(handlesIds.containsAll(handlesStreamIds));
        assertTrue(handlesStreamIds.containsAll(handlesIds));
    }

    @Test
    @DisplayName("destroy() destroys dependent instance obtained from the same Instance")
    void shouldDestroyDependentInstanceFromSameInstance() {
        DependentDestroyableBean.reset();

        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertEquals(1, consumer.destroyDependentInstanceAndGetPreDestroyCalls());
    }

    @Test
    @DisplayName("destroy() accepts normal scoped client proxy instance")
    void shouldAcceptDestroyForNormalScopedProxy() {
        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        consumer.destroyNormalScopedProxy();
    }

    @Test
    @DisplayName("destroy() propagates UnsupportedOperationException when context does not support destroy")
    void shouldThrowUnsupportedOperationWhenDestroyNotSupported() {
        InstanceImpl<Object> instance = new InstanceImpl<>(
                Object.class,
                java.util.Collections.<java.lang.annotation.Annotation>emptyList(),
                new InstanceImpl.ResolutionStrategy<Object>() {
                    @Override
                    public Object resolveInstance(Class<Object> type, java.util.Collection<java.lang.annotation.Annotation> qualifiers) {
                        return new Object();
                    }

                    @Override
                    public java.util.Collection<Class<? extends Object>> resolveImplementations(Class<Object> type, java.util.Collection<java.lang.annotation.Annotation> qualifiers) {
                        return java.util.Collections.<Class<? extends Object>>singletonList(Object.class);
                    }

                    @Override
                    public void invokePreDestroy(Object instance) {
                        throw new UnsupportedOperationException("Destroy not supported");
                    }
                }
        );

        assertThrows(UnsupportedOperationException.class, () -> instance.destroy(new Object()));
    }

    @Test
    @DisplayName("getHandle() throws UnsatisfiedResolutionException when no bean matches")
    void shouldThrowUnsatisfiedResolutionExceptionForGetHandle() {
        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertThrows(UnsatisfiedResolutionException.class, consumer::getUnsatisfiedHandle);
    }

    @Test
    @DisplayName("getHandle() throws AmbiguousResolutionException when more than one bean matches")
    void shouldThrowAmbiguousResolutionExceptionForGetHandle() {
        Syringe syringe = createProgrammaticLookupSyringe();
        ProgrammaticLookupConsumer consumer = syringe.inject(ProgrammaticLookupConsumer.class);

        assertThrows(AmbiguousResolutionException.class, consumer::getAmbiguousHandle);
    }

    @Test
    @DisplayName("getHandle() returns lazily initialized contextual reference")
    void shouldReturnLazilyInitializedHandle() {
        AtomicInteger resolveCalls = new AtomicInteger(0);
        InstanceImpl<HandlePayload> instance = testInstance(resolveCalls, new AtomicInteger(0));

        Instance.Handle<HandlePayload> handle = instance.getHandle();
        assertEquals(0, resolveCalls.get());

        HandlePayload payload = handle.get();
        assertEquals("handle-payload", payload.value());
        assertEquals(1, resolveCalls.get());
    }

    @Test
    @DisplayName("Handle.getBean() returns Bean metadata for the contextual instance")
    void shouldExposeBeanMetadataThroughHandle() {
        InstanceImpl<HandlePayload> instance = testInstance(new AtomicInteger(0), new AtomicInteger(0));
        Instance.Handle<HandlePayload> handle = instance.getHandle();
        jakarta.enterprise.inject.spi.Bean<HandlePayload> bean = handle.getBean();

        assertNotNull(bean);
        assertEquals(HandlePayload.class, bean.getBeanClass());
    }

    @Test
    @DisplayName("Handle.get() throws IllegalStateException after successful destroy")
    void shouldThrowIllegalStateAfterHandleDestroy() {
        InstanceImpl<HandlePayload> instance = testInstance(new AtomicInteger(0), new AtomicInteger(0));
        Instance.Handle<HandlePayload> handle = instance.getHandle();
        handle.get();
        handle.destroy();

        assertThrows(IllegalStateException.class, handle::get);
    }

    @Test
    @DisplayName("Handle.destroy() is a no-op when get() was never called")
    void shouldNoOpDestroyWhenHandleNeverInitialized() {
        AtomicInteger resolveCalls = new AtomicInteger(0);
        AtomicInteger destroyCalls = new AtomicInteger(0);
        InstanceImpl<HandlePayload> instance = testInstance(resolveCalls, destroyCalls);
        Instance.Handle<HandlePayload> handle = instance.getHandle();
        handle.destroy();
        handle.destroy();

        assertEquals(0, resolveCalls.get());
        assertEquals(0, destroyCalls.get());
    }

    @Test
    @DisplayName("Handle.destroy() is idempotent when called multiple times")
    void shouldDestroyHandleContextualReferenceOnlyOnce() {
        AtomicInteger resolveCalls = new AtomicInteger(0);
        AtomicInteger destroyCalls = new AtomicInteger(0);
        InstanceImpl<HandlePayload> instance = testInstance(resolveCalls, destroyCalls);
        Instance.Handle<HandlePayload> handle = instance.getHandle();
        handle.get();
        handle.destroy();
        handle.destroy();

        assertEquals(1, destroyCalls.get());
    }

    @Test
    @DisplayName("Handle.close() delegates to destroy()")
    void shouldDelegateCloseToDestroy() throws Exception {
        AtomicInteger resolveCalls = new AtomicInteger(0);
        AtomicInteger destroyCalls = new AtomicInteger(0);
        InstanceImpl<HandlePayload> instance = testInstance(resolveCalls, destroyCalls);
        Instance.Handle<HandlePayload> handle = instance.getHandle();
        handle.get();
        handle.close();

        assertEquals(1, destroyCalls.get());
        assertThrows(IllegalStateException.class, handle::get);
    }

    private InstanceImpl<HandlePayload> testInstance(AtomicInteger resolveCalls, AtomicInteger destroyCalls) {
        return new InstanceImpl<>(
                HandlePayload.class,
                java.util.Collections.<java.lang.annotation.Annotation>emptyList(),
                new InstanceImpl.ResolutionStrategy<HandlePayload>() {
                    @Override
                    public HandlePayload resolveInstance(Class<HandlePayload> type, java.util.Collection<java.lang.annotation.Annotation> qualifiers) {
                        resolveCalls.incrementAndGet();
                        return new HandlePayload();
                    }

                    @Override
                    public java.util.Collection<Class<? extends HandlePayload>> resolveImplementations(Class<HandlePayload> type, java.util.Collection<java.lang.annotation.Annotation> qualifiers) {
                        return java.util.Collections.<Class<? extends HandlePayload>>singletonList(HandlePayload.class);
                    }

                    @Override
                    public void invokePreDestroy(HandlePayload instance) {
                        destroyCalls.incrementAndGet();
                    }
                }
        );
    }

    private static class HandlePayload {
        String value() {
            return "handle-payload";
        }
    }

    private Syringe createProgrammaticLookupSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ProgrammaticLookupConsumer.class);
        syringe.exclude(RawInstanceInjectionPointBean.class);
        syringe.exclude(BuiltInInstanceConsumer.class);
        syringe.exclude(ProgrammaticLookupIteratorConsumer.class);
        syringe.exclude(PlainIteratorCandidateA.class);
        syringe.exclude(PlainIteratorCandidateB.class);
        syringe.exclude(AlternativeIteratorCandidate.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private Syringe createBuiltInInstanceSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), BuiltInInstanceConsumer.class);
        syringe.exclude(RawInstanceInjectionPointBean.class);
        syringe.exclude(ProgrammaticLookupConsumer.class);
        syringe.exclude(ProgrammaticLookupIteratorConsumer.class);
        syringe.exclude(LiteralSelectionConsumer.class);
        syringe.exclude(PlainIteratorCandidateA.class);
        syringe.exclude(PlainIteratorCandidateB.class);
        syringe.exclude(AlternativeIteratorCandidate.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private Syringe createLiteralSelectionSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), LiteralSelectionConsumer.class);
        syringe.exclude(RawInstanceInjectionPointBean.class);
        syringe.exclude(ProgrammaticLookupConsumer.class);
        syringe.exclude(ProgrammaticLookupIteratorConsumer.class);
        syringe.exclude(BuiltInInstanceConsumer.class);
        syringe.exclude(CardGenericPaymentProcessor.class);
        syringe.exclude(PlainIteratorCandidateA.class);
        syringe.exclude(PlainIteratorCandidateB.class);
        syringe.exclude(AlternativeIteratorCandidate.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private Syringe createIteratorSyringeWithoutAlternatives() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ProgrammaticLookupIteratorConsumer.class);
        syringe.exclude(RawInstanceInjectionPointBean.class);
        syringe.exclude(ProgrammaticLookupConsumer.class);
        syringe.exclude(AlternativeIteratorCandidate.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private Syringe createIteratorSyringeWithAlternatives() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ProgrammaticLookupIteratorConsumer.class);
        syringe.exclude(RawInstanceInjectionPointBean.class);
        syringe.exclude(ProgrammaticLookupConsumer.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private void assertHasNestedLiteralClass(Class<? extends Annotation> annotationType) {
        Class<?>[] nestedClasses = annotationType.getDeclaredClasses();
        boolean found = false;
        for (Class<?> nested : nestedClasses) {
            if ("Literal".equals(nested.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected nested Literal class on " + annotationType.getName());
    }

    private void assertInstanceConstant(Class<?> literalClass, Class<? extends Annotation> annotationType) throws Exception {
        Field instanceField = literalClass.getField("INSTANCE");
        Object instance = instanceField.get(null);
        assertNotNull(instance);
        assertTrue(annotationType.isInstance(instance),
                "INSTANCE should implement " + annotationType.getName());
    }

}
