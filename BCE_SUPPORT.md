# BCE Support Status (Syringe)

## Supported Runtime Scope
1. BCE phases: `@Discovery`, `@Enhancement`, `@Registration`, `@Synthesis`, `@Validation`.
2. Deterministic execution:
   - phase order is fixed (`DISCOVERY -> ENHANCEMENT -> REGISTRATION -> SYNTHESIS -> VALIDATION`);
   - method order is deterministic (extension class name, then method name).
3. Signature validation:
   - exactly one BCE phase annotation per method;
   - return type must be `void`;
   - illegal signatures fail deployment (`DefinitionException`).
4. Phase parameters currently supported:
   - `@Discovery`: `BuildServices`, `Types`, `Messages`, `MetaAnnotations`, `ScannedClasses`
   - `@Enhancement`, `@Validation`: `BuildServices`, `Types`, `Messages`
   - `@Enhancement` also supports one model/config parameter per method:
     - `ClassInfo` or `ClassConfig`
     - `MethodInfo` or `MethodConfig`
     - `FieldInfo` or `FieldConfig`
     - `ClassConfig` now exposes member graph collections (`constructors()`, `methods()`, `fields()`) with nested method/constructor `ParameterConfig` lists.
     - enhancement `addAnnotation/removeAnnotation/removeAllAnnotations` operations are materialized in subsequent `info()` annotation views for class/method/field/parameter configs.
     - enhancement config objects are now reused per target during one enhancement phase so mutations persist across enhancement methods for the same class/method/field target.
     - method/field config mutation coherence is preserved between dedicated `MethodConfig`/`FieldConfig` enhancement paths and `ClassConfig.methods()/fields()` views.
     - field mutation coherence is covered in both directions, including visibility from `ClassConfig.fields()` changes into later `FieldInfo` model invocations in the same enhancement phase.
     - method mutation coherence is covered in both directions, including visibility from `ClassConfig.methods()` changes into later `MethodInfo` model invocations in the same enhancement phase.
     - `removeAllAnnotations()` coherence is covered across method/field config views and subsequent info-model views in the same enhancement phase.
     - `removeAnnotation(predicate)` coherence is covered across method/field config views and subsequent info-model views in the same enhancement phase.
     - parameter-level annotation mutation coherence is covered from `ParameterConfig` into later `MethodInfo.parameters()` views in the same enhancement phase.
   - `@Registration`: `InvokerFactory`, `BceRegistrationContext`, `BuildServices`, `Types`, `Messages`
   - `@Registration` and `@Validation` also support one model parameter per method:
     - `BeanInfo` or `ObserverInfo` or `InterceptorInfo` or `InjectionPointInfo` or `DisposerInfo` or `ScopeInfo` or `StereotypeInfo`
   - `@Synthesis`: required `SyntheticComponents`, optional `BuildServices`, `Types`, `Messages`
5. Build services:
   - `BuildServicesResolver` is bound during BCE phase execution.
6. Invoker/synthetic runtime flow:
   - `InvokerFactory.createInvoker(...).build()` -> `InvokerInfo`;
   - runtime materialization to `Invoker` in synthetic beans/observers is implemented.
7. Metadata surface:
   - reflection-backed BCE metadata/language-model adapters are implemented and used by runtime flows.
   - `repeatableAnnotation(...)` now resolves repeated annotations from container annotations in adapter-backed metadata views.
   - repeatable-annotation resolution is aligned for mutable enhancement adapter declaration views as well.
   - validation/registration `ObserverInfo` model collection includes synthetic observers created via BCE `SyntheticComponents`.
   - synthetic `ObserverInfo` now exposes non-throwing `observerMethod()`/`eventParameter()` metadata (placeholder method model) for extension-time diagnostics/introspection.
   - synthetic `ObserverInfo.bean()` access is covered in conformance tests.

## Test Coverage Highlights
1. Phase ordering and mixed portable/BCE lifecycle ordering.
2. Signature matrix positive/negative cases (including missing/duplicate/unsupported params).
   - includes duplicate `SyntheticComponents` rejection in `@Synthesis`.
   - includes duplicate `Messages` rejection in `@Registration`.
   - includes unsupported `MetaAnnotations` rejection in `@Synthesis`.
   - includes duplicate `BuildServices` rejection in `@Registration`.
   - includes enhancement model/config validation and per-element invocation coverage.
   - includes registration/validation model parameter validation (`BeanInfo`/`ObserverInfo`) and duplicate/illegal parameter checks.
   - includes registration/validation model parameter validation for `InterceptorInfo` and invalid multi-model combinations.
   - includes registration/validation model parameter validation for `InjectionPointInfo` and invalid multi-model combinations.
   - includes registration/validation model parameter validation for `DisposerInfo` and invalid multi-model combinations.
   - includes registration/validation model parameter validation for `ScopeInfo` and invalid multi-model combinations.
   - includes registration/validation model parameter validation for `StereotypeInfo` and invalid multi-model combinations.
3. BCE diagnostics behavior:
   - `Messages.error(String|Exception)` causes deployment failure.
   - `Messages.info/warn` do not fail deployment.
4. Dedup behavior:
   - duplicate BCE registration in same channel is deduplicated;
   - duplicate portable extension registration in same channel is deduplicated.
5. BCE conformance gate tag:
   - tagged tests: `bce-conformance`
   - command: `mvn -Dgroups=bce-conformance test`
   - latest run: 79 tests, 0 failures, 0 errors.

## Remaining Gaps (Full BCE Support)
1. Full language-model edge-case parity beyond current repeatable support:
   - advanced generic/type-use fidelity for nested parameterized/wildcard/type-variable structures,
   - additional declaration/type model corner cases.
2. Full configurator contract parity beyond current enhancement-focused coverage:
   - broader mutable API semantics and corner cases,
   - consistency guarantees across all BCE model/config surfaces.
3. Additional ObserverInfo parity for synthetic observer edge-cases (beyond current synthetic visibility, metadata placeholders, and diagnostics safety coverage).
4. Broader conformance matrix depth (more positive/negative cross-combination cases).
