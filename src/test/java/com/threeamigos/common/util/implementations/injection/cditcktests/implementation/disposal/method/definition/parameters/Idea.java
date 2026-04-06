package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.parameters;

public class Idea {

    private final String summary;

    public Idea(String summary) {
        this.summary = summary;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Idea other = (Idea) obj;
        if (summary == null) {
            return other.summary == null;
        }
        return summary.equals(other.summary);
    }
}
