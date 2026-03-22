# BCE Roadmap (Clean)

## Goal
Reach full CDI BCE support in Syringe with deterministic lifecycle execution, robust runtime behavior, and explicit compliance boundaries.

## Current State
1. Core lifecycle support is implemented for `@Discovery`, `@Enhancement`, `@Registration`, `@Synthesis`, `@Validation`.
2. Deterministic phase/method ordering is implemented and tested.
3. Invoker registration-to-synthesis-to-runtime materialization flow is implemented and tested.
4. Supported phase parameter matrix is implemented with strict validation and deployment-error semantics.
5. `@Enhancement` supports per-element model/config parameters (`ClassInfo/ClassConfig`, `MethodInfo/MethodConfig`, `FieldInfo/FieldConfig`) with `@Enhancement` filter-driven invocation.
6. BCE diagnostics behavior is tested (`Messages.error` fails deployment, `info/warn` do not).
7. Duplicate extension registration dedup is tested for both BCE and portable channels.
8. BCE-focused test gate exists via `@Tag("bce-conformance")`.
9. Current BCE conformance gate passes (`mvn -Dgroups=bce-conformance test`).
10. Discovery `ScannedClasses` behavior is covered with positive/negative tests.
11. Enhancement subtype filtering (`withSubtypes=true/false`) is covered by conformance tests.
12. `@Registration`/`@Validation` support model parameters (`BeanInfo`/`ObserverInfo`) with per-item invocation and legality checks.
13. `@Registration`/`@Validation` support `InterceptorInfo` model parameter with legality checks and conformance tests.
14. `@Registration`/`@Validation` support `InjectionPointInfo` model parameter with per-item invocation and legality checks.
15. `@Registration`/`@Validation` support `DisposerInfo` model parameter with legality checks and conformance tests.
16. `@Registration`/`@Validation` support `ScopeInfo` model parameter with legality checks and conformance tests.
17. `@Registration`/`@Validation` support `StereotypeInfo` model parameter with legality checks and conformance tests.
18. Invoker coverage split is now explicit:
   - `UsingInvokerBuilderTest` validates API/signature contract surfaces only (reflection-level checks, no container bootstrap/runtime invocation path).
   - Runtime BCE/invoker behavior is covered by `BuildCompatibleInvokerMaterializationTest`, `BuildingAnInvokerTest`, and `UsingAnInvokerTest`.

## Full Support Gaps
1. Full language-model conformance for advanced generic/type-use/declaration edge cases (beyond current repeatable-annotation support).
2. Full configurator behavior parity for all BCE mutable model/config APIs (beyond current enhancement-focused coverage).
3. Additional synthetic-observer metadata parity (beyond current visibility, placeholder metadata, and diagnostics safety).
4. Broader conformance depth across legal/illegal API cross-combinations.

## Robustness Snapshot
1. Practical robustness is high: roughly 97-98% for currently implemented BCE runtime paths.
2. Remaining distance to "full" BCE is concentrated in edge-case parity and breadth:
   - language-model edge cases (complex generics/type-use fidelity),
   - deeper mutable/configurator corner semantics beyond enhancement-heavy paths,
   - synthetic observer metadata parity edge-cases,
   - larger negative/combination conformance matrix.

## Next Steps (Ordered)
1. Expand `BceMetadata`/adapters for deeper generic/type-use edge cases (nested wildcard/type-variable structures and annotation fidelity).
2. Extend configurator parity beyond enhancement-focused semantics to remaining mutable API corners.
3. Add synthetic-observer `ObserverInfo` parity cases beyond current placeholder-based coverage.
4. Expand conformance matrix breadth (positive + negative) and keep `bce-conformance` as acceptance gate.

