package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated;

import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.Alpha.AlphaLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.Bravo.BravoLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.Charlie.CharlieLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.support.AddForwardingAnnotatedTypeAction;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.support.annotated.AnnotatedTypeWrapper;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessSyntheticAnnotatedType;

import java.util.ArrayList;
import java.util.List;

import static com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.support.AddForwardingAnnotatedTypeAction.buildId;

public class ModifyingExtension implements Extension {

    private static final String BASE_ID_BRAVO = ModifyingExtension.class.getName() + "_b";
    private static final String BASE_ID_CHARLIE = ModifyingExtension.class.getName() + "_c";

    private AnnotatedType<Foo> aplha;
    private AnnotatedType<Foo> bravo;
    private AnnotatedType<Foo> charlie;
    private AnnotatedType<Bar> bar;
    private final List<AnnotatedType<Foo>> allFoo = new ArrayList<AnnotatedType<Foo>>();

    public void observeBeforeBeanDiscovery(@Observes BeforeBeanDiscovery event, final BeanManager beanManager) {
        new AddForwardingAnnotatedTypeAction<Foo>() {
            @Override
            public String getBaseId() {
                return BASE_ID_BRAVO;
            }

            @Override
            public AnnotatedType<Foo> delegate() {
                return new AnnotatedTypeWrapper<Foo>(
                        beanManager.createAnnotatedType(Foo.class),
                        false,
                        BravoLiteral.INSTANCE,
                        Any.Literal.INSTANCE
                );
            }
        }.perform(event);

        new AddForwardingAnnotatedTypeAction<Foo>() {
            @Override
            public String getBaseId() {
                return BASE_ID_CHARLIE;
            }

            @Override
            public AnnotatedType<Foo> delegate() {
                return new AnnotatedTypeWrapper<Foo>(
                        beanManager.createAnnotatedType(Foo.class),
                        false,
                        CharlieLiteral.INSTANCE
                );
            }
        }.perform(event);
    }

    public void observeProcessAnnotatedType(@Observes ProcessAnnotatedType<Foo> event) {
        if (!(event instanceof ProcessSyntheticAnnotatedType<?>)) {
            event.setAnnotatedType(new AnnotatedTypeWrapper<Foo>(event.getAnnotatedType(), false, AlphaLiteral.INSTANCE));
        }
    }

    public void observeAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        for (AnnotatedType<Foo> annotatedType : event.getAnnotatedTypes(Foo.class)) {
            allFoo.add(annotatedType);
        }
        bravo = event.getAnnotatedType(Foo.class, buildId(BASE_ID_BRAVO, Foo.class));
        charlie = event.getAnnotatedType(Foo.class, buildId(BASE_ID_CHARLIE, Foo.class));
        aplha = event.getAnnotatedType(Foo.class, null);
        bar = event.getAnnotatedType(Bar.class, null);
    }

    public AnnotatedType<Foo> getAplha() {
        return aplha;
    }

    public AnnotatedType<Foo> getBravo() {
        return bravo;
    }

    public AnnotatedType<Foo> getCharlie() {
        return charlie;
    }

    public List<AnnotatedType<Foo>> getAllFoo() {
        return allFoo;
    }

    public AnnotatedType<Bar> getBar() {
        return bar;
    }

}
