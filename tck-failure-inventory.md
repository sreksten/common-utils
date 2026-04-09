# TCK Failure Inventory

- Source: `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/testng-results.xml` and `/Users/stefano.reksten/Downloads/jakartacdi/cdi-tck-4.1.0/weld/jboss-tck-runner/target/surefire-reports/TestSuite.txt`
- Summary: Tests run: 1341, Failures: 10, Errors: 0, Skipped: 34
- Failed class entries: 10

## Top Causes
- 4x `jakarta.enterprise.inject.spi.DefinitionException: org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat: declares normal scope @RequestScoped and non-static public field(s) org.jboss.cdi.tck.tests.full.extensions.annotated.WildCat#publicName. Such beans must declare a pseudo-scope (e.g. @Dependent or @Singleton)."}}`
- 2x `jakarta.enterprise.inject.spi.DeploymentException: Container initialization failed`
- 2x `jakarta.enterprise.inject.spi.DeploymentException: Deployment validation failed. See log for details."}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.SyntheticBeanExtension.synthesize`
- 2x `java.lang.IllegalArgumentException: Cannot resolve class org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.MyEnum`
- 2x `java.lang.ClassNotFoundException: org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.MyEnum from [Module \"com.threeamigos.common.util\" from local module loader @4b41e4dd (finder: local module finder @22ffa91a (roots: /Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules,/Users/stefano.reksten/Downloads/jakartacdi/wildfly-31.0.1.Final/modules/system/layers/base))]"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Error invoking BCE phase method org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticObserver.SyntheticObserverExtension.synthesize`
- 2x `java.lang.IllegalArgumentException: Cannot instantiate qualifier org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticObserver.MyQualifier. Use qualifier(Annotation) or qualifier(AnnotationInfo).`
- 2x `java.lang.NoSuchMethodException: org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticObserver.MyQualifier.<init>()"}}`
- 2x `jakarta.enterprise.inject.spi.DefinitionException: Invalid BCE SYNTHESIS method org.jboss.cdi.tck.tests.full.extensions.lite.coexistence.StandardBuildCompatibleExtension.synthesis: expected SyntheticComponents and optional parameters from {BuildServices, Types, Messages}."}}`
- 1x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"ChangeBeanQualifierTestca32dd166497cfa5821f0234bc2da6f7e8b5ef.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"ChangeBeanQualifierTestca32dd166497cfa5821f0234bc2da6f7e8b5ef.war\"`
- 1x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"ChangeBeanQualifierTestca32dd166497cfa5821f0234bc2da6f7e8b5ef.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"ChangeBeanQualifierTestca32dd166497cfa5821f0234bc2da6f7e8b5ef.war\"`
- 1x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"SyntheticBeanTestb141a63e3b2fca12c1c71ac6cfdf022b776596.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"SyntheticBeanTestb141a63e3b2fca12c1c71ac6cfdf022b776596.war\"`
- 1x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"SyntheticBeanTestb141a63e3b2fca12c1c71ac6cfdf022b776596.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"SyntheticBeanTestb141a63e3b2fca12c1c71ac6cfdf022b776596.war\"`
- 1x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"SyntheticObserverTestd644cb3b4de89ccaa873efdaaf5416fac8860.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"SyntheticObserverTestd644cb3b4de89ccaa873efdaaf5416fac8860.war\"`
- 1x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"SyntheticObserverTestd644cb3b4de89ccaa873efdaaf5416fac8860.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"SyntheticObserverTestd644cb3b4de89ccaa873efdaaf5416fac8860.war\"`
- 1x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"AlternativeMetaDataTest53ce62d7759cc29e7291c182bb33edd6ae2643.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"AlternativeMetaDataTest53ce62d7759cc29e7291c182bb33edd6ae2643.war\"`
- 1x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"AlternativeMetaDataTest53ce62d7759cc29e7291c182bb33edd6ae2643.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"AlternativeMetaDataTest53ce62d7759cc29e7291c182bb33edd6ae2643.war\"`
- 1x `org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper$ServerDeploymentException: java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"ProcessAnnotatedTypeTest63e0df24618720567c20cccad9e7838edceb1347.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"ProcessAnnotatedTypeTest63e0df24618720567c20cccad9e7838edceb1347.war\"`
- 1x `java.lang.Exception: {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"ProcessAnnotatedTypeTest63e0df24618720567c20cccad9e7838edceb1347.war\".POST_MODULE" => "WFLYSRV0153: Failed to process phase POST_MODULE of deployment \"ProcessAnnotatedTypeTest63e0df24618720567c20cccad9e7838edceb1347.war\"`

## By Functional Area

### Extensions SPI & lifecycle (10)
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.customStereotype.CustomStereotypeTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.afterBeanDiscovery.annotated.GetAnnotatedTypesTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.producer.ProducerTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.annotated.AlternativeMetaDataTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticBean.SyntheticBeanTest**
- [ ] **org.jboss.cdi.tck.tests.build.compatible.extensions.syntheticObserver.SyntheticObserverTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.lite.coexistence.BuildCompatibleExtensionSmokeTest**
- [ ] **org.jboss.cdi.tck.tests.full.extensions.configurators.annotatedTypeConfigurator.beforeBeanDiscovery.AnnotatedTypeConfiguratorInBBDTest**
