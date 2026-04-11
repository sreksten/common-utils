# Injection Package Splitting Roadmap

## Goal
- Reduce cognitive load and merge risk by splitting very large classes into focused units with explicit responsibilities.
- Keep behavior stable while extracting orchestration, validation, metadata, and runtime execution concerns into dedicated components.

## Selection Criteria
- Primary candidates are classes with very high LOC and multiple unrelated responsibilities.
- Priority is based on architectural centrality first (container bootstrap/resolution/validation), then size.

## Largest Classes Snapshot
- `Syringe.java` (6218 LOC)
- `CDI41BeanValidator.java` (5432 LOC)
- `BeanManagerImpl.java` (5317 LOC)
- `BeanImpl.java` (3172 LOC)
- `CDI41InjectionValidator.java` (3022 LOC)
- `EventImpl.java` (2356 LOC)
- `BceMetadata.java` (1907 LOC)
- `BeanResolver.java` (1725 LOC)
- `BuildCompatibleExtensionRunner.java` (1620 LOC)
- `InjectorImpl.java` (1554 LOC)
- `TypeChecker.java` (1046 LOC)
- `ClientProxyGenerator.java` (1001 LOC)
- `ProducerBean.java` (981 LOC)

## Recommended Waves
1. Wave 1: `Syringe`, `CDI41BeanValidator`, `BeanManagerImpl`
2. Wave 2: `BeanImpl`, `CDI41InjectionValidator`, `EventImpl`, `BeanResolver`
3. Wave 3: `BuildCompatibleExtensionRunner`, `BceMetadata`
4. Wave 4: `TypeChecker`, `ClientProxyGenerator`, `InjectorImpl`, `ProducerBean`

## Progress Updates
- `2026-04-11`: Started Wave 3 with `KnowledgeBase` split phase 1.
- Implemented internal state stores and kept `KnowledgeBase` as facade/composition root.
- Added:
  - `KnowledgeBaseDiscoveryStore`
  - `KnowledgeBaseBeanRegistryStore`
  - `KnowledgeBaseExtensionRegistrationStore`
  - `KnowledgeBaseEnablementStore`
  - `KnowledgeBaseProblemCollector`
- `KnowledgeBase.clearAllState()` now delegates to store clear methods.
- `2026-04-11`: Removed facade-level collection aliases from `KnowledgeBase`; methods now delegate directly to store-owned state.
- `2026-04-11`: Tightened the facade boundary: `KnowledgeBase` now uses store behavior APIs (no direct `store.collection().add/put/compute` mutation calls).
- `2026-04-11`: Extracted interceptor query/sorting logic from `KnowledgeBase` into `interceptors/InterceptorsHelper`; `KnowledgeBase` now delegates.
- `2026-04-11`: Extracted beans.xml interceptor/decorator ordering logic into `beansxml/BeansXmlOrderingHelper`; `KnowledgeBase` now delegates.
- `2026-04-11`: Extracted alternative helper logic into `annotations/AlternativesHelper` and stereotype-recursion logic into `annotations/StereotypesHelper`; `KnowledgeBase` now delegates.
- `2026-04-11`: Point 12 split completed for `AnnotationsEnum`: static predicate methods moved to `AnnotationPredicates`, static extractor methods moved to `AnnotationExtractors`, dynamic runtime annotation state/operations moved to `DynamicAnnotationRegistry`; `AnnotationsEnum` kept as clean enum aliases + matching behavior.
- `2026-04-11`: Step 2 started (`CDI41BeanValidator` split): extracted stereotype/scope/priority rule group into `discovery/StereotypePriorityValidator`; `CDI41BeanValidator` now delegates stereotype validation and stereotype-priority collection.
- `2026-04-11`: Continued Step 2 (`CDI41BeanValidator` split):
  - Extracted `discovery/BeanClassEligibilityValidator` and delegated managed-bean candidacy checks.
  - Extracted `discovery/BeanAttributesExtractor` and delegated bean attribute extraction (name/qualifiers/scope/stereotypes).
  - Extracted `discovery/InjectionMetadataValidator` and delegated constructor/injection-point/metadata validation blocks.
  - Extracted `discovery/ProducerDisposerValidator` and delegated producer/disposer/specializing-producer validation and disposer matching.
  - Extracted `discovery/InterceptorDecoratorDefinitionValidator` and delegated interceptor/decorator definition checks, binding-conflict checks, and registration metadata.
  - Extracted `discovery/BeanRegistrationService` and delegated producer bean registration plus managed bean registration flow.
- `2026-04-11`: Step 2 completed. `CDI41BeanValidator` now acts primarily as orchestration/facade with split validation/registration units.
- `2026-04-11`: Step 3 started (`BeanManagerImpl` split):
  - Extracted global BeanManager registry, transient dependent-reference tracking, and BeanManager serialization-handle resolution into `spi/BeanManagerRegistry`.
  - `BeanManagerImpl` now delegates registration/unregistration, snapshot lookup, transient reference lifecycle, and `writeReplace` serialization indirection to the helper.

