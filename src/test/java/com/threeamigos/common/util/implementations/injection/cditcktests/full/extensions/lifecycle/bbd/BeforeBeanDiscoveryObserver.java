package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd;

import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.metadata.support.annotated.AnnotatedMethodWrapper;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.metadata.support.annotated.AnnotatedTypeWrapper;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Baz;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Boss;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Pro;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;

import java.util.HashSet;
import java.util.Set;

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

        // Register @Skill as qualifier and mark level() as @Nonbinding via AnnotatedType metadata.
        beforeBeanDiscovery.addQualifier(new AnnotatedTypeWrapper<Skill>(beanManager.createAnnotatedType(Skill.class), true) {

            final Set<AnnotatedMethod<? super Skill>> methods = new HashSet<AnnotatedMethod<? super Skill>>();

            {
                for (AnnotatedMethod<? super Skill> method : super.getMethods()) {
                    if ("level".equals(method.getJavaMember().getName())) {
                        methods.add(new AnnotatedMethodWrapper<Skill>(
                                (AnnotatedMethod<Skill>) method,
                                this,
                                true,
                                new Nonbinding.Literal()));
                    } else {
                        methods.add(new AnnotatedMethodWrapper<Skill>(
                                (AnnotatedMethod<Skill>) method,
                                this,
                                true));
                    }
                }
            }

            @Override
            public Set<AnnotatedMethod<? super Skill>> getMethods() {
                return methods;
            }
        });
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
