# TCK Failure Inventory

- Source: `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/testng-results.xml` and `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/TestSuite.txt`
- Summary: Tests run: 3225, Failures: 589, Errors: 0, Skipped: 2636
- Failed class entries: 589

## Top Causes
- 10x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\"`
- 10x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"AbstractInvalidExtensionParamTest25d39bb27b2d53d727921af23845518216590dd.war\"`
- 3x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\"`
- 3x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"DecoratorWithNoDecoratedTypes1Teste740685d979e5dbce9d8658294dd5989589ffc2.war\"`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.ConstructorInterceptionTest6eda4b5a1dc5e2735766f3dee3ed7e9feebc29.war' @1a1879df)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.InvalidStereotypeInterceptorBindingAnnotationsTest33bc232eb6fd82bdd4587c14bc8b74b7db81282f.war' @303effaf)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.InvalidTransitiveInterceptorBindingAnnotationsTestca15895647fe8ec3785edaeb4fd0f5b07ad08a.war' @3a84e377)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.InterceptorBindingTypeWithMemberTest4cd3271048ad32393f7d579846ff2a77f6e6.war' @5fc4753a)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.MultipleInterceptorBindingsTestc1e51317779a2b34d5543887e4b9d5f0ad54f8be.war' @31322347)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.InterceptorBindingOverridingTest1da8a4f27490aa967d5d4665124c21bd7c5e03a.war' @5647124a)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.InterceptorBindingResolutionTest1e66bdf133ea7e5e1b2312a263b42370f9889524.war' @1deac4d7)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.AroundConstructTest24532c5aefe78fa5ce54a325bc97ae0c48638.war' @989d556)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.AroundConstructTest3b1268c6746335af3048425d34eb5e7c298d2c3c.war' @6c6f7ca9)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.AroundInvokeAccessInterceptorTestdf742f6f32126099ef54ff2e15b84f7bb9142823.war' @61f882c2)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.AroundInvokeInterceptorTestd8d5d346925eef1fc2a33cfa816a379a769b350.war' @4e1ad24c)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.ExceptionTestd574c30483d77dadd5f54c1781db32e5b8f56a.war' @54b62fb8)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.InterceptorLifeCycleTest7529b9262a5d146684def534f61464a89587a77e.war' @26d62175)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.AroundConstructLifeCycleTest96a098218fb0918f99d4837d4894b88c1ceb116d.war' @23928e1b)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.AroundConstructInterceptorReturnValueTest3f65bfc0992456e0de4f25ad1e9f3b3d8073c574.war' @9bf95d1)"}}`
- 2x `java.lang.IllegalAccessError: failed to access class org.jboss.as.server.mgmt.domain.HostControllerConnection from class org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler (org.jboss.as.server.mgmt.domain.HostControllerConnection is in unnamed module of loader 'org.jboss.as.server@23.0.3.Final' @18ef96; org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler is in unnamed module of loader 'deployment.SingleInterceptorInstanceTest4d34218e7fcb68badea652c18b6d70df602318.war' @9b66cbc)"}}`

## By Functional Area

### Core DI resolution & injection (139)
- [x] **org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array.ArrayTest**
- [x] **org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.privateConstructor.PrivateConstructorTest**
- [x] **org.jboss.cdi.tck.tests.lookup.dynamic.DynamicLookupTest**
- [x] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.SyntheticBeanWithLookupTest**
- [x] **org.jboss.cdi.tck.tests.invokers.lookup.BadArgumentLookupGreaterThanNumberOfParamsTest**
- [x] **org.jboss.cdi.tck.tests.invokers.basic.VarargsMethodInvokerTest**
- [x] **org.jboss.cdi.tck.tests.lookup.byname.broken.prefix.ExpandedNamePrefixTest**
- [x] **org.jboss.cdi.tck.tests.lookup.circular.CircularDependencyTest**

### Extensions SPI & lifecycle (119)
- [ ] **org.jboss.cdi.tck.tests.full.extensions.beanManager.el.WrapExpressionFactoryTest**
- [x] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.processBeanAttributes.broken.AddDefinitionErrorTest**
- [x] **org.jboss.cdi.tck.tests.full.extensions.beanManager.bean.SyntheticBeanTest**
- [x] **org.jboss.cdi.tck.tests.build.compatible.extensions.registration.RegistrationTest**
- [x] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.bbd.broken.normalScope.AddingNormalScopeTest**
- [x] **org.jboss.cdi.tck.tests.build.compatible.extensions.invalid.EnhancementMultipleParams2Test**
- [x] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.atd.AfterTypeDiscoveryTest**
- [x] **org.jboss.cdi.tck.tests.full.extensions.lifecycle.bbd.BeforeBeanDiscoveryTest**

### Producers/disposers/initializers (72)
- [x] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasDisposesParameter.ConstructorHasDisposesParameterTest**
- [x] **org.jboss.cdi.tck.tests.definition.bean.broken.restricted.RestrictedProducerMethodTest**
- [x] **org.jboss.cdi.tck.tests.implementation.producer.field.lifecycle.ProducerFieldLifecycleTest**
- [x] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.invocation.DisposesMethodCalledOnceTest**
- [x] **org.jboss.cdi.tck.tests.full.context.passivating.broken.producer.method.managed.NonPassivationCapableProducerMethodTest**
- [x] **org.jboss.cdi.tck.tests.implementation.producer.method.definition.ProducerMethodDefinitionTest**
- [x] **org.jboss.cdi.tck.tests.implementation.disposal.method.definition.broken.multiple.MultipleDisposerMethodsForProducerMethodTest**
- [x] **org.jboss.cdi.tck.tests.full.context.passivating.broken.producer.field.managed.dependent.ManagedBeanWithIllegalDependencyTest**

