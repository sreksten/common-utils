# Syringe to WildFly Integration Roadmap

This document outlines the architectural and implementation steps required to integrate **Syringe** as a primary CDI container within **WildFly**.

## 1. Modularization (WildFly Modules)
WildFly uses a JBoss Modules-based ClassLoading system. Syringe must be packaged as a system module.

- **Objective:** Allow WildFly to load Syringe without bundling it in every WAR/EAR.
- **Steps:**
    1. Create a directory: `${WILDFLY_HOME}/modules/system/layers/base/com/threeamigos/common/util/main/`
    2. Add `common-utils.jar` (containing Syringe) to this directory.
    3. Create a `module.xml`:
       ```xml
       <module xmlns="urn:jboss:module:1.3" name="com.threeamigos.common.util">
           <resources>
               <resource-root path="common-utils.jar"/>
           </resources>
           <dependencies>
               <module name="jakarta.enterprise.api"/>
               <module name="jakarta.inject.api"/>
               <module name="jakarta.servlet.api"/>
               <!-- Other dependencies -->
           </dependencies>
       </module>
       ```

## 2. Syringe Bootstrap API (Core Refactor)
Currently, `Syringe` is designed for SE-style manual instantiation and automatic classpath scanning. For WildFly, we need a managed bootstrap.

- **Refactor Suggestions:**
    - Decouple **Discovery** from **Initialization**. [DONE]
    - Create a `SyringeBootstrap` class that accepts a `Set<Class<?>>` and a `ClassLoader` directly. [DONE]
    - **Why?** WildFly already scans all classes via Jandex (a high-performance indexer). Syringe should reuse this index instead of re-scanning the JARs.

## 3. WildFly Subsystem Implementation
You must create a dedicated WildFly extension.

- **Extension Class:** `com.threeamigos.common.util.implementations.injection.wildfly.SyringeExtension` [DONE]
    - Registers the `syringe` subsystem in `standalone.xml`.
    - Handles the parsing of `<syringe xmlns="urn:jboss:domain:syringe:1.0"/>`.
- **Subsystem Add Handler:** `SyringeSubsystemAdd` [DONE]
    - Registers a global "Syringe Manager Service" in the **MSC (Modular Service Container)**.

## 4. Deployment Unit Processors (DUPs) [DONE]
The DUP is the "hook" that detects a deployment (WAR/EAR) and attaches Syringe.

- **Phase: DEPENDENCIES** [DONE]
    - Added `SyringeDependencyProcessor` to add `com.threeamigos.common.util` module dependency to deployments.
- **Phase: POST_MODULE** [DONE]
    - Added `SyringeDeploymentProcessor` to retrieve the `CompositeIndex` (Jandex) from the `DeploymentUnit`.
    - Extract all classes and instantiate a deployment-specific `Syringe` container via `SyringeBootstrap`.
- **Phase: INSTALL** [DONE]
    - Register the `BeanManager` in JNDI at `java:comp/BeanManager`.
    - Implementation: `SyringeJndiBinderProcessor`.

## 5. CDI Bridge (The Provider) [DONE]
To satisfy `CDI.current()`, we must use the existing `SyringeCDIProvider`.

- **Implementation:** [DONE]
    - Created `SyringeSetupAction` which implements WildFly's `SetupAction` interface.
    - Before a request enters a Servlet or EJB, WildFly executes this setup action.
    - **Action:** Calls `SyringeCDIProvider.registerThreadLocalCDI(syringe.getCDI())`.
    - **Cleanup:** Calls `SyringeCDIProvider.unregisterThreadLocalCDI()` in the teardown action.
    - **Registration:** `SyringeDeploymentProcessor` adds the `SyringeSetupAction` to the deployment's `SETUP_ACTIONS` attachment list.
- **Benefit:** This ensures that `CDI.current()` always returns the container instance belonging to the current deployment, maintaining isolation between multiple WARs.

## 6. Integration with Jakarta EE Services
A true WildFly integration requires Syringe to interact with other subsystems:

- **Resource Injection:** Syringe should handle `@Resource`, `@PersistenceContext`, and `@EJB` by delegating back to WildFly's JNDI/Naming system.
- **Transaction Support:** Integrate Syringe's interceptors with WildFly's Transaction Manager (`JTA`).
- **Servlet Injection:** Provide a `SyringeServletExtension` that allows Syringe to inject into Servlets, Filters, and Listeners (which are instantiated by the Web Subsystem, not Syringe).

## 7. TCK Readiness & Arquillian
To achieve official CDI 4.1 compliance, Syringe must pass the TCK using the Arquillian framework.

- **Arquillian Container Adapter:**
    - Develop a `Syringe-WildFly-Arquillian` adapter.
    - This adapter manages the server lifecycle and handles deployment of the TCK test WARs.
- **Test Enrichment:**
    - Implement an Arquillian `TestEnricher` to ensure `@Inject` works inside the test classes themselves.
- **Weld Replacement:**
    - Ensure the test environment is configured to exclude Weld and use the `syringe` subsystem exclusively to avoid JNDI name conflicts.

## 8. Configuration Example
In WildFly's `standalone.xml`:
```xml
<profile>
    ...
    <subsystem xmlns="urn:jboss:domain:syringe:1.0">
        <!-- Syringe global settings go here -->
    </subsystem>
</profile>
```

## Summary of Responsibilities
| Component | Responsibility |
| :--- | :--- |
| **WildFly DUP** | Scans classes, manages lifecycle of Syringe instances. |
| **Syringe** | Handles dependency injection, interceptors, and observers. |
| **SyringeCDIProvider** | Bridges the gap for static `CDI.current()` calls. |
| **JNDI** | Exposes the `BeanManager` to the rest of the application server. |
| **Arquillian Adapter**| Manages the test lifecycle for TCK execution. |
