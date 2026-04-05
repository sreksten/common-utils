package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.processBean;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;

public class ProcessBeanObserver implements Extension {

    private static Bean<Cat> catBean;
    private static AnnotatedType<Cat> catAnnotatedType;
    private static Annotated catAnnotated;
    private static Bean<CatInterceptor> interceptor;
    private static Bean<AnimalDecorator> decorator;
    private static int catProcessBeanCount;

    private static Bean<Cowshed> cowBean;
    private static AnnotatedMethod<Cow> cowMethod;
    private static AnnotatedParameter<Cow> cowParameter;
    private static Annotated cowAnnotated;
    private static int cowShedProcessBeanCount;

    private static Bean<ChickenHutch> chickenBean;
    private static AnnotatedField<Chicken> chickenField;
    private static AnnotatedParameter<Chicken> chickenParameter;
    private static Annotated chickedAnnotated;
    private static int chickenHutchProcessBeanCount;

    private static ActionSequence catActionSeq = new ActionSequence();
    private static ActionSequence cowActionSeq = new ActionSequence();
    private static ActionSequence chickenActionSeq = new ActionSequence();

    public void observeCatManagedBean(@Observes ProcessManagedBean<Cat> event) {
        catBean = event.getBean();
        catAnnotatedType = event.getAnnotatedBeanClass();
        catAnnotated = event.getAnnotated();
        ProcessBeanObserver.catProcessBeanCount++;
        catActionSeq.add(ProcessManagedBean.class.getName());
    }

    public void observeCatBean(@Observes ProcessBean<Cat> event) {
        ProcessBeanObserver.catProcessBeanCount++;
    }

    public void observeCowProcessProducerMethod(@Observes ProcessProducerMethod<Cow, Cowshed> event) {
        cowBean = event.getBean();
        cowAnnotated = event.getAnnotated();
        cowMethod = event.getAnnotatedProducerMethod();
        cowParameter = event.getAnnotatedDisposedParameter();
        cowActionSeq.add(ProcessProducerMethod.class.getName());
    }

    public void observeCowShedProccesBean(@Observes ProcessBean<Cowshed> event) {
        ProcessBeanObserver.cowShedProcessBeanCount++;
    }

    public void observeChickenProcessProducerField(@Observes ProcessProducerField<Chicken, ChickenHutch> event) {
        chickenBean = event.getBean();
        chickenField = event.getAnnotatedProducerField();
        chickenParameter = event.getAnnotatedDisposedParameter();
        chickedAnnotated = event.getAnnotated();
        chickenActionSeq.add(ProcessProducerField.class.getName());
    }

    public void observeInterceptor(@Observes ProcessBean<CatInterceptor> event) {
        interceptor = event.getBean();
    }

    public void observeDecorator(@Observes ProcessBean<AnimalDecorator> event) {
        decorator = event.getBean();
    }

    public void observeChickenHutchProccesBean(@Observes ProcessBean<ChickenHutch> event) {
        ProcessBeanObserver.chickenHutchProcessBeanCount++;
    }

    public void observeCatBeanAttributes(@Observes ProcessBeanAttributes<Cat> event) {
        catActionSeq.add(ProcessBeanAttributes.class.getName());
    }

    public void observeCowBeanAttributes(@Observes ProcessBeanAttributes<Cow> event) {
        cowActionSeq.add(ProcessBeanAttributes.class.getName());
    }

    public void observeChickenBeanAttributes(@Observes ProcessBeanAttributes<Chicken> event) {
        chickenActionSeq.add(ProcessBeanAttributes.class.getName());
    }

    public static void reset() {
        catBean = null;
        catAnnotatedType = null;
        catAnnotated = null;
        interceptor = null;
        decorator = null;
        catProcessBeanCount = 0;

        cowBean = null;
        cowMethod = null;
        cowParameter = null;
        cowAnnotated = null;
        cowShedProcessBeanCount = 0;

        chickenBean = null;
        chickenField = null;
        chickenParameter = null;
        chickedAnnotated = null;
        chickenHutchProcessBeanCount = 0;

        catActionSeq = new ActionSequence();
        cowActionSeq = new ActionSequence();
        chickenActionSeq = new ActionSequence();
    }

    public static int getCatProcessBeanCount() {
        return catProcessBeanCount;
    }

    public static int getCowShedProcessBeanCount() {
        return cowShedProcessBeanCount;
    }

    public static int getChickenHutchProcessBeanCount() {
        return chickenHutchProcessBeanCount;
    }

    public static ActionSequence getCatActionSeq() {
        return catActionSeq;
    }

    public static ActionSequence getCowActionSeq() {
        return cowActionSeq;
    }

    public static ActionSequence getChickenActionSeq() {
        return chickenActionSeq;
    }

    public static Bean<Cat> getCatBean() {
        return catBean;
    }

    public static AnnotatedType<Cat> getCatAnnotatedType() {
        return catAnnotatedType;
    }

    public static Annotated getCatAnnotated() {
        return catAnnotated;
    }

    public static Bean<Cowshed> getCowBean() {
        return cowBean;
    }

    public static AnnotatedMethod<Cow> getCowMethod() {
        return cowMethod;
    }

    public static AnnotatedParameter<Cow> getCowParameter() {
        return cowParameter;
    }

    public static Annotated getCowAnnotated() {
        return cowAnnotated;
    }

    public static AnnotatedField<Chicken> getChickenField() {
        return chickenField;
    }

    public static AnnotatedParameter<Chicken> getChickenParameter() {
        return chickenParameter;
    }

    public static Annotated getChickedAnnotated() {
        return chickedAnnotated;
    }

    public static Bean<ChickenHutch> getChickenBean() {
        return chickenBean;
    }

    public static Bean<CatInterceptor> getInterceptor() {
        return interceptor;
    }

    public static Bean<AnimalDecorator> getDecorator() {
        return decorator;
    }
}
