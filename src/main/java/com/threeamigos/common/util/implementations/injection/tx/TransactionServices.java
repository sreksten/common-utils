package com.threeamigos.common.util.implementations.injection.tx;

/**
 * Minimal abstraction for transaction integration.
 */
public interface TransactionServices {

    /**
     * @return true if a transaction is currently active.
     */
    boolean isTransactionActive();

    /**
     * Registers synchronization callbacks with the current transaction.
     *
     * @param callbacks callbacks to invoke at transaction boundaries
     */
    void registerSynchronization(TransactionSynchronizationCallbacks callbacks);
}
