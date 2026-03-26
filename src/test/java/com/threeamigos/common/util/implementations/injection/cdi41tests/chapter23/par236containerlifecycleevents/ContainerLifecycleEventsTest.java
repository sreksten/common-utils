package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter23.par236containerlifecycleevents;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.inject.Qualifier;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;
import jakarta.enterprise.inject.spi.ProcessProducer;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.interceptor.Interceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("23.6 - Container lifecycle events")
@Execution(ExecutionMode.SAME_THREAD)
public class ContainerLifecycleEventsTest {

    @Test
    @DisplayName("23.6 - Container lifecycle events are delivered synchronously to extension observers and extension instance is reused")
    void shouldDeliverContainerLifecycleEventsSynchronouslyToSingleExtensionInstance() {
        LifecycleEventRecorder.reset();

        long setupThreadId = Thread.currentThread().getId();
        Syringe syringe = newSyringe();
        syringe.addExtension(LifecycleRecordingExtension.class.getName());
        syringe.setup();

        assertTrue(LifecycleEventRecorder.events.contains("BeforeBeanDiscovery"));
        assertTrue(LifecycleEventRecorder.events.contains("ProcessAnnotatedType"));
        assertTrue(LifecycleEventRecorder.events.contains("AfterBeanDiscovery"));
        assertEquals(1, LifecycleEventRecorder.extensionInstanceIds.size());
        assertEquals(1, LifecycleEventRecorder.threadIds.size());
        assertTrue(LifecycleEventRecorder.threadIds.contains(setupThreadId));
        assertNotNull(LifecycleEventRecorder.lastBeanManager);
    }

    @Test
    @DisplayName("23.6 - Lifecycle observers declared on beans are not required to receive initialization events")
    void shouldNotRequireContainerLifecycleDeliveryToBeanObservers() {
        BeanLifecycleObserver.called = 0;

        Syringe syringe = newSyringe(BeanLifecycleObserver.class);
        syringe.setup();

        assertEquals(0, BeanLifecycleObserver.called);
    }

    @Test
    @DisplayName("23.6 - Calling lifecycle event object methods outside observer invocation throws IllegalStateException")
    void shouldRejectLifecycleEventObjectUsageOutsideObserverInvocation() {
        CapturedLifecycleEventExtension.reset();

        Syringe syringe = newSyringe();
        syringe.addExtension(CapturedLifecycleEventExtension.class.getName());
        syringe.setup();

        assertNotNull(CapturedLifecycleEventExtension.capturedBeforeBeanDiscovery);
        assertThrows(IllegalStateException.class, () ->
                CapturedLifecycleEventExtension.capturedBeforeBeanDiscovery.addQualifier(LateAddedQualifier.class));
    }

    @Test
    @DisplayName("23.6 - Injecting beans into extension observer method parameters is non-portable")
    void shouldRejectBeanInjectionIntoExtensionObserverMethodParameters() {
        Syringe syringe = newSyringe(SampleBean.class);
        syringe.addExtension(NonPortableInjectedBeanInExtensionObserver.class.getName());

        assertThrows(NonPortableBehaviourException.class, syringe::setup);
    }

    @Test
    @DisplayName("23.6 - Static extension observer methods for lifecycle events are non-portable")
    void shouldRejectStaticLifecycleObserverMethodOnExtension() {
        Syringe syringe = newSyringe();
        syringe.addExtension(StaticLifecycleObserverExtension.class.getName());

        assertThrows(NonPortableBehaviourException.class, syringe::setup);
    }

    @Test
    @DisplayName("23.6 - Static extension observer method for @Observes Object without qualifiers is non-portable")
    void shouldRejectStaticObjectObserverWithoutQualifierOnExtension() {
        Syringe syringe = newSyringe();
        syringe.addExtension(StaticObjectObserverWithoutQualifierExtension.class.getName());

        assertThrows(NonPortableBehaviourException.class, syringe::setup);
    }

