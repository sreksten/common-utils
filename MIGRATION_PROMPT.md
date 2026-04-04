You are migrating CDI TCK tests to standalone Syringe compatibility tests in batches.

Non-negotiable constraints:
- Java version is strictly Java 8.
- All migrated tests and helper code must compile and run on Java 8.
- Do not introduce Java 9+ language features or APIs (for example `var`, `List.of`, `Set.of`, `Map.of`, `Optional.or`, module-system assumptions, or newer JDK-only methods).
- Test parity must be as strict as possible to the original TCK behavior.
- Do not relax tests to make Syringe pass:
  - do not weaken assertions,
  - do not broaden expected outcomes,
  - do not remove checks that exist in the original,
  - do not add `@Disabled`/ignored paths for failing behavior unless the original test is disabled,
  - do not add timing/retry hacks that hide deterministic failures.
- If a migrated test fails and fixture/test adherence to the original has been verified, treat it as a likely Syringe gap and investigate/fix Syringe.

Source of truth:
- Read class list from `CDI_TCK_CLASSES.md`.
- Original TCK source files are reference-only under project root `org/jboss/...` (not under `src/test/java`).
- Do NOT delete or alter in any way original `org.jboss...` classes.
- Python is not available in this environment; use shell tools only (`grep`, `sed`, `head`, `awk`) when selecting pending classes.
- Track status in that file only:
    - Pending: `- [ ] org.jboss.cdi.tck.tests....TestClass`
    - Ported:  `- [x] org.jboss.cdi.tck.tests....TestClass`
- If any old line is `- \`org.jboss...\``, convert it to pending format `- [ ] ...`.

Batch behavior:
- Port only the next N pending classes (default N=5 unless explicitly specified).
- Select pending classes with shell matching on lines that begin with `- [ ]` (including spaces), for example:
  `grep '^- \\[ \\] ' CDI_TCK_CLASSES.md | head -n N`
- Mark `[x]` only after the migrated class is fully passing.
- Leave `[ ]` for failing/incomplete classes.

Cost-efficiency rules (important for long migrations):
- Do not re-scan the full `org/jboss/...` tree each batch.
- For each class, read only:
  - the original test class,
  - directly referenced fixture/helper classes needed for parity.
- Reuse the Syringe assumptions documented in this prompt; do not rediscover container behavior unless a failure contradicts them.
- Keep per-batch edits small and focused on the selected classes only.
- Derive the minimal fixture set from:
  - injected fields in the test class,
  - classes listed in deployment builder calls (`withClasses`, `withBeanLibrary`, similar),
  - directly used assertion/helper types.
- Ignore audit metadata (`@SpecAssertion`, `@SpecVersion`, section constants) unless it affects runtime behavior.
- For multi-method TCK classes, split fixtures by test method when possible; smaller bean graphs reduce ambiguity and debugging cost.
- For priority/alternative tests, include only producers/beans that participate in the asserted resolution conflict.
- For stereotype-priority tests, prefer the minimal 4-class fixture shape:
  - stereotype annotation,
  - interface,
  - standard implementation,
  - alternative implementation with priority.
- Prefer bulk fixture bootstrap per selected package:
  - copy the exact reference fixture subtree from `org/jboss/...` into the mapped destination,
  - rewrite package declarations in one pass,
  - then minimally adapt only migrated parity tests and any fixture classes that still depend on Arquillian/TestNG/TCK utilities.
- Use migration archetypes to avoid rediscovery:
  - deployment-failure classes (`@ShouldThrowException`) usually map to `assertThrows(DeploymentException.class, syringe::setup)`,
  - runtime passivation dependency violations usually map to resolved contextual reference + `assertThrows(IllegalProductException.class, ...)`,
  - custom-context behavior classes usually map to extension instance registration + direct bean resolution assertions.

Mandatory package/class mapping:
- Original: `org.jboss.cdi.tck.tests.X.Y.Z.TestName`
- Target:   `com.threeamigos.common.util.implementations.injection.cditcktests.X.Y.Z.TestName`
- Keep same simple class name and same package tail after `org.jboss.cdi.tck.tests`.
- Destination filesystem root for migrated tests:
  `src/test/java/com/threeamigos/common/util/implementations/injection/cditcktests/...`

Reference-source rule:
- `org/jboss/...` is a reference corpus only.
- Reuse from it is allowed and encouraged for framework-neutral helpers (for example qualifiers and annotation literals like `True`, `False`, `TrueLiteral`, `FalseLiteral`, and similar `*Literal` classes).
- Because `org/jboss/...` is not a test source root, do not import those classes directly from root-level `org/jboss/...`.
- “Reuse” means copy/adapt into the migrated destination tree.

Isolation rules:
- No shared utility classes across migrated test classes.
- Isolate fixtures/helpers per test method in dedicated java-compliant subpackages (derived from method name).
- Duplicating fixtures is preferred to avoid cross-test pollution.
- Prefer a local `Probe` bean in each method fixture package to mirror the original test's injection points; assert on probe getters in the parity test class.
- Exception for neutral helpers:
  - If a helper is pure Jakarta/Java (no Arquillian/TestNG/audit/shrinkwrap dependency) and semantically generic, it may be reused from a shared support package under:
    `com.threeamigos.common.util.implementations.injection.cditcktests.support...`
  - Keep this limited to stable literals/qualifiers; test-behavior fixtures remain isolated per test/method.

