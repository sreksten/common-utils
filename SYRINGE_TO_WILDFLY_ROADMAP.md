# Syringe to WildFly Integration Roadmap (Revalidated)

This roadmap was re-checked against the current implementation in `common-utils`.

## Current Reality

Syringe has working integration pieces, but WildFly execution is still **Weld-coupled**:

- `SyringeExtension` registers deployment processors on Weld-specific phases:
  - `Phase.DEPENDENCIES_WELD`
  - `Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS`
  - `Phase.INSTALL_WELD_BEAN_MANAGER`
- `SyringeDependencyProcessor` explicitly adds module dependency `org.jboss.weld.core`.
- Arquillian support is present (`SyringeWildFlyArquillianContainer`, `SyringeArquillianExtension`, transformer, enricher), but this does not by itself replace Weld inside WildFly.

Because of this, today we have: **Syringe integrated into a Weld-shaped server lifecycle**, not full “Syringe instead of Weld”.

## Point-by-Point Status Check

### 1. Modularization (WildFly Modules)
- Status: **Partial**
- Present:
  - Service registration for WildFly extension: `META-INF/services/org.jboss.as.controller.Extension`.
  - `SyringeDependencyProcessor` adds `com.threeamigos.common.util` module to deployments.
- Missing/Needs hardening:
  - Production-ready module distribution docs/scripts for WildFly install.
  - Clear versioned module layout and compatibility matrix per WildFly version.

### 2. Syringe Bootstrap API
- Status: **Implemented (with caveats)**
- Present:
  - `SyringeBootstrap(Set<Class<?>>, ClassLoader)` exists.
  - `SyringeDeploymentProcessor` uses Jandex `CompositeIndex` and bootstraps deployment-specific Syringe.
- Caveat:
  - `classLoader` parameter in `SyringeBootstrap` is accepted but not actively used in bootstrap decisions.

### 3. WildFly Subsystem
- Status: **Implemented**
- Present:
  - `SyringeExtension` registers subsystem and XML parser.
  - `SyringeSubsystemAdd` installs `SyringeService`.
  - Namespace parsing for `urn:jboss:domain:syringe:1.0`.
- Caveat:
  - Runtime activation points are tied to Weld phase slots.

### 4. Deployment Unit Processors (DUPs)
- Status: **Implemented (Weld-coupled)**
- Present:
  - `SyringeDependencyProcessor` (module dependencies).
  - `SyringeDeploymentProcessor` (class extraction, bootstrap, attach container, setup action).
  - `SyringeJndiBinderProcessor` (JNDI bind of BeanManager).
- Gap:
  - The processors execute in Weld phase anchors; not provider-neutral.

### 5. CDI Bridge (`CDI.current()`)
- Status: **Implemented**
- Present:
  - `SyringeCDIProvider` registered via ServiceLoader.
  - `SyringeSetupAction` registers/unregisters thread-local CDI per request/deployment execution path.
- Caveat:
  - Provider precedence and coexistence with Weld provider in full server runtime still needs explicit governance/testing.

### 6. Jakarta EE Service Integration
- Status: **Partial**
- Present:
  - JTA helper abstractions exist (`util/tx` package).
  - Servlet-related utility/filter code exists.
- Missing:
  - End-to-end WildFly integration for `@Resource`, `@PersistenceContext`, `@EJB`.
  - Verified integration contract with Undertow/EJB/JPA subsystems for container-managed artifacts.

### 7. TCK + Arquillian
- Status: **Partial**
- Present:
  - DeployableContainer implementation and Arquillian extension registration.
  - Deployment exception transformer and test enricher.
- Missing:
  - A fully Weld-free in-container TCK path in WildFly.
  - Server configuration that guarantees Syringe is the active CDI engine instead of Weld.

## Rethought Roadmap (for True Syringe-as-Provider in WildFly)

## Phase A: Define Target Runtime Model
- Decide and document one supported model:
  - `Model 1`: Syringe coexists with Weld (current state).
  - `Model 2`: Syringe replaces Weld as CDI provider (target requested).
- For `Model 2`, define subsystem ownership boundaries with web/ejb/jpa/naming/transactions.

## Phase B: Remove Weld Coupling in Boot Sequence
- Replace Weld phase anchors in `SyringeExtension` with provider-neutral phase anchors.
- Remove hard dependency on `org.jboss.weld.core` from `SyringeDependencyProcessor`.
- Add explicit startup ordering dependencies only where technically required.

## Phase C: CDI Service Surface Completion
- Ensure BeanManager lifecycle and visibility match Jakarta EE expectations.
- Implement or bridge required integration points:
  - Resource injection delegation.
  - Transaction synchronization and interceptor interactions.
  - Servlet/listener/filter injection lifecycle for server-instantiated components.

## Phase D: Server Configuration and Conflict Control
- Provide deterministic configuration to disable/replace Weld CDI activation for deployments under Syringe.
- Add startup validation that fails fast if both providers would claim the same deployment semantics.

## Phase E: Qualification and TCK Strategy
- Keep two tracks:
  - `SE track`: Syringe pure CDI behavior tests (`cdi41tests`) for fast regression.
  - `WF in-container track`: Arquillian/WildFly validation of runtime integration.
- For compliance evidence, ensure in-container runs prove Syringe provider ownership, not only transformed exception parity.

## Immediate Next Technical Steps

1. Refactor `SyringeExtension` deployment processor phase registrations away from `*_WELD*`.
2. Remove `org.jboss.weld.core` module injection from `SyringeDependencyProcessor` and run smoke deployments.
3. Add runtime diagnostics endpoint/log at deployment start:
   - active CDI provider class
   - BeanManager implementation class
   - subsystem that bound `java:module/BeanManager`
4. Add one dedicated integration test deployment asserting provider identity inside WildFly runtime.

## Minimal Configuration Example

```xml
<profile>
    <!-- other subsystems -->
    <subsystem xmlns="urn:jboss:domain:syringe:1.0"/>
</profile>
```

Note: this XML alone does not guarantee Weld replacement; provider selection depends on runtime wiring and subsystem activation rules above.
