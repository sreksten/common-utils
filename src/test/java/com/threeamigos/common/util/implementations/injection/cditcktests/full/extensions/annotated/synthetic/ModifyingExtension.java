/*
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.synthetic;

import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.synthetic.support.annotated.AnnotatedTypeWrapper;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessSyntheticAnnotatedType;

public class ModifyingExtension implements Extension {

    public <T> void modify(@Observes ProcessSyntheticAnnotatedType<T> event) {
        Class<T> clazz = event.getAnnotatedType().getJavaClass();
        if (Orange.class.equals(clazz)) {
            event.veto();
        } else if (Apple.class.equals(clazz)) {
            event.setAnnotatedType(new AnnotatedTypeWrapper<T>(event.getAnnotatedType(), true, Fresh.Literal.INSTANCE));
        }
    }
}