Fixture-safety rules (mandatory):
- Every class under a scanned fixture package can be discovered as a bean in EXPLICIT mode if it is bean-defining.
- Do not place abstract helper beans inside scanned fixture packages unless they are intentionally excluded.
- If a helper must be abstract, either:
  - move it to a non-scanned package, or
  - explicitly exclude it (for example with `@Vetoed`) when that preserves parity intent.
- For `AssertBean`-style helpers used from injected test fixtures, prefer concrete classes in scanned packages.
- Do not scan shared support packages; only scan per-method fixture leaf packages.
- Do not make fixture classes depend on package-private members of the parent parity test class.
- In fixture subpackages, avoid importing the parity test class just to read constants; duplicate tiny literal values locally to prevent visibility/compile issues.

Syringe rules (do not re-discover each batch; assume these):
1) `setup()` = `initialize()` + discovery + `start()`.
2) `new Syringe("pkg")` scans that package recursively (includes subpackages).
3) For this migration, use one leaf fixture package per test method; scan only that package.
4) Use `forceBeanArchiveMode(BeanArchiveMode.EXPLICIT)` before bootstrap to avoid implicit-mode exclusions.
5) Always `shutdown()` in `finally`.
6) If package-scan causes contamination or unstable behavior, switch to deterministic bootstrap:
    - `new Syringe()`
    - `forceBeanArchiveMode(EXPLICIT)`
    - `initialize()`
    - `addDiscoveredClass(..., EXPLICIT)` for fixture classes only
    - `start()`
7) Do not replicate original multi-archive deployment shape unless behavior depends on archive boundaries or distinct `beans.xml` activation; prefer one isolated fixture package per method.
8) For passivation/proxy serialization scenarios, preserve these baselines:
    - proxy `writeReplace` must remain on serialization path (must not be re-intercepted as business method),
    - container must be registered for proxy deserialization during startup and unregistered at shutdown.
9) For custom passivating scopes (`@NormalScope(passivating = true)`), custom context `get(Contextual, CreationalContext)` calls should receive serializable-compatible contextual and creational parameters; if not, treat as Syringe gap.

Recommended default bootstrap helper in each migrated test:
- `Syringe s = new Syringe("<method-fixture-package>");`
- `s.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);`
- `s.setup();`
- `try { ...assertions... } finally { s.shutdown(); }`

Recommended assertion pattern:
- Resolve a single fixture `Probe` bean when a test validates multiple `@Inject` points.
- Keep assertions in the parity test class and keep probe logic minimal (injected fields + getters only).

Low-cost preflight checklist (before considering Syringe core changes):
1) Confirm scanned package is the intended leaf fixture package only.
2) Confirm fixture classes required as beans are concrete and bean-defining.
3) Confirm shared helpers/literals are not accidentally included in scan roots.
4) Confirm qualifiers/literals used in assertions match fixture annotations exactly.
5) Confirm fixture code compiles without cross-package access to non-public members.

Execution model:
- No Arquillian, no WildFly/container adapters, no deployment archives.
- Standalone Syringe tests only.
- Batch execution strategy for lower debug cost:
  - if a batch introduces a new mechanism (for example custom passivating scope, passivating interceptor wiring, producer passivation edge), run one sentinel class first,
  - only run the full N-class targeted Maven command after sentinel passes.
- After each batch migration, automatically run only the newly ported test classes via a targeted Maven run:
  - `mvn -q -Dtest=<FQCN1>,<FQCN2>,... test`
- Do not run full-suite Maven goals for migration verification.

Automatic completion rule:
- If all newly ported tests pass in the targeted batch run, mark those classes as `[x]` in `CDI_TCK_CLASSES.md`.
- If any newly ported test fails, keep that class as `[ ]`, fix minimally, and rerun only the still-failing classes.
- Never mark `[x]` for classes that were not executed in the targeted batch run.

Parity rules:
- Preserve original TCK intent, assertions, and expected exceptions.
- Match the strictness of original assertions and failure conditions; parity is correctness, not "close enough".
- Never make a failing migrated test pass by loosening its expectations.
- If exact parity is not possible, document mismatch and rationale.

Failure triage rules (if user reports a failing migrated test):
- First classify failure type before editing Syringe:
  - `Could not inject members`: check fixture wiring first (scan root, bean-defining annotations, abstract/discoverable helpers).
  - `DefinitionException` during bootstrap: check invalid fixture bean classes in scanned packages first.
  - Ambiguous/unsatisfied resolution: verify isolation boundaries and qualifiers before changing core resolution logic.
- Prefer fixing parity fixture mistakes before changing Syringe core.
- After fixture parity is confirmed, bias strongly toward fixing Syringe core/runtime behavior rather than altering tests.
- A parity-correct failing test is evidence of a Syringe gap until proven otherwise.
- If you change Syringe core, document why fixture-level fixes were insufficient.
- Low-credit debug workflow:
  - Run only the exact failing test:
    - `mvn -q -Dtest=<fully-qualified-test-class> test`
  - Inspect `target/surefire-reports` for that class before broader code exploration.
  - Apply the smallest viable patch (fixture/test wiring first; Syringe core only if fixture fixes are insufficient).
  - Rerun only the same failing test class once for verification.
  - Report concise root cause and changed files.

Report-truth rule:
- If class-level textual summaries and observed behavior disagree, use XML reports in `target/surefire-reports/TEST-*.xml` as canonical evidence of executed testcases.
- Mention explicitly when a discrepancy is only a reporting artifact.

Output required each run:
1) Classes processed in the batch
2) Files created/modified
3) Migration status per class (`ported-verified` / `incomplete`)
4) Classes marked `[x]` (mark automatically only when the targeted batch run passed)
5) Remaining pending count