## Class-by-Class Split Targets

### 1) `Syringe.java` (6218)
Current responsibilities:
- Container bootstrap API and config toggles.
- Portable extension and build-compatible extension loading.
- Discovery orchestration, archive mode handling, and beans.xml integration.
- Full CDI lifecycle phase orchestration.
- SPI event pipelines (`ProcessAnnotatedType`, `ProcessInjectionPoint`, `ProcessBeanAttributes`, producer/observer events).
- Alternative/priority/stereotype enablement logic.
- Deployment validation and beans.xml alternatives/interceptors/decorators checks.
- Extension observer dispatch engine with generic matching and `@WithAnnotations` filtering.
- Observer lifecycle fallback discovery and runtime invocation adapters.
- Shutdown/state cleanup and programmatic runtime helpers.

Suggested split units:
- `ContainerBootstrapOrchestrator`
- `ExtensionLoader` and `BuildCompatibleExtensionLoader`
- `DiscoveryCoordinator`
- `LifecyclePhaseExecutor`
- `SpiEventPipeline` and `ProducerObserverPipeline`
- `DeploymentValidationCoordinator`
- `ExtensionObserverDispatcher`
- `ContainerShutdownCoordinator`
- `SyringeRuntimeFacade` for helper APIs (`inject`, request/session helpers)

Extraction notes:
- First extract pure helper blocks (archive mode, priority/stereotype helpers, beans.xml validation helpers).
- Then isolate event pipelines; keep `Syringe` as high-level orchestrator only.

### 3) `BeanManagerImpl.java` (5317)
Current responsibilities:
- BeanManager API surface implementation.
- Bean lookup and resolution orchestration.
- Context access and BCE custom context handling.
- Observer/interceptor/decorator resolution.
- Built-in beans (`Event`, `Instance`) and built-in lookup behavior.
- Injection point validation and injectable reference creation.
- Registry/serialization/transient dependent reference tracking.
- Misc bean attribute extraction from annotated metadata.

Suggested split units:
- `BeanLookupService`
- `InjectableReferenceService`
- `ObserverResolutionService`
- `InterceptorResolutionService`
- `DecoratorResolutionService`
- `ContextAccessService`
- `BuiltInBeanServices`
- `BeanManagerRegistry`

Extraction notes:
- Static registry and serialization support should leave first.
- Keep `BeanManagerImpl` thin and API-focused.

### 4) `BeanImpl.java` (3172)
Current responsibilities:
- Bean metadata model and mutators.
- Instance creation and destruction lifecycle.
- Constructor/field/method injection execution.
- InjectionPoint contextual stack and transient argument destruction.
- PostConstruct/PreDestroy/passivation lifecycle callbacks.
- Interceptor chain construction and invocation (including legacy style).
- Decorator chain application and cleanup.
- Custom `InjectionTarget` integration.

Suggested split units:
- `BeanDefinitionState`
- `BeanInstanceFactory`
- `BeanInjectionExecutor`
- `BeanLifecycleCallbacks`
- `BeanInterceptionEngine`
- `BeanDecorationEngine`
- `TransientReferenceCleanupService`

Extraction notes:
- Split runtime execution from metadata first.
- Interceptor/decorator logic should be extracted behind strategy interfaces.

### 5) `CDI41InjectionValidator.java` (3022)
Current responsibilities:
- Unsatisfied/ambiguous dependency validation.
- Circular dependency detection.
- Name resolution conflict checks.
- Alternative conflict/precedence validation.
- Proxyability and unproxyable type checks.
- Passivation validation and transient reference rules.
- Decorator/interceptor binding-related validation.

Suggested split units to put into injection/discovery/validation/injection:
- `InjectionPointResolutionValidator`
- `CircularDependencyValidator`
- `BeanNameConflictValidator`
- `AlternativeSelectionValidator`
- `ProxyabilityValidator`
- `PassivationValidator`
- `DecoratorInterceptorUsageValidator`

Extraction notes:
- This class is rule-heavy; rule objects per concern will simplify testing and maintenance.

### 6) `EventImpl.java` (2356)
Current responsibilities:
- Synchronous and asynchronous event dispatch.
- Observer filtering and matching with generic type resolution.
- Transaction phase handling for observer delivery.
- Context activation/deactivation around observer invocation.
- Observer invocation reflection and dependent cleanup.
- `Event.select(...)` API and qualifier validation.
- Event serialization/context snapshot support.
- Async executor lifecycle management.

