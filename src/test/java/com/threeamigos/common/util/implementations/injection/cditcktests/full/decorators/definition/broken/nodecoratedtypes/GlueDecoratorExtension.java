/*
 * Copyright 2012, Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.definition.broken.nodecoratedtypes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class GlueDecoratorExtension implements Extension {

    public void registerDecorator(@Observes AfterBeanDiscovery event, BeanManager manager) {
        AnnotatedType<GlueDecorator> annotatedType = manager.createAnnotatedType(GlueDecorator.class);
        final BeanAttributes<GlueDecorator> attributes = manager.createBeanAttributes(annotatedType);
        final InjectionPoint delegateInjectionPoint = manager.createInjectionPoint(annotatedType.getConstructors().iterator()
                .next().getParameters().get(0));

        Decorator<GlueDecorator> decorator = new Decorator<GlueDecorator>() {

            @Override
            public Type getDelegateType() {
                return Glue.class;
            }

            @Override
            public Set<Annotation> getDelegateQualifiers() {
                return Collections.<Annotation> singleton(Any.Literal.INSTANCE);
            }

            @Override
            public Set<Type> getDecoratedTypes() {
                return Collections.emptySet();
            }

            @Override
            public Class<?> getBeanClass() {
                return GlueDecorator.class;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                final Decorator<GlueDecorator> self = this;
                return Collections.<InjectionPoint>singleton(new InjectionPoint() {
                    @Override
                    public Type getType() {
                        return delegateInjectionPoint.getType();
                    }

                    @Override
                    public Set<Annotation> getQualifiers() {
                        return delegateInjectionPoint.getQualifiers();
                    }

                    @Override
                    public Bean<?> getBean() {
                        return self;
                    }

                    @Override
                    public Member getMember() {
                        return delegateInjectionPoint.getMember();
                    }

                    @Override
                    public jakarta.enterprise.inject.spi.Annotated getAnnotated() {
                        return delegateInjectionPoint.getAnnotated();
                    }

                    @Override
                    public boolean isDelegate() {
                        return delegateInjectionPoint.isDelegate();
                    }

                    @Override
                    public boolean isTransient() {
                        return delegateInjectionPoint.isTransient();
                    }
                });
            }

            @Override
            public GlueDecorator create(CreationalContext<GlueDecorator> creationalContext) {
                return new GlueDecorator(null);
            }

            @Override
            public void destroy(GlueDecorator instance, CreationalContext<GlueDecorator> creationalContext) {
                creationalContext.release();
            }

            @Override
            public String getName() {
                return attributes.getName();
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return attributes.getQualifiers();
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return attributes.getScope();
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return attributes.getStereotypes();
            }

            @Override
            public Set<Type> getTypes() {
                return attributes.getTypes();
            }

            @Override
            public boolean isAlternative() {
                return attributes.isAlternative();
            }
        };
        event.addBean(decorator);
    }
}
