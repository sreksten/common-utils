In WildFly, modules are stored in a specific directory structure that mirrors the module's name. For the module name defined in the roadmap (`com.threeamigos.common.util`), the `module.xml` must be located at:

**Path:**
`${WILDFLY_HOME}/modules/system/layers/base/com/threeamigos/common/util/main/module.xml`

### Recommended Directory Structure

You should create the following directory structure inside your WildFly installation:

```text
${WILDFLY_HOME}/
└── modules/
    └── system/
        └── layers/
            └── base/
                └── com/
                    └── threeamigos/
                        └── common/
                            └── util/
                                └── main/
                                    ├── common-utils.jar  <-- Your Syringe JAR
                                    └── module.xml        <-- The module configuration
```

### Key Points:
*   **The `main` folder:** This is the default "slot" for modules in WildFly. If you don't specify a slot when depending on a module, WildFly looks for the `main` directory.
*   **Case Sensitivity:** Ensure the directory names match the case of your package/module name exactly (usually lowercase for module paths).
*   **Layer/Base:** While you can put modules in `${WILDFLY_HOME}/modules/`, it is standard practice for system-level integrations to reside under `system/layers/base/`.
*   **Permissions:** After copying the files, ensure the WildFly process has read permissions for the new directories and files.

### Contents of `module.xml` (Reminder)
The `module.xml` file itself tells WildFly which JARs to include and what other modules it depends on:

```xml
<module xmlns="urn:jboss:module:1.3" name="com.threeamigos.common.util">
    <resources>
        <resource-root path="common-utils.jar"/>
    </resources>
    <dependencies>
        <module name="jakarta.enterprise.api"/>
        <module name="jakarta.inject.api"/>
        <module name="jakarta.servlet.api"/>
    </dependencies>
</module>
```

By placing it here, you make Syringe available to any other WildFly subsystem or any deployment that declares a dependency on `com.threeamigos.common.util`.

---

WildFly needs to know about your new extension and subsystem. Even though we have implemented the code, the server won't execute it unless it is registered in the configuration.

There are **two mandatory changes** you need to make in your `standalone.xml`:

---

### 1. Register the Extension Module
In the `<extensions>` section (usually at the very top of the file), add the module we created:

```xml
<extensions>
    ...
    <extension module="com.threeamigos.common.util"/>
</extensions>
```
*   **Why?** This tells WildFly to look for a `module.xml` in `${WILDFLY_HOME}/modules/system/layers/base/com/threeamigos/common/util/main/` and load the `SyringeExtension` class specified there.

---

### 2. Add the Subsystem to the Profile
In the `<profile>` section (where all other subsystems like `logging`, `undertow`, or `weld` are), add the `syringe` subsystem:

```xml
<profile>
    ...
    <subsystem xmlns="urn:jboss:domain:syringe:1.0">
        <!-- Currently, no extra configuration is required, but the tag must exist -->
    </subsystem>
</profile>
```
*   **Why?** This triggers the `SyringeSubsystemAdd` handler we implemented, which registers the `SyringeService` and the **Deployment Unit Processors (DUPs)**. Without this, your DUPs won't run, and deployments won't get a Syringe container.

---

### Summary Checklist Before Starting WildFly:
1.  **JAR Placed:** `common-utils.jar` is in `${WILDFLY_HOME}/modules/system/layers/base/com/threeamigos/common/util/main/`.
2.  **`module.xml` Correct:** The file exists and points to your JAR and the `SyringeExtension` class.
3.  **`standalone.xml` Modified:** Both the `<extension>` and the `<subsystem>` are added.

### A Note on the Weld Subsystem
If you want to test **Syringe instead of Weld**, you might want to:
*   **Remove or comment out** the `<subsystem xmlns="urn:jboss:domain:weld:4.0"/>` in `standalone.xml`.
*   If both are present, they might conflict over the `java:comp/BeanManager` JNDI binding or over which one should handle a deployment (unless you explicitly tell WildFly which one to use via `jboss-deployment-structure.xml`).

For initial testing, it's safer to use a profile where **Weld is removed**, ensuring that Syringe is the sole CDI provider for your applications.
