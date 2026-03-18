package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par31managedbeans;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par31managedbeans.bullet1.InvalidNormalScopedPublicFieldBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par31managedbeans.bullet2.InvalidGenericNormalScopedBean;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("3.1 - Managed beans")
public class ManagedBeansTest {

    /**
     * If a managed bean has a non-static public field, its scope must be a pseudo-scope (for example,
     * @Dependent or @Singleton). If a managed bean with a non-static public field declares a normal scope,
     * the container automatically detects the problem and treats it as a definition error.
     */
    @Test
    @DisplayName("3.1 - Managed bean with non-static public field and normal scope is a definition error")
    void managedBeanWithNonStaticPublicFieldAndNormalScopeIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidNormalScopedPublicFieldBean.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If the managed bean class is a generic type, it must have scope @Dependent. If a managed bean with a
     * parameterized bean class declares any scope other than @Dependent, the container automatically
     * detects the problem and treats it as a definition error.
     */
    @Test
    @DisplayName("3.1 - Generic managed bean with non-@Dependent scope is a definition error")
    void genericManagedBeanWithNonDependentScopeIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidGenericNormalScopedBean.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

}
