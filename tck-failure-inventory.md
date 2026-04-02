# TCK Failure Inventory

- Source: `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/testng-results.xml` and `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/TestSuite.txt`
- Summary: Tests run: 1669, Failures: 392, Errors: 0, Skipped: 462
- Failed class entries: 392

## Top Causes
- 27x `jakarta.enterprise.inject.spi.DefinitionException: Deployment validation failed. See log for details."}}`
- 16x `jakarta.enterprise.inject.spi.DeploymentException: Container initialization failed`
- 16x `jakarta.enterprise.inject.spi.DeploymentException: Deployment validation failed. See log for details."}}`
- 10x `org.testng.TestException: `
- 3x `java.lang.reflect.InvocationTargetException`
- 2x `java.lang.RuntimeException`
- 2x `com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException: Asynchronous observer method observes in org.jboss.cdi.tck.tests.event.observer.async.basic.MixedObservers$MassachusettsInstituteObserver declares @Priority on observed event parameter; this is non-portable behavior"}}`
- 2x `com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException: org.jboss.cdi.tck.tests.full.decorators.definition.broken.nonDependent.FooServiceDecorator: decorator declares scope @RequestScoped. Decorators with any scope other than @Dependent are non-portable."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat: declares normal scope @RequestScoped and non-static public field(s) org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat#publicName. Such beans must declare a pseudo-scope (e.g. @Dependent or @Singleton)."}}`
- 2x `com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException: org.jboss.cdi.tck.tests.interceptors.definition.broken.nonDependent.NonDependentInterceptor: interceptor declares scope @RequestScoped. Interceptors with any scope other than @Dependent are non-portable."}}`
- 1x `java.lang.AssertionError: Interceptor1 id == 7 expected [7] but found [8]`
- 1x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.ChangeBeanQualifierExtension.discovery`
- 1x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.MyServiceFoo`
- 1x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.MyServiceFoo from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 1x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.ChangeObserverQualifierExtension.discovery`
- 1x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.MyConsumer`
- 1x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.MyConsumer from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 1x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.CustomQualifierExtension.discovery`
- 1x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.MyServiceFoo`
- 1x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.MyServiceFoo from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`

## By Functional Area

### Extensions SPI & lifecycle (150)
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.el.WrapExpressionFactoryTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.processBeanAttributes.broken.AddDefinitionErrorTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**

### Core DI resolution & injection (88)
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.SyntheticBeanWithLookupTest**
- [ ] **org.jboss.cdi.tck.tests.invokers.lookup.BadArgumentLookupGreaterThanNumberOfParamsTest**
- [ ] **org.jboss.cdi.tck.tests.invokers.basic.VarargsMethodInvokerTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.ExpandedNamePrefixTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.ExpandedNamePrefix2Test**
- [ ] **org.jboss.cdi.tck.tests.full.lookup.dynamic.broken.raw.RawInstanceCustomBeanTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.injectionpoint.InjectionPointTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.NamePrefixTest**

### Contexts/scopes (38)
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.context.DestroyForSameCreationalContextTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.alterable.AlterableContextTest**
- [ ] **org.jboss.cdi.tck.tests.context.DestroyedInstanceReturnedByGetTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.ContextDestroysBeansTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.PassivatingContextTest**

### Interceptors (27)
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.interceptorNotListedInBeansXml.InterceptorNotListedInBeansXmlNotEnabledTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.interceptor.field.PassivationCapableBeanWithNonPassivatingInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.interceptorCalledBeforeDecorator.InterceptorCalledBeforeDecoratorTest**
- [ ] **org.jboss.cdi.tck.interceptors.tests.contract.lifecycleCallback.bindings.LifecycleInterceptorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.inheritance.broken.binding.FinalClassWithInheritedStereotypeInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.validation.InterceptorWithNonPassivationCapableDependenciesTest**
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.broken.nonExistantClassInBeansXml.NonExistantClassInBeansXmlTest**
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.conflictingenablement.InterceptorConflictingEnablementTest**

### Decorators (18)
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.finalBeanClass.FinalBeanClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.instance.BuiltinInstanceDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.instance.BuiltinInstanceDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**

### Unclassified (17)
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.vetoed.VetoedTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.BuiltinMetadataBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.BuiltinMetadataBeanTest**

### Events/observers (17)
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.broken.observer.InterceptorWithObserverMethodTest**
- [ ] **org.jboss.cdi.tck.tests.event.observer.broken.validation.unsatisfied.ObserverMethodParameterInjectionValidationTest**
- [ ] **org.jboss.cdi.tck.tests.event.implicit.ImplicitEventTest**
- [ ] **org.jboss.cdi.tck.tests.event.observer.async.basic.MixedObserversTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.broken.observer.async.InterceptorWithAsyncObserverMethodTest**

### Bean definition/typing (14)
- [ ] **org.jboss.cdi.tck.tests.definition.bean.BeanDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.SimpleBeanDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.SimpleBeanDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.types.illegal.BeanTypesWithIllegalTypeTest**

### Alternatives/stereotypes/specialization (12)
- [ ] **org.jboss.cdi.tck.tests.full.alternative.broken.incorrect.name.stereotype.NoAnnotationWithSpecifiedNameTest**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative01Test**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative01Test**
- [ ] **org.jboss.cdi.tck.tests.full.alternative.selection.stereotype.SelectedBeanWithUnselectedStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative02Test**
- [ ] **org.jboss.cdi.tck.tests.full.alternative.veto.VetoedAlternativeTest**
- [ ] **org.jboss.cdi.tck.tests.full.alternative.broken.same.type.twice.SameTypeListedTwiceTest**
- [ ] **org.jboss.cdi.tck.tests.full.alternative.AlternativeAvailabilityTest**

### Producers/disposers/initializers (11)
- [ ] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.invocation.DisposesMethodCalledOnceTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.method.definition.ProducerMethodDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.producer.method.broken.name.SpecializingAndSpecializedBeanHaveNameTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.producer.DecoratorNotAppliedToResultOfProducerTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.producer.DecoratorNotAppliedToResultOfProducerTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.producer.method.ProducerMethodSpecializationTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.producer.method.ProducerMethodSpecializationTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.DisposalMethodDefinitionTest**