Suggested split units:
- `EventDispatcher`
- `ObserverMatcher`
- `ObserverInvoker`
- `ObserverContextCoordinator`
- `EventTypeResolutionService`
- `EventSerializationSupport`
- `AsyncEventExecutorProvider`

Extraction notes:
- Split matcher and invoker early; they are independently testable and reduce regression risk.

### 7) `BceMetadata.java` (1907)
Current responsibilities:
- Reflection to BCE model conversion.
- BCE model unwrapping back to reflection types.
- Annotation/member conversion and proxying.
- Runtime and reflection bean info adapters.
- Large set of nested classes implementing BCE model interfaces.

Suggested split units:
- `BceTypeAdapters`
- `BceAnnotationAdapters`
- `BceBeanInfoAdapters`
- `BceDeclarationAdapters` (`ClassInfo`, `MethodInfo`, `FieldInfo`)
- `BceRecordIntrospectionSupport`

Extraction notes:
- Move nested adapter classes to dedicated files; keep `BceMetadata` as static facade.

### 8) `BeanResolver.java` (1725)
Current responsibilities:
- Core runtime resolution for normal injections.
- Built-in resolution for `Event`, `Instance/Provider`, `InterceptionFactory`, decorator metadata.
- Candidate filtering by type/qualifier/accessibility/specialization.
- Alternative precedence decisions.
- Optional decoration of resolved instances.
- Dynamic injection-point context handling.

Suggested split units:
- `InjectionCandidateFinder`
- `BuiltInInjectionResolver`
- `AlternativePrecedenceResolver`
- `BeanAccessibilityGuard`
- `ResolvedInstanceDecorator`
- `InjectionPointContextStack`

Extraction notes:
- Separate candidate-finding from instance-creation/decoration to simplify resolution debugging.

### 9) `BuildCompatibleExtensionRunner.java` (1620)
Current responsibilities:
- Phase method scanning and ordering.
- Phase method signature validation.
- Argument resolution for BCE phase methods.
- Registration model fan-out (beans, observers, interceptors, injection points, scopes, stereotypes).
- Delivery deduplication and filtering (`@Registration(types=...)`, enhancement filters).
- Enhancement model state application.

Suggested split units:
- `BcePhaseMethodScanner`
- `BcePhaseSignatureValidator`
- `BcePhaseArgumentResolver`
- `BceRegistrationModelDispatcher`
- `BceEnhancementModelApplier`
- `BceInvocationExecutor`

Extraction notes:
- Registration-model dispatch should be split first; it is the highest branching complexity area.

### 10) `InjectorImpl.java` (1554)
Current responsibilities:
- Legacy bootstrap and classpath scanning flow.
- Scope registration and handler management.
- Constructor/field/method injection logic.
- Provider/Instance/Event wrapper production.
- Lifecycle invocation and shutdown handling.

Suggested split units:
- `LegacyBootstrapEngine`
- `ScopeRegistryService`
- `LegacyInjectionExecutor`
- `LegacyProgrammaticLookupFactory`
- `LegacyLifecycleManager`

Extraction notes:
- If `Syringe` is the strategic path, consider minimizing `InjectorImpl` to compatibility facade.

### 13) `TypeChecker.java` (1046)
Current responsibilities:
- CDI type assignability engine.
- Event type assignability rules.
- Lookup assignability rules and type-variable/wildcard handling.

Suggested split units:
- `GenericAssignabilityEngine`
- `EventTypeAssignabilityPolicy`
- `LookupTypeAssignabilityPolicy`

Extraction notes:
- Preserve existing algorithm tests before extraction; this is behavior-critical infrastructure.

### 14) `ClientProxyGenerator.java` (1001)
Current responsibilities:
- Proxyability validation.
- Proxy class generation/instantiation.
- Container context registry by classloader/bean manager id.
- Proxy serialization and restoration support.

Suggested split units:
- `ProxyabilityValidator`
- `ProxyClassFactory`
- `ClientProxyContainerRegistry`
- `ClientProxySerializationSupport`

Extraction notes:
- Keep generation and serialization separate to reduce accidental coupling.

### 15) `ProducerBean.java` (981)
Current responsibilities:
- Producer method/field invocation.
- Disposer invocation.
- Dependent/transient parameter and receiver cleanup.
- Passivation checks for produced values.
- Producer metadata state and injection-point resolution.

Suggested split units:
- `ProducerDefinitionState`
- `ProducerInvoker`
- `DisposerInvoker`
- `ProducerDependentCleanupService`
- `ProducerPassivationValidator`

Extraction notes:
- Split lifecycle/invocation from metadata first; this mirrors the `BeanImpl` direction.

## Suggested Execution Pattern For Each Split
- Introduce a new class and delegate from original class without changing public behavior.
- Move one concern at a time with tests green after each move.
- Keep old methods as thin wrappers until extraction is complete.
- Remove dead helper methods only after the delegated path is stable.
