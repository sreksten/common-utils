package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

@Decorator
public class DataAccessAuthorizationDecorator implements DataAccess {

    @Inject
    @Delegate
    DataAccess delegate;

    @Inject
    User user;

    public void save() {
        authorize("save");
        delegate.save();
    }

    public void delete() {
        authorize("delete");
        delegate.delete();
    }

    private void authorize(String action) {
        Object id = delegate.getId();
        Class<?> type = delegate.getDataType();
        if (!user.hasPermission(action, type, id)) {
            throw new NotAuthorizedException(action);
        }
    }

    public Class<?> getDataType() {
        return delegate.getDataType();
    }

    public Object getId() {
        return delegate.getId();
    }

    public Object load(Object id) {
        return delegate.load(id);
    }
}
