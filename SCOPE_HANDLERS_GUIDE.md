# InjectorImpl Scope Handlers Guide

This guide explains how to use the different scope handlers available in InjectorImpl and how to create custom scope handlers for your application's needs.

## Table of Contents
- [Built-in Scopes](#built-in-scopes)
- [Standard CDI Scopes](#standard-cdi-scopes)
- [Custom Scope Handlers](#custom-scope-handlers)
- [Web Application Example](#web-application-example)
- [Best Practices](#best-practices)

---

## Built-in Scopes

### @Singleton (JSR-330)

**Automatically registered** - One instance per injector.

```java
import javax.inject.Singleton;

@Singleton
public class DatabaseConnection {
    // Single instance shared across entire application
}
```

**Behavior:**
- Thread-safe using double-checked locking
- Instance created on first injection
- Destroyed during JVM shutdown or `injector.shutdown()`
- `@PreDestroy` methods invoked on shutdown

**Usage:**
```java
InjectorImpl injector = new InjectorImpl("com.myapp");
DatabaseConnection db1 = injector.inject(DatabaseConnection.class);
DatabaseConnection db2 = injector.inject(DatabaseConnection.class);
assert db1 == db2; // Same instance
```

---

### @ApplicationScoped (CDI)

**Automatically registered** - Behaves identically to `@Singleton`.

```java
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigurationService {
    // Single instance for the entire application lifecycle
}
```

**Behavior:**
- Shares the same scope handler as `@Singleton`
- Thread-safe
- Destroyed during JVM shutdown
- `@PreDestroy` methods invoked on shutdown

**Why separate from @Singleton?**
- CDI standard compatibility
- Semantic clarity (application-level vs singleton pattern)
- Both resolve to the same instance pool

---

## Standard CDI Scopes

### @RequestScoped

**Requires manual registration** - One instance per thread (HTTP request in web apps).

#### Step 1: Register the scope handler

```java
import com.threeamigos.common.util.implementations.injection.InjectorImpl;
import com.threeamigos.common.util.implementations.injection.RequestScopeHandler;
import javax.enterprise.context.RequestScoped;

InjectorImpl injector = new InjectorImpl("com.myapp");

// Register RequestScoped with RequestScopeHandler
RequestScopeHandler requestHandler = new RequestScopeHandler();
injector.registerScope(RequestScoped.class, requestHandler);
```

#### Step 2: Define request-scoped beans

```java
@RequestScoped
public class ShoppingCart {
    private List<Item> items = new ArrayList<>();

    @PreDestroy
    public void cleanup() {
        System.out.println("Cart cleanup for this request");
    }
}
```

#### Step 3: Use in request processing

```java
// In your HTTP request handler (e.g., servlet, controller)
public void handleRequest(HttpServletRequest request) {
    try {
        // Each thread gets its own ShoppingCart instance
        ShoppingCart cart = injector.inject(ShoppingCart.class);
        cart.addItem(new Item("Product"));

        // Within same thread/request, same instance
        ShoppingCart sameCart = injector.inject(ShoppingCart.class);
        assert cart == sameCart;

    } finally {
        // Clean up request scope at end of request
        requestHandler.close(); // Invokes @PreDestroy on all request-scoped beans
    }
}
```

**Threading behavior:**
```java
// Main thread
RequestScoped cart1 = injector.inject(ShoppingCart.class);

// Different thread - gets different instance
Thread t = new Thread(() -> {
    RequestScoped cart2 = injector.inject(ShoppingCart.class);
    assert cart1 != cart2; // Different instances per thread
});
t.start();
```

---

### @SessionScoped

**Requires manual registration** - One instance per session ID.

#### Step 1: Register the scope handler

```java
import com.threeamigos.common.util.implementations.injection.SessionScopeHandler;
import javax.enterprise.context.SessionScoped;

SessionScopeHandler sessionHandler = new SessionScopeHandler();
injector.registerScope(SessionScoped.class, sessionHandler);
```

#### Step 2: Define session-scoped beans

```java
@SessionScoped
public class UserPreferences {
    private String theme = "light";
    private String language = "en";

    @PreDestroy
    public void cleanup() {
        System.out.println("User session ending");
    }
}
```

#### Step 3: Use with session management

```java
public void handleRequest(HttpServletRequest request) {
    try {
        // Set the current session ID for this thread
        String sessionId = request.getSession().getId();
        sessionHandler.setCurrentSession(sessionId);

        // Get session-scoped bean
        UserPreferences prefs = injector.inject(UserPreferences.class);

        // All requests in same session get same instance
        UserPreferences samePrefs = injector.inject(UserPreferences.class);
        assert prefs == samePrefs;

    } finally {
        // Don't close session handler on every request!
        // Only close when session expires
    }
}

// When session expires (e.g., timeout, logout)
public void onSessionExpired(String sessionId) {
    sessionHandler.setCurrentSession(sessionId);
    sessionHandler.close(); // Invokes @PreDestroy, cleans up session beans
}
```

**Multi-session behavior:**
```java
// Session 1
sessionHandler.setCurrentSession("session-123");
UserPreferences prefs1 = injector.inject(UserPreferences.class);

// Session 2 - gets different instance
sessionHandler.setCurrentSession("session-456");
UserPreferences prefs2 = injector.inject(UserPreferences.class);

assert prefs1 != prefs2; // Different instances per session
```

---

### @ConversationScoped

**Requires manual registration** - One instance per conversation ID.

Conversations are longer-lived than requests but shorter than sessions, typically used for multi-step workflows (wizards, checkout processes, etc.).

#### Step 1: Register the scope handler

```java
import com.threeamigos.common.util.implementations.injection.ConversationScopeHandler;
import javax.enterprise.context.ConversationScoped;

ConversationScopeHandler conversationHandler = new ConversationScopeHandler();
injector.registerScope(ConversationScoped.class, conversationHandler);
```

#### Step 2: Define conversation-scoped beans

```java
@ConversationScoped
public class CheckoutWizard {
    private Address shippingAddress;
    private PaymentInfo paymentInfo;
    private int currentStep = 1;

    @PreDestroy
    public void cleanup() {
        System.out.println("Conversation ended");
    }
}
```

#### Step 3: Use with conversation management

```java
public void startCheckout(String userId) {
    // Begin a new conversation
    String conversationId = "checkout-" + userId + "-" + System.currentTimeMillis();
    conversationHandler.beginConversation(conversationId);

    // Get conversation-scoped bean
    CheckoutWizard wizard = injector.inject(CheckoutWizard.class);
    wizard.setCurrentStep(1);

    // Store conversation ID in session for subsequent requests
    httpSession.setAttribute("conversationId", conversationId);
}

public void continueCheckout(HttpServletRequest request) {
    // Resume existing conversation
    String conversationId = (String) request.getSession().getAttribute("conversationId");
    conversationHandler.beginConversation(conversationId);

    // Same wizard instance across multiple requests
    CheckoutWizard wizard = injector.inject(CheckoutWizard.class);
    wizard.setCurrentStep(wizard.getCurrentStep() + 1);
}

public void completeCheckout(HttpServletRequest request) {
    String conversationId = (String) request.getSession().getAttribute("conversationId");

    // End conversation - invokes @PreDestroy and cleans up
    conversationHandler.endConversation(conversationId);
    request.getSession().removeAttribute("conversationId");
}
```

**Multi-conversation behavior:**
```java
// Conversation 1 (User A's checkout)
conversationHandler.beginConversation("checkout-userA-123");
CheckoutWizard wizardA = injector.inject(CheckoutWizard.class);

// Conversation 2 (User B's checkout)
conversationHandler.beginConversation("checkout-userB-456");
CheckoutWizard wizardB = injector.inject(CheckoutWizard.class);

assert wizardA != wizardB; // Different instances per conversation
```

---

## Custom Scope Handlers

You can create custom scope handlers for application-specific needs by implementing the `ScopeHandler` interface.

### Example: Custom TenantScopeHandler

```java
import com.threeamigos.common.util.interfaces.injection.ScopeHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TenantScopeHandler implements ScopeHandler {
    private final Map<String, Map<Class<?>, Object>> tenants = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
        tenants.putIfAbsent(tenantId, new ConcurrentHashMap<>());
    }

    @Override
    public <T> T get(Class<T> clazz, Supplier<T> provider) {
        String tenantId = currentTenant.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set");
        }

        Map<Class<?>, Object> beans = tenants.get(tenantId);
        return (T) beans.computeIfAbsent(clazz, k -> provider.get());
    }

    @Override
    public void close() throws Exception {
        String tenantId = currentTenant.get();
        if (tenantId != null) {
            tenants.remove(tenantId);
            currentTenant.remove();
        }
    }
}
```

### Register custom scope

```java
@Scope
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface TenantScoped {}

// Register
TenantScopeHandler tenantHandler = new TenantScopeHandler();
injector.registerScope(TenantScoped.class, tenantHandler);

// Use
@TenantScoped
public class TenantConfiguration {
    private String databaseUrl;
    private String theme;
}

// In application
tenantHandler.setCurrentTenant("tenant-acme");
TenantConfiguration config = injector.inject(TenantConfiguration.class);
```

---

## Web Application Example

Complete example for a web application using all scopes:

```java
public class WebApplicationStartup {
    private InjectorImpl injector;
    private RequestScopeHandler requestHandler;
    private SessionScopeHandler sessionHandler;

    public void initialize() {
        // Create injector
        injector = new InjectorImpl("com.myapp");

        // Register web scopes
        requestHandler = new RequestScopeHandler();
        sessionHandler = new SessionScopeHandler();

        injector.registerScope(RequestScoped.class, requestHandler);
        injector.registerScope(SessionScoped.class, sessionHandler);
    }

    public void handleHttpRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Set session context
            String sessionId = request.getSession().getId();
            sessionHandler.setCurrentSession(sessionId);

            // Inject controller (may have mixed-scope dependencies)
            UserController controller = injector.inject(UserController.class);
            controller.handleRequest(request, response);

        } finally {
            // Clean up request scope
            try {
                requestHandler.close();
            } catch (Exception e) {
                // Log error
            }
        }
    }

    public void onSessionExpired(String sessionId) {
        try {
            sessionHandler.setCurrentSession(sessionId);
            sessionHandler.close();
        } catch (Exception e) {
            // Log error
        }
    }

    public void shutdown() {
        injector.shutdown(); // Cleans up all scopes
    }
}

// Controller with mixed scopes
public class UserController {
    @Inject
    private DatabaseService dbService;  // @ApplicationScoped - shared

    @Inject
    private ShoppingCart cart;  // @RequestScoped - per request

    @Inject
    private UserPreferences prefs;  // @SessionScoped - per session

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        // dbService is singleton
        // cart is unique to this request
        // prefs is shared across all requests in this session
    }
}
```

---

## Best Practices

### 1. **Scope Selection**

- **@Singleton/@ApplicationScoped**: Stateless services, database connections, configuration
- **@RequestScoped**: Request-specific data, temporary state, HTTP request context
- **@SessionScoped**: User-specific state, shopping carts, user preferences
- **@ConversationScoped**: Multi-step workflows, wizards, checkout processes
- **Custom scopes**: Special lifecycle requirements (tenant isolation, transactions, etc.)

### 2. **Cleanup**

Always clean up scoped beans to prevent memory leaks:

```java
try {
    // Use beans
} finally {
    requestHandler.close();  // Always clean up
}
```

### 3. **Thread Safety**

- Singleton/ApplicationScoped: Must be thread-safe
- RequestScoped: Thread-confined, no synchronization needed
- SessionScoped: May need synchronization if session accessed by multiple threads

### 4. **@PreDestroy Usage**

Implement `@PreDestroy` for cleanup:

```java
@RequestScoped
public class DatabaseTransaction {
    private Connection conn;

    @PostConstruct
    public void begin() {
        conn = dataSource.getConnection();
        conn.setAutoCommit(false);
    }

    @PreDestroy
    public void cleanup() {
        if (conn != null) {
            try {
                conn.rollback();
                conn.close();
            } catch (SQLException e) {
                // Log error
            }
        }
    }
}
```

### 5. **Avoid Scope Mixing Issues**

Be careful when injecting longer-lived scopes into shorter-lived ones:

```java
// GOOD: Singleton depends on Singleton
@Singleton
public class ServiceA {
    @Inject private ServiceB serviceB;  // @Singleton - OK
}

// CAREFUL: Singleton depends on RequestScoped
@Singleton
public class ServiceA {
    @Inject private RequestScopedBean bean;  // Singleton will capture FIRST request's bean!
}

// FIX: Use Provider for shorter-lived dependencies
@Singleton
public class ServiceA {
    @Inject private Provider<RequestScopedBean> beanProvider;

    public void doWork() {
        RequestScopedBean bean = beanProvider.get();  // Gets current request's bean
    }
}
```

### 6. **Testing**

Test different scopes appropriately:

```java
@Test
void testRequestScoped() {
    InjectorImpl injector = new InjectorImpl("com.myapp");
    RequestScopeHandler handler = new RequestScopeHandler();
    injector.registerScope(RequestScoped.class, handler);

    try {
        // First request
        MyBean bean1 = injector.inject(MyBean.class);
        MyBean bean2 = injector.inject(MyBean.class);
        assertSame(bean1, bean2);  // Same within request

        handler.close();  // End request

        // Second request - new instance
        MyBean bean3 = injector.inject(MyBean.class);
        assertNotSame(bean1, bean3);  // Different across requests
    } finally {
        handler.close();
    }
}
```

---

## Summary

| Scope | Registration | Lifecycle | Thread Safety | Use Case |
|-------|-------------|-----------|---------------|----------|
| @Singleton | Automatic | Application | Thread-safe | Stateless services |
| @ApplicationScoped | Automatic | Application | Thread-safe | CDI compatibility |
| @RequestScoped | Manual | Per thread | Thread-confined | HTTP requests |
| @SessionScoped | Manual | Per session ID | Requires care | User state |
| @ConversationScoped | Manual | Per conversation ID | Requires care | Multi-step workflows |
| Custom | Manual | Custom | Depends | Special needs |

For more details, see the [InjectorImpl Javadoc](src/main/java/com/threeamigos/common/util/implementations/injection/InjectorImpl.java).
