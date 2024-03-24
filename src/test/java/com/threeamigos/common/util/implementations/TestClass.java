package com.threeamigos.common.util.implementations;

import com.threeamigos.common.util.interfaces.preferences.Preferences;

import java.beans.PropertyChangeListener;

public class TestClass implements Preferences {

    public static final String TEST_STRING = "My test string";
    public static final int TEST_VALUE = 50;
    public static final String JSON_REPRESENTATION = "{\"string\":\"" + TEST_STRING + "\",\"value\":" + TEST_VALUE + "}";

    private String string;
    private int value;

    public TestClass() {
    }

    public TestClass(String string, int value) {
        this.string = string;
        this.value = value;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
    }

    @Override
    public String getDescription() {
        return "TestClass Description";
    }

    @Override
    public void validate() {
    }

    @Override
    public void loadDefaultValues() {
        string = TEST_STRING;
        value = TEST_VALUE;
    }
}