### Interceptors (68)
- [ ] **org.jboss.cdi.tck.interceptors.tests.order.lifecycleCallback.PostConstructOrderTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.interceptor.ManagedBeanWithNonSerializableInterceptorClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.interceptors.definition.interceptorNotListedInBeansXml.InterceptorNotListedInBeansXmlNotEnabledTest**
- [ ] **org.jboss.cdi.tck.interceptors.tests.contract.aroundInvoke.bindings.AroundInvokeInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.producer.field.definition.broken.interceptor.ProducerFieldOnInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.interceptor.field.PassivationCapableBeanWithNonPassivatingInterceptorTest**
- [ ] **org.jboss.cdi.tck.tests.interceptors.definition.inheritance.broken.binding.FinalMethodWithInheritedStereotypeInterceptorTest**
- [ ] **org.jboss.cdi.tck.interceptors.tests.contract.interceptorLifeCycle.aroundConstruct.returnValueIgnored.AroundConstructInterceptorReturnValueTest**

### Events/observers (59)
- [ ] **org.jboss.cdi.tck.tests.full.event.observer.extension.BeanManagerObserverNotificationTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.observer.inject.DeploymentFailureTest**
- [ ] **org.jboss.cdi.tck.tests.event.broken.observer.dependentIsConditionalObserver.DependentIsConditionalObserverTest**
- [ ] **org.jboss.cdi.tck.tests.event.observer.param.modification.SyncEventModificationTest**
- [ ] **org.jboss.cdi.tck.tests.event.EventTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.event.BuiltinEventDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.event.resolve.typeWithParameters.ChecksEventTypeWhenResolvingTest**
- [ ] **org.jboss.cdi.tck.tests.event.observer.method.ObserverMethodTest**

### Alternatives/stereotypes/specialization (35)
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.stereotype.priority.StereotypeWithAlternativeSelectedByPriorityTest**
- [ ] **org.jboss.cdi.tck.tests.definition.stereotype.named.DefaultNamedTest**
- [ ] **org.jboss.cdi.tck.tests.full.alternative.broken.incorrect.name.stereotype.NoAnnotationWithSpecifiedNameTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.qualifiers.SpecializingBeanQualifiersTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.inconsistent.InconsistentSpecializationTest**
- [ ] **org.jboss.cdi.tck.tests.definition.stereotype.broken.multiplePriorities.ConflictingPrioritiesFromSingleStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.full.inheritance.specialization.simple.broken.noextend3.SpecializingClassExtendsNonSimpleBeanTest**
- [ ] **org.jboss.cdi.tck.tests.alternative.selection.SelectedAlternative01Test**

### Contexts/scopes (33)
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.dependency.builtin.BuiltinBeanPassivationDependencyTest**
- [ ] **org.jboss.cdi.tck.tests.context.DestroyForSameCreationalContextTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.alterable.AlterableContextTest**
- [ ] **org.jboss.cdi.tck.tests.context.DestroyedInstanceReturnedByGetTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.ContextDestroysBeansTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.PassivatingContextTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.conversation.BuiltinConversationDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.context.ContextTest**

### Decorators (27)
- [ ] **org.jboss.cdi.tck.tests.full.decorators.custom.broken.finalBeanClass.CustomDecoratorMatchingBeanWithFinalClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.finalBeanClass.FinalBeanClassTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratoredBeanTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.builtin.instance.BuiltinInstanceDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratorTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.DecoratorDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.nonDependent.NonDependentDecoratorTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.typeparam.decorator.DecoratorTypeParamConstructorTest**

### Bean definition/typing (21)
- [ ] **org.jboss.cdi.tck.tests.definition.scope.inOtherBda.ScopeDefinedInOtherBDATest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.BeanDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.bean.genericbroken.GenericManagedBeanTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.constructorHasAsyncObservesParameter.ConstructorHasAsyncObservesParameterTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.normalScopedWithPublicField.NormalScopedWithPublicFieldTest**
- [ ] **org.jboss.cdi.tck.tests.definition.qualifier.builtin.BuiltInQualifierDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.simple.definition.SimpleBeanDefinitionTest**
- [ ] **org.jboss.cdi.tck.tests.definition.scope.ScopeDefinitionTest**

### Unclassified (16)
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.injection.decorated.DecoratedBeanConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.discovery.BeanDiscoveryTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.injection.intercepted.InterceptedBeanConstructorInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.implementation.builtin.metadata.broken.typeparam.BeanTypeParamFieldTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.broken.injection.decorated.DecoratedBeanFieldInjectionTest**
- [ ] **org.jboss.cdi.tck.tests.full.vetoed.VetoedTest**
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.BuiltinMetadataBeanTest**
- [ ] **org.jboss.cdi.tck.tests.full.deployment.trimmed.TrimmedBeanArchiveTest**
