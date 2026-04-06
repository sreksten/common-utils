package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.parameters;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class Thinker {

    private final List<Idea> ideas = new ArrayList<Idea>();

    public Idea think() {
        Idea idea = new Idea(UUID.randomUUID().toString());
        ideas.add(idea);
        return idea;
    }

    public void forget(Idea idea) {
        ideas.remove(idea);
    }

    public List<Idea> getIdeas() {
        return ideas;
    }
}