    @Test
    @DisplayName("23.6 - Static extension observer method for @Observes @Any Object is non-portable")
    void shouldRejectStaticObjectObserverWithAnyQualifierOnExtension() {
        Syringe syringe = newSyringe();
        syringe.addExtension(StaticObjectObserverWithAnyQualifierExtension.class.getName());

        assertThrows(NonPortableBehaviourException.class, syringe::setup);
    }

    @Test
    @DisplayName("23.6 - Extension observer method notification order follows observer ordering and @Priority on observed parameter")
    void shouldOrderExtensionObserverMethodsByPriority() {
        PriorityOrderRecorder.reset();
        Syringe syringe = newSyringe();
        syringe.addExtension(PriorityOrderedObserverExtension.class.getName());
        syringe.setup();

        assertEquals(2, PriorityOrderRecorder.events.size());
        assertEquals("before-low", PriorityOrderRecorder.events.get(0));
        assertEquals("before-high", PriorityOrderRecorder.events.get(1));
    }

    @Test
    @DisplayName("23.6 - Each service provider has an @ApplicationScoped @Default bean exposing extension types")
    void shouldExposeExtensionServiceProviderAsApplicationScopedDefaultBean() {
        Syringe syringe = newSyringe();
        syringe.addExtension(InjectablePortableExtension.class.getName());
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        Set<Bean<?>> byClass = beanManager.getBeans(InjectablePortableExtension.class, Default.Literal.INSTANCE);
        Set<Bean<?>> byInterface = beanManager.getBeans(PortableExtensionContract.class, Default.Literal.INSTANCE);
        Set<Bean<?>> byBase = beanManager.getBeans(BasePortableExtension.class, Default.Literal.INSTANCE);

        assertTrue(byClass.size() >= 1);
        assertTrue(byInterface.size() >= 1);
        assertTrue(byBase.size() >= 1);

        Bean<?> extensionBean = null;
        for (Bean<?> candidate : byClass) {
            if (InjectablePortableExtension.class.equals(candidate.getBeanClass()) &&
                    ApplicationScoped.class.equals(candidate.getScope())) {
                extensionBean = candidate;
                break;
            }
        }
        assertNotNull(extensionBean);
        assertEquals(ApplicationScoped.class, extensionBean.getScope());
        assertTrue(extensionBean.getTypes().contains(InjectablePortableExtension.class));
        assertTrue(extensionBean.getTypes().contains(PortableExtensionContract.class));
        assertTrue(extensionBean.getTypes().contains(BasePortableExtension.class));
        assertTrue(extensionBean.getTypes().contains(Extension.class));
        assertTrue(extensionBean.getQualifiers().stream()
                .anyMatch(annotation -> annotation.annotationType().equals(Default.class)));
    }

    @Test
    @DisplayName("23.6 - Service provider bean supports obtaining an injectable reference to the extension instance")
    void shouldProvideInjectableReferenceForExtensionServiceProviderBean() {
        Syringe syringe = newSyringe();
        syringe.addExtension(InjectablePortableExtension.class.getName());
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        Set<Bean<?>> candidates = beanManager.getBeans(
                InjectablePortableExtension.class,
                Default.Literal.INSTANCE
        );
        Bean<?> extensionBean = null;
        for (Bean<?> candidate : candidates) {
            if (InjectablePortableExtension.class.equals(candidate.getBeanClass()) &&
                    ApplicationScoped.class.equals(candidate.getScope())) {
                extensionBean = candidate;
                break;
            }
        }
        assertNotNull(extensionBean);

        CreationalContext<?> context = beanManager.createCreationalContext(extensionBean);
        Object reference = beanManager.getReference(extensionBean, InjectablePortableExtension.class, context);

        assertNotNull(reference);
        assertTrue(reference instanceof InjectablePortableExtension);
    }

