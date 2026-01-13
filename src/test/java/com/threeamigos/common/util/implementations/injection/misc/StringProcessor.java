package com.threeamigos.common.util.implementations.injection.misc;

public class StringProcessor implements Processor<String> {
    @Override
    public void process(String input) {
        System.out.println(input);
    }
}
