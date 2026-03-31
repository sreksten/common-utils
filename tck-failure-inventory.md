# TCK Failure Inventory

- Source: `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/testng-results.xml` and `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/TestSuite.txt`
- Summary: Tests run: 1943, Failures: 719, Errors: 0, Skipped: 748
- Failed class entries: 719

## Top Causes
- 328x `jakarta.enterprise.inject.spi.DeploymentException: Failed to bootstrap Syringe`
- 262x `jakarta.enterprise.inject.spi.DefinitionException: Deployment validation failed. See log for details."}}`
- 36x `jakarta.enterprise.inject.spi.DeploymentException: Container initialization failed`
- 30x `jakarta.enterprise.inject.spi.DeploymentException: Deployment validation failed. See log for details."}}`
- 20x `org.testng.TestException: `
- 6x `java.lang.IllegalStateException: Unable to access CDI"}}`
- 4x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat: declares normal scope @RequestScoped and non-static public field(s) org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat#publicName. Such beans must declare a pseudo-scope (e.g. @Dependent or @Singleton)."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.interceptors.tests.bindings.broken.Bar: conflicting interceptor binding values for @BazBinding"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.interceptors.tests.bindings.broken.Foo: conflicting interceptor binding values for @BazBinding"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.bean.genericbroken.FooBroken: managed bean class is generic and declares scope @RequestScoped. Generic managed beans must have @Dependent scope."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.SomeOtherBean: stereotypes declare different @Priority values (200, 100). Bean must explicitly declare @Priority."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.SomeBean: stereotypes declare different @Priority values (100, 200). Bean must explicitly declare @Priority."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.inherited.FooAlternative: stereotypes declare different @Priority values (100, 200). Bean must explicitly declare @Priority."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.stereotype.broken.nonEmptyNamed.StereotypeWithNonEmptyNamed_Broken: stereotype declares non-empty @Named(\"foo\")"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.stereotype.broken.scopeConflict.transitive.AnimalStereotype: stereotype declares multiple scopes: @ApplicationScoped, @RequestScoped"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.definition.stereotype.broken.tooManyScopes.StereotypeWithTooManyScopeTypes_Broken: stereotype declares multiple scopes: @ApplicationScoped, @RequestScoped"}}`
- 2x `com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException: Asynchronous observer method observes in org.jboss.cdi.tck.tests.event.observer.async.basic.MixedObservers$MassachusettsInstituteObserver declares @Priority on observed event parameter; this is non-portable behavior"}}`
- 2x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\"`
- 2x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\"`
- 2x `com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException: org.jboss.cdi.tck.tests.full.decorators.definition.broken.nonDependent.FooServiceDecorator: decorator declares scope @RequestScoped. Decorators with any scope other than @Dependent are non-portable."}}`

## By Functional Area

### Extensions SPI & lifecycle (192)
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.el.WrapExpressionFactoryTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.processBeanAttributes.broken.AddDefinitionErrorTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**

### Core DI resolution & injection (150)
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.SyntheticBeanWithLookupTest**
- [ ] **org.jboss.cdi.tck.tests.invokers.lookup.BadArgumentLookupGreaterThanNumberOfParamsTest**
- [ ] **org.jboss.cdi.tck.tests.invokers.basic.VarargsMethodInvokerTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.ExpandedNamePrefix2Test**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.injectionPoint.CreateInjectionPointTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.injectionPoint.CreateInjectionPointTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.injectionPoint.CreateInjectionPointTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.injectionPoint.CreateInjectionPointTest**

### Events/observers (77)
- [ ] **org.jboss.cdi.tck.tests.full.event.observer.extension.BeanManagerObserverNotificationTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.observer.inject.DeploymentFailureTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.observer.dependentIsConditionalObserver.DependentIsConditionalObserverTest**
- [ ] **org.jboss.cdi.tck.tests.event.EventTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.event.fires.sync.FireSyncEventTest**

### Producers/disposers/initializers (66)
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasDisposesParameter.ConstructorHasDisposesParameterTest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.broken.restricted.RestrictedProducerMethodTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.field.lifecycle.ProducerFieldLifecycleTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.invocation.DisposesMethodCalledOnceTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.method.definition.ProducerMethodDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.broken.multiple.MultipleDisposerMethodsForProducerMethodTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.producer.field.managed.dependent.ManagedBeanWithIllegalDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.producer.field.managed.dependent.ManagedBeanWithIllegalDependencyTest**

### Interceptors (62)
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.interceptorNotListedInBeansXml.InterceptorNotListedInBeansXmlNotEnabledTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.field.definition.broken.interceptor.ProducerFieldOnInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.interceptor.field.PassivationCapableBeanWithNonPassivatingInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.contract.invocationContext.InterceptorBindingsWithAtInterceptorsTest**
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.interceptorCalledBeforeDecorator.InterceptorCalledBeforeDecoratorTest**
- [ ] **org.jboss.cdi.tck.interceptors.tests.contract.lifecycleCallback.bindings.LifecycleInterceptorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.injection.BuiltinInterceptorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.inheritance.broken.binding.FinalClassWithInheritedStereotypeInterceptorTest**

### Contexts/scopes (48)
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.context.DestroyForSameCreationalContextTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.alterable.AlterableContextTest**
- [ ] **org.jboss.cdi.tck.tests.context.DestroyedInstanceReturnedByGetTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.ContextDestroysBeansTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.PassivatingContextTest**

### Bean definition/typing (40)
- [ ] **org.jboss.cdi.tck.tests.definition.bean.BeanDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.genericbroken.GenericManagedBeanTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasAsyncObservesParameter.ConstructorHasAsyncObservesParameterTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.normalScopedWithPublicField.NormalScopedWithPublicFieldTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**

### Decorators (30)
- [ ] **org.jboss.cdi.tck.tests.full.decorators.custom.broken.finalBeanClass.CustomDecoratorMatchingBeanWithFinalClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.finalBeanClass.FinalBeanClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratoredBeanTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.instance.BuiltinInstanceDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.instance.BuiltinInstanceDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratorTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**

### Unclassified (30)
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.injection.decorated.DecoratedBeanConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.injection.intercepted.InterceptedBeanConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.typeparam.BeanTypeParamFieldTest**

### Alternatives/stereotypes/specialization (24)
- [ ] **org.jboss.cdi.tck.tests.full.alternative.broken.incorrect.name.stereotype.NoAnnotationWithSpecifiedNameTest**
- [ ] **org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.ConflictingPrioritiesFromSingleStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.noextend3.SpecializingClassExtendsNonSimpleBeanTest**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative01Test**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative01Test**
- [ ] **org.jboss.cdi.tck.tests.full.alternative.selection.stereotype.SelectedBeanWithUnselectedStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative02Test**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.names.SpecializingAndSpecializedBeanHasNameTest**