    @Test
    @DisplayName("23.6 - Application lifecycle events fire once and bean discovery events may fire multiple times in chronological order")
    void shouldFireLifecycleEventCategoriesInExpectedOrder() {
        LifecycleCategoryRecorder.reset();
        Syringe syringe = newSyringe(DiscoveryPhaseFixture.class, DependencyForDiscoveryPhase.class);
        syringe.addExtension(LifecycleCategoryObserverExtension.class.getName());
        syringe.setup();
        syringe.shutdown();

        assertEquals(1, LifecycleCategoryRecorder.applicationCount("BeforeBeanDiscovery"));
        assertEquals(1, LifecycleCategoryRecorder.applicationCount("AfterTypeDiscovery"));
        assertEquals(1, LifecycleCategoryRecorder.applicationCount("AfterBeanDiscovery"));
        assertEquals(1, LifecycleCategoryRecorder.applicationCount("AfterDeploymentValidation"));
        assertEquals(1, LifecycleCategoryRecorder.applicationCount("BeforeShutdown"));

        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessAnnotatedType") >= 1);
        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessInjectionPoint") >= 1);
        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessInjectionTarget") >= 1);
        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessBeanAttributes") >= 1);
        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessBean") >= 1);
        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessProducer") >= 1);
        assertTrue(LifecycleCategoryRecorder.discoveryCount("ProcessObserverMethod") >= 1);

        assertTrue(LifecycleCategoryRecorder.indexOf("BeforeBeanDiscovery")
                < LifecycleCategoryRecorder.indexOf("AfterTypeDiscovery"));
        assertTrue(LifecycleCategoryRecorder.indexOf("AfterTypeDiscovery")
                < LifecycleCategoryRecorder.indexOf("AfterBeanDiscovery"));
        assertTrue(LifecycleCategoryRecorder.indexOf("AfterBeanDiscovery")
                < LifecycleCategoryRecorder.indexOf("AfterDeploymentValidation"));
        assertTrue(LifecycleCategoryRecorder.indexOf("AfterDeploymentValidation")
                < LifecycleCategoryRecorder.indexOf("BeforeShutdown"));
    }

    @Test
    @DisplayName("23.6 - Build compatible extensions are executed during container lifecycle phases")
    void shouldExecuteBuildCompatibleExtensionsAlongsideLifecycleEvents() {
        BuildCompatibleLifecycleRecorder.reset();
        Syringe syringe = newSyringe();
        syringe.addBuildCompatibleExtension(LifecycleTrackingBuildCompatibleExtension.class.getName());
        syringe.setup();

        assertEquals(
                asList("DISCOVERY", "ENHANCEMENT", "REGISTRATION", "SYNTHESIS", "REGISTRATION", "VALIDATION"),
                BuildCompatibleLifecycleRecorder.phases
        );
    }

    @Test
    @DisplayName("23.6 - @SkipIfPortableExtensionPresent build compatible extension is ignored in CDI Full when portable extension is present")
    void shouldSkipBuildCompatibleExtensionWhenPortableExtensionIsPresent() {
        BuildCompatibleLifecycleRecorder.reset();
        Syringe syringe = newSyringe();
        syringe.addExtension(PortablePresenceExtension.class.getName());
        syringe.addBuildCompatibleExtension(SkipWhenPortablePresentBuildCompatibleExtension.class.getName());
        syringe.setup();

        assertEquals(Collections.emptyList(), BuildCompatibleLifecycleRecorder.phases);
    }

