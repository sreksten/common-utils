# TCK Failure Inventory

- Source: `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/testng-results.xml` and `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/TestSuite.txt`
- Summary: Tests run: 1868, Failures: 282, Errors: 0, Skipped: 564
- Failed class entries: 282

## Top Causes
- 308x `jakarta.enterprise.inject.spi.DefinitionException: Deployment validation failed. See log for details."}}`
- 118x `jakarta.enterprise.inject.spi.DeploymentException: Container initialization failed`
- 110x `jakarta.enterprise.inject.spi.DeploymentException: Deployment validation failed. See log for details."}}`
- 10x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\"`
- 10x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\"`
- 8x `java.lang.RuntimeException"}}`
- 4x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat: declares normal scope @RequestScoped and non-static public field(s) org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat#publicName. Such beans must declare a pseudo-scope (e.g. @Dependent or @Singleton)."}}`
- 4x `jakarta.enterprise.inject.spi.DeploymentException: Deployment validation failed due to AfterDeploymentValidation problems."}}`
- 3x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\"`
- 3x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\"`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.interceptors.tests.bindings.broken.Bar: conflicting interceptor binding values for @BazBinding"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.interceptors.tests.bindings.broken.Foo: conflicting interceptor binding values for @BazBinding"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.ChangeBeanQualifierExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.MyServiceFoo`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.MyServiceFoo from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.ChangeObserverQualifierExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.MyConsumer`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.MyConsumer from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.CustomQualifierExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.MyServiceFoo`

## By Functional Area

### Extensions SPI & lifecycle (62)
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.processBeanAttributes.broken.AddDefinitionErrorTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.bbd.broken.normalScope.AddingNormalScopeTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.invalid.EnhancementMultipleParams2Test**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.atd.AfterTypeDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.atd.AfterTypeDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.atd.AfterTypeDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.atd.AfterTypeDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.bbd.BeforeBeanDiscoveryTest**

### Core DI resolution & injection (61)
- [ ] **org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array.ArrayTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.privateConstructor.PrivateConstructorTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.SyntheticBeanWithLookupTest**
- [ ] **org.jboss.cdi.tck.tests.invokers.lookup.BadArgumentLookupGreaterThanNumberOfParamsTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.ExpandedNamePrefixTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.ExpandedNamePrefix2Test**
- [ ] **org.jboss.cdi.tck.tests.full.lookup.dynamic.broken.raw.RawInstanceCustomBeanTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.injectionpoint.broken.normal.scope.NormalScopedBeanWithInjectionPoint**

### Producers/disposers/initializers (51)
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasDisposesParameter.ConstructorHasDisposesParameterTest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.broken.restricted.RestrictedProducerMethodTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.producer.method.managed.NonPassivationCapableProducerMethodTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.broken.multiple.MultipleDisposerMethodsForProducerMethodTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.initializer.broken.methodAnnotatedProduces.InitializerMethodAnnotatedProducesTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.method.broken.array.ProducerMethodArrayTypeVariableTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.method.broken.parameterizedTypeWithTypeParameter.ParametrizedReturnTypeWithTypeVariable02Test**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.disposal.method.definition.broken.decorator.DisposerMethodOnDecoratorTest**

### Interceptors (27)
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.interceptor.ManagedBeanWithNonSerializableInterceptorClassTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.field.definition.broken.interceptor.ProducerFieldOnInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.interceptor.field.PassivationCapableBeanWithNonPassivatingInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.inheritance.broken.binding.FinalMethodWithInheritedStereotypeInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.broken.finalClassInterceptor.NormalScopedBeanFinalClassInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.broken.finalClassInterceptor.DependentBeanFinalMethodInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.injection.BuiltinInterceptorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.inheritance.broken.binding.FinalClassWithInheritedStereotypeInterceptorTest**

### Events/observers (22)
- [ ] **org.jboss.cdi.tck.tests.event.broken.observer.inject.DeploymentFailureTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.observer.dependentIsConditionalObserver.DependentIsConditionalObserverTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.raw.RawEventConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.raw.RawEventInitMethodInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.broken.observer.InterceptorWithObserverMethodTest**
- [ ] **org.jboss.cdi.tck.tests.event.observer.broken.validation.unsatisfied.ObserverMethodParameterInjectionValidationTest**
- [ ] **org.jboss.cdi.tck.tests.event.observer.async.basic.MixedObserversTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.raw.RawEventDisposerInjectionTest**

### Decorators (20)
- [ ] **org.jboss.cdi.tck.tests.full.decorators.custom.broken.finalBeanClass.CustomDecoratorMatchingBeanWithFinalClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.finalBeanClass.FinalBeanClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratoredBeanTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratorTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.nonDependent.NonDependentDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratorTypeParamConstructorTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.notAllDecoratedTypesImplemented.parameterized.TypeParametersNotTheSameTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.nonDecoratorWithDecorates.NonDecoratorWithDecoratesTest**

### Alternatives/stereotypes/specialization (18)
- [ ] **org.jboss.cdi.tck.tests.full.alternative.broken.incorrect.name.stereotype.NoAnnotationWithSpecifiedNameTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.inconsistent.InconsistentSpecializationTest**
- [ ] **org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.ConflictingPrioritiesFromSingleStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.noextend3.SpecializingClassExtendsNonSimpleBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.names.SpecializingAndSpecializedBeanHasNameTest**
- [ ] **org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.ConflictingPriorityStereotypesTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.noextend2.SpecializingBeanExtendsNothingTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.types.SpecializingBeanWithoutBeanTypeOfSpecializedBeanTest**

### Bean definition/typing (9)
- [ ] **org.jboss.cdi.tck.tests.definition.bean.genericbroken.GenericManagedBeanTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasAsyncObservesParameter.ConstructorHasAsyncObservesParameterTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.normalScopedWithPublicField.NormalScopedWithPublicFieldTest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.broken.restricted.RestrictedManagedBeanTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasObservesParameter.ConstructorHasObservesParameterTest**
- [ ] **org.jboss.cdi.tck.tests.definition.scope.broken.tooManyScopes.TooManyScopesTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.broken.field.InjectedFieldAnnotatedWithProducesTest**
- [ ] **org.jboss.cdi.tck.tests.inheritance.generics.MemberLevelInheritanceTest**

### Unclassified (7)
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.injection.decorated.DecoratedBeanConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.injection.intercepted.InterceptedBeanConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.typeparam.BeanTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.injection.decorated.DecoratedBeanFieldInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.BuiltinMetadataBeanTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.injection.intercepted.InterceptedBeanFieldInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.typeparam.BeanTypeParamConstructorTest**

### Contexts/scopes (5)
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.NonPassivationManagedBeanHasPassivatingScopeTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.constructor.NonPassivatingConstructorParamTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.field.NonPassivatingInjectedFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.decorator.ManagedBeanWithNonPassivatingDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.decorator.field.DecoratorWithNonPassivatingInjectedFieldTest**
