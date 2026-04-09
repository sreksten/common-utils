# TCK Failure Inventory

- Source: `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/testng-results.xml` and `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/TestSuite.txt`
- Summary: Tests run: 1366, Failures: 29, Errors: 0, Skipped: 62
- Failed class entries: 29

## Top Causes
- 4x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat: declares normal scope @RequestScoped and non-static public field(s) org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat#publicName. Such beans must declare a pseudo-scope (e.g. @Dependent or @Singleton)."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.ChangeBeanQualifierExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.MyServiceFoo`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.changeBeanQualifier.MyServiceFoo from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.ChangeObserverQualifierExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.MyConsumer`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.changeObserverQualifier.MyConsumer from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.CustomQualifierExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.MyServiceFoo`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.customQualifier.MyServiceFoo from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.customStereotype.CustomStereotypeExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.customStereotype.MyService`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.customStereotype.MyService from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.inspectAnnotatedSubtypes.InspectAnnotatedSubtypesExtension.discovery`
- 2x `java.lang.IllegalArgumentException: Cannot add scanned class org.jboss.cdi.tck.tests.build.compatible.extensions.inspectAnnotatedSubtypes.MyServiceBaz`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.inspectAnnotatedSubtypes.MyServiceBaz from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.SyntheticBeanExtension.synthesize`
- 2x `java.lang.IllegalArgumentException: Cannot resolve class org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.MyEnum`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.MyEnum from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.SyntheticBeanWithLookupExtension.discovery`

## By Functional Area

### Extensions SPI & lifecycle (13)
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.inspectAnnotatedSubtypes.InspectAnnotatedSubtypesTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.customStereotype.CustomStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.afterBeanDiscovery.annotated.GetAnnotatedTypesTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.producer.ProducerTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.annotated.AlternativeMetaDataTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticObserver.SyntheticObserverTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lite.coexistence.BuildCompatibleExtensionSmokeTest**

### Unclassified (6)
- [ ] **org.jboss.cdi.tck.tests.full.implementation.builtin.metadata.BuiltinMetadataBeanTest**

### Core DI resolution & injection (3)
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBeanWithLookup.SyntheticBeanWithLookupTest**
- [ ] **org.jboss.cdi.tck.tests.lookup.injectionpoint.requiredtype.LegalRequiredTypeTest**
- [ ] **org.jboss.cdi.tck.tests.invokers.basic.GoodInstanceInvokerTest**

### Decorators (2)
- [ ] **org.jboss.cdi.tck.tests.full.decorators.definition.broken.nodecoratedtypes.DecoratorWithNoDecoratedTypes3Test**

### Contexts/scopes (1)
- [ ] **org.jboss.cdi.tck.tests.full.context.passivating.broken.decorator.field.DecoratorWithNonPassivatingInjectedFieldTest**
