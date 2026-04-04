package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.builtin.event.complex;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class Observers {

    private final Sequence sequence = new Sequence();

    @Ordered(20)
    void observe1(@Observes @Bar @Baz Payload event) {
        sequence.add("third");
    }

    @Ordered(-10)
    void observe2(@Observes @Bar @Baz Payload event) {
        sequence.add("first");
    }

    void observe3(@Observes @Bar @Baz Payload event) {
        sequence.add("second");
    }

    public Sequence getSequence() {
        return sequence;
    }

    public static class Sequence {

        private final List<String> data = new ArrayList<String>();

        public void add(String action) {
            data.add(action);
        }

        public List<String> getData() {
            return Collections.unmodifiableList(new ArrayList<String>(data));
        }
    }
}