## Recently Completed (This Iteration)
1. Added `@Enhancement` model/config parameter support with per-element invocation and filter matching.
2. Added enhancement conformance tests (class/method model invocation and invalid multi-model signature).
3. Added additional negative signature tests (`@Synthesis`/`@Registration`) and kept `bce-conformance` gate green.
4. Added enhancement conformance test for `MethodConfig` per matching annotated method.
5. Added enhancement conformance test for `FieldInfo` and `FieldConfig` per matching annotated field.
6. Added discovery `ScannedClasses` tests for successful class add and invalid class rejection.
7. Added enhancement `withSubtypes` true/false conformance tests.
8. Added enhancement test for model + services (`BuildServices`, `Types`, `Messages`) and duplicate `Messages` rejection with model.
9. Added enhancement no-match conformance tests for class and method annotation filters.
10. Added discovery negative conformance tests for duplicate `MetaAnnotations` and duplicate `ScannedClasses` parameters.
11. Added enhancement/validation negative conformance tests for duplicate `BuildServices` (`@Enhancement`) and duplicate `Messages` (`@Validation`).
12. Implemented `BeanInfo`/`ObserverInfo` model-parameter support for `@Registration` and `@Validation` with per-item invocation.
13. Added conformance tests for registration/validation model parameters and invalid multi-model signatures.
14. Implemented `InterceptorInfo` model-parameter support for `@Registration`/`@Validation`.
15. Added conformance tests for `InterceptorInfo` model parameters and invalid `BeanInfo + InterceptorInfo` multi-model signatures.
16. Implemented `InjectionPointInfo` model-parameter support for `@Registration`/`@Validation` including `@Inject` fields and `@Inject` constructor/method parameters.
17. Added conformance tests for `InjectionPointInfo` model parameters and invalid `BeanInfo + InjectionPointInfo` multi-model signatures.
18. Implemented `DisposerInfo` model-parameter support for `@Registration`/`@Validation` from discovered producer/disposer metadata.
19. Added conformance tests for `DisposerInfo` model parameters and invalid `BeanInfo + DisposerInfo` multi-model signatures.
20. Implemented `ScopeInfo` model-parameter support for `@Registration`/`@Validation` with deterministic unique-scope invocation.
21. Added conformance tests for `ScopeInfo` model parameters and invalid `BeanInfo + ScopeInfo` multi-model signatures.
22. Implemented `StereotypeInfo` model-parameter support for `@Registration`/`@Validation` with deterministic unique-stereotype invocation.
23. Added conformance tests for `StereotypeInfo` model parameters and invalid `BeanInfo + StereotypeInfo` multi-model signatures.
24. Included synthetic observers in `ObserverInfo` model collection for BCE registration/validation phases.
25. Added conformance test ensuring validation `ObserverInfo` receives synthetic observers and `Messages.*(…, ObserverInfo)` remains safe for synthetic metadata.
26. Added non-throwing synthetic `ObserverInfo.observerMethod()` and `ObserverInfo.eventParameter()` metadata exposure via placeholder method model.
27. Extended synthetic observer parity conformance test to assert method/parameter metadata accessibility.
28. Extended synthetic observer parity conformance test to assert `ObserverInfo.bean()` accessibility for synthetic observers.
29. Implemented `ClassConfig` member graph exposure (`constructors()`, `methods()`, `fields()`) and nested `ParameterConfig` availability for enhancement phase introspection.
30. Added conformance test that validates `ClassConfig` member graph and nested parameter-config availability for a concrete target type.
31. Implemented mutation-aware annotation views for enhancement configs so `addAnnotation/removeAnnotation` are reflected by `info()` on class/method/field/parameter configs.
32. Added conformance test that validates add/remove annotation materialization for class, method, field and parameter config views.
33. Implemented enhancement-model state caching so config mutations persist across enhancement methods for the same target within one enhancement phase execution.
34. Added conformance test validating cross-method persistence (`ClassConfig` mutation in one enhancement method visible in a later enhancement method).
35. Implemented shared `MethodConfig`/`FieldConfig` instances between method/field model paths and `ClassConfig.methods()/fields()` views to preserve mutation coherence across enhancement config views.
36. Added conformance test validating `MethodConfig` mutation visibility when later inspected through `ClassConfig.methods()` in the same enhancement phase.
37. Added conformance test validating field mutation coherence in both directions: `FieldConfig` -> `ClassConfig.fields()` and `ClassConfig.fields()` -> `FieldInfo` model view.
38. Added conformance test validating reverse method-view coherence: mutation applied through `ClassConfig.methods()` is visible in later `MethodInfo` enhancement model invocations.
39. Added conformance test validating `removeAllAnnotations()` coherence across views for both methods and fields (`MethodConfig`/`FieldConfig` -> `ClassConfig` -> `MethodInfo`/`FieldInfo`).
40. Added conformance test validating `removeAnnotation(predicate)` coherence across method/field config views and subsequent class/info model views in the same enhancement phase.
41. Implemented/validated parameter-level mutation parity: `ParameterConfig` annotation add/remove now remains visible in later `MethodInfo.parameters()` views within the same enhancement phase.
42. Expanded language-model adapter behavior for repeatable annotations by flattening container annotations in `repeatableAnnotation(...)`.
43. Added conformance test validating repeated annotations are resolved through language-model `ClassInfo.repeatableAnnotation(...)`.
44. Extended repeatable-annotation flattening parity to mutable enhancement adapter views (`ClassConfig.info()` and related declaration wrappers), with conformance checks.