    private Syringe newSyringe(Class<?>... classes) {
        Class<?>[] effectiveClasses = classes;
        if (effectiveClasses == null || effectiveClasses.length == 0) {
            effectiveClasses = new Class<?>[]{LifecycleMarkerBean.class};
        }
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), effectiveClasses);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    public static class LifecycleRecordingExtension implements Extension {
        public void before(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
            LifecycleEventRecorder.record("BeforeBeanDiscovery", this, beanManager);
        }

        public void process(@Observes ProcessAnnotatedType<?> event, BeanManager beanManager) {
            LifecycleEventRecorder.record("ProcessAnnotatedType", this, beanManager);
        }

        public void after(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
            LifecycleEventRecorder.record("AfterBeanDiscovery", this, beanManager);
        }
    }

    @Dependent
    public static class BeanLifecycleObserver {
        static int called = 0;

        void before(@Observes BeforeBeanDiscovery ignored) {
            called++;
        }
    }

    public static class CapturedLifecycleEventExtension implements Extension {
        static BeforeBeanDiscovery capturedBeforeBeanDiscovery;

        static void reset() {
            capturedBeforeBeanDiscovery = null;
        }

        public void before(@Observes BeforeBeanDiscovery event) {
            capturedBeforeBeanDiscovery = event;
        }
    }

    public static class NonPortableInjectedBeanInExtensionObserver implements Extension {
        public void before(@Observes BeforeBeanDiscovery event, BeanManager beanManager, SampleBean bean) {
            // Non-portable by spec: extension observers should not receive injected beans besides BeanManager.
        }
    }

    @Dependent
    public static class SampleBean {
    }

    @Dependent
    public static class LifecycleMarkerBean {
    }

    public static class StaticLifecycleObserverExtension implements Extension {
        public static void before(@Observes BeforeBeanDiscovery event) {
        }
    }

    public static class StaticObjectObserverWithoutQualifierExtension implements Extension {
        public static void observe(@Observes Object event) {
        }
    }

    public static class StaticObjectObserverWithAnyQualifierExtension implements Extension {
        public static void observe(@Observes @Any Object event) {
        }
    }

    public static class PriorityOrderedObserverExtension implements Extension {
        public void first(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE + 10) BeforeBeanDiscovery event) {
            PriorityOrderRecorder.events.add("before-low");
        }

        public void second(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE + 100) BeforeBeanDiscovery event) {
            PriorityOrderRecorder.events.add("before-high");
        }
    }

    public interface PortableExtensionContract {
    }

    public static class BasePortableExtension implements Extension {
    }

    public static class InjectablePortableExtension extends BasePortableExtension implements PortableExtensionContract {
    }

    @Dependent
    public static class DependencyForDiscoveryPhase {
    }

    @Dependent
    public static class DiscoveryPhaseFixture {
        @jakarta.inject.Inject
        DependencyForDiscoveryPhase fieldInjection;

        @jakarta.inject.Inject
        void init(DependencyForDiscoveryPhase dependency) {
        }

        @jakarta.enterprise.inject.Produces
        String producer() {
            return "discovery";
        }

        void observe(@Observes String payload) {
            // force observer discovery
        }
    }

    public static class LifecycleCategoryObserverExtension implements Extension {
        public void before(@Observes BeforeBeanDiscovery event) {
            LifecycleCategoryRecorder.recordApplication("BeforeBeanDiscovery");
        }

        public void afterType(@Observes AfterTypeDiscovery event) {
            LifecycleCategoryRecorder.recordApplication("AfterTypeDiscovery");
        }

        public void afterBean(@Observes AfterBeanDiscovery event) {
            LifecycleCategoryRecorder.recordApplication("AfterBeanDiscovery");
        }

        public void afterDeployment(@Observes AfterDeploymentValidation event) {
            LifecycleCategoryRecorder.recordApplication("AfterDeploymentValidation");
        }

        public void beforeShutdown(@Observes BeforeShutdown event) {
            LifecycleCategoryRecorder.recordApplication("BeforeShutdown");
        }

        public void pat(@Observes ProcessAnnotatedType<?> event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessAnnotatedType");
        }

        public void pip(@Observes ProcessInjectionPoint<?, ?> event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessInjectionPoint");
        }

        public void pit(@Observes ProcessInjectionTarget<?> event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessInjectionTarget");
        }

        public void pba(@Observes ProcessBeanAttributes<?> event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessBeanAttributes");
        }

        public void pb(@Observes ProcessBean<?> event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessBean");
        }

        public void pp(@Observes ProcessProducer<?, ?> event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessProducer");
        }

        public void pom(@Observes ProcessObserverMethod event) {
            LifecycleCategoryRecorder.recordDiscovery("ProcessObserverMethod");
        }
    }

    public static class LifecycleTrackingBuildCompatibleExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(Types types) {
            BuildCompatibleLifecycleRecorder.phases.add("DISCOVERY");
        }

        @Enhancement(types = LifecycleMarkerBean.class, withSubtypes = false)
        public void enhancement(jakarta.enterprise.inject.build.compatible.spi.ClassConfig classConfig) {
            BuildCompatibleLifecycleRecorder.phases.add("ENHANCEMENT");
        }

        @Registration(types = Object.class)
        public void registration() {
            BuildCompatibleLifecycleRecorder.phases.add("REGISTRATION");
        }

        @Synthesis
        public void synthesis(jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents syntheticComponents) {
            BuildCompatibleLifecycleRecorder.phases.add("SYNTHESIS");
        }

        @Validation
        public void validation() {
            BuildCompatibleLifecycleRecorder.phases.add("VALIDATION");
        }
    }

    public static class PortablePresenceExtension implements Extension {
    }

    @SkipIfPortableExtensionPresent(PortablePresenceExtension.class)
    public static class SkipWhenPortablePresentBuildCompatibleExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(Types types) {
            BuildCompatibleLifecycleRecorder.phases.add("DISCOVERY");
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LateAddedQualifier {
    }

    private static class LifecycleEventRecorder {
        private static final List<String> events = new ArrayList<String>();
        private static final Set<Long> threadIds = new HashSet<Long>();
        private static final Set<Integer> extensionInstanceIds = new HashSet<Integer>();
        private static BeanManager lastBeanManager;

        static synchronized void reset() {
            events.clear();
            threadIds.clear();
            extensionInstanceIds.clear();
            lastBeanManager = null;
        }

        static synchronized void record(String eventName, Object extension, BeanManager beanManager) {
            events.add(eventName);
            threadIds.add(Thread.currentThread().getId());
            extensionInstanceIds.add(System.identityHashCode(extension));
            lastBeanManager = beanManager;
        }
    }

    private static class PriorityOrderRecorder {
        private static final List<String> events = new ArrayList<String>();

        static synchronized void reset() {
            events.clear();
        }
    }

    private static class LifecycleCategoryRecorder {
        private static final List<String> chronology = new ArrayList<String>();
        private static final java.util.Map<String, Integer> appCounters = new java.util.HashMap<String, Integer>();
        private static final java.util.Map<String, Integer> discoveryCounters = new java.util.HashMap<String, Integer>();

        static synchronized void reset() {
            chronology.clear();
            appCounters.clear();
            discoveryCounters.clear();
        }

        static synchronized void recordApplication(String eventName) {
            chronology.add(eventName);
            Integer current = appCounters.get(eventName);
            appCounters.put(eventName, current == null ? 1 : current + 1);
        }

        static synchronized void recordDiscovery(String eventName) {
            Integer current = discoveryCounters.get(eventName);
            discoveryCounters.put(eventName, current == null ? 1 : current + 1);
        }

        static synchronized int applicationCount(String eventName) {
            Integer count = appCounters.get(eventName);
            return count == null ? 0 : count;
        }

        static synchronized int discoveryCount(String eventName) {
            Integer count = discoveryCounters.get(eventName);
            return count == null ? 0 : count;
        }

        static synchronized int indexOf(String eventName) {
            return chronology.indexOf(eventName);
        }
    }

    private static class BuildCompatibleLifecycleRecorder {
        private static final List<String> phases = new ArrayList<String>();

        static synchronized void reset() {
            phases.clear();
        }
    }

    private static List<String> asList(String... values) {
        List<String> out = new ArrayList<String>();
        for (String value : values) {
            out.add(value);
        }
        return out;
    }
}
