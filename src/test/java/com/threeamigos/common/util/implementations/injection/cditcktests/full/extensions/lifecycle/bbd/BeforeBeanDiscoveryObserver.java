package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd;

import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Baz;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Boss;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Pro;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;

/**
 * BeforeBeanDiscovery observer used by lifecycle.bbd tests.
 */
public class BeforeBeanDiscoveryObserver implements Extension {

    private static boolean observed;

    public static boolean isObserved() {
        return observed;
    }

    public static void setObserved(boolean observed) {
        BeforeBeanDiscoveryObserver.observed = observed;
    }

    public void addScope(@Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        setObserved(true);
        beforeBeanDiscovery.addScope(EpochScoped.class, false, false);
    }

    public void addQualifierByClass(@Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        setObserved(true);
        beforeBeanDiscovery.addQualifier(Tame.class);
    }

    public void addQualifierByAnnotatedType(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        setObserved(true);

        // Register @Skill as qualifier and mark level() as @Nonbinding.
        beforeBeanDiscovery.configureQualifier(Skill.class)
                .filterMethods(annotatedMethod -> "level".equals(annotatedMethod.getJavaMember().getName()))
                .findFirst()
                .ifPresent(annotatedMethodConfigurator -> annotatedMethodConfigurator.add(Nonbinding.Literal.INSTANCE));
    }

    public void addAnnotatedType(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        event.addAnnotatedType(Boss.class, BeforeBeanDiscoveryObserver.class.getName() + ":" + Boss.class.getName());

        event.addAnnotatedType(Baz.class, BeforeBeanDiscoveryObserver.class.getName() + ":" + Baz.class.getName())
                .add(Pro.ProLiteral.INSTANCE)
                .add(RequestScoped.Literal.INSTANCE)
                .filterFields(annotatedField -> annotatedField.getJavaMember().getType().equals(Instance.class))
                .findFirst()
                .get()
                .add(InjectLiteral.INSTANCE)
                .add(Pro.ProLiteral.INSTANCE);
    }
}
