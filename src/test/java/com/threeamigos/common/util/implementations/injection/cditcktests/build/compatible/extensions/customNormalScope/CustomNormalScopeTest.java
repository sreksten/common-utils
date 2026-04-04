package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandcontextcontroller.CommandContextController;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandcontextcontroller.CustomNormalScopeExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandcontextcontroller.IdService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor.CommandExecution;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor.CommandExecutor;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor.MyService;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomNormalScopeTest {

    private static final String COMMAND_CONTEXT_CONTROLLER_FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandcontextcontroller";

    private static final String COMMAND_EXECUTOR_FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor";

    @Test
    void commandContextController() {
        Syringe syringe = new Syringe(COMMAND_CONTEXT_CONTROLLER_FIXTURE_PACKAGE);
        syringe.addBuildCompatibleExtension(CustomNormalScopeExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            CommandContextController control = resolveReference(beanManager, CommandContextController.class);
            boolean activated = control.activate();
            assertTrue(activated);
            try {
                assertEquals(resolveReference(beanManager, IdService.class).get(),
                        resolveReference(beanManager, IdService.class).get());
            } finally {
                control.deactivate();
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void commandExecutor() {
        Syringe syringe = new Syringe(COMMAND_EXECUTOR_FIXTURE_PACKAGE);
        syringe.addBuildCompatibleExtension(
                com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor.CustomNormalScopeExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            CommandExecutor executor = resolveReference(beanManager, CommandExecutor.class);
            executor.execute(() -> {
                CommandExecution execution = resolveReference(beanManager, CommandExecution.class);
                com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor.IdService idService =
                        resolveReference(beanManager,
                                com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customNormalScope.customnormalscopetest.commandexecutor.IdService.class);

                resolveReference(beanManager, MyService.class).process();
                assertEquals(idService.get(), execution.getData().get("id"));
                assertNotNull(execution.getStartedAt());
            });
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
