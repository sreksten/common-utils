package com.threeamigos.common.util.implementations.injection.cditcktests.event.fires;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FireEventTest {

    @Test
    void testBeanManagerFireEvent() {
        Syringe syringe = newSyringe();
        try {
            Billing billing = getContextualReference(syringe, Billing.class);
            billing.reset();

            MiniBar miniBar = new MiniBar();
            miniBar.stockNoNotify();
            syringe.getBeanManager().getEvent().select(MiniBar.class).fire(miniBar);
            assertTrue(billing.isActive());

            Item chocolate = miniBar.getItemByName("Chocolate");
            syringe.getBeanManager().getEvent().select(Item.class, new Lifted.LiftedLiteral()).fire(chocolate);
            assertEquals(5, billing.getCharge());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testTypeVariableEventTypeFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class, new Runnable() {
                @Override
                public void run() {
                    getContextualReference(syringe, Bar.class).<Integer>fireWithTypeVariable();
                }
            }::run);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDuplicateBindingsToFireEventFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class, new Runnable() {
                @Override
                public void run() {
                    syringe.getBeanManager().getEvent().select(
                            new Lifted.LiftedLiteral("a"),
                            new Lifted.LiftedLiteral("b")
                    ).fire(new Object());
                }
            }::run);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectedAnyEventCanFireEvent() {
        Syringe syringe = newSyringe();
        try {
            Billing billing = getContextualReference(syringe, Billing.class);
            billing.reset();

            Bean<MiniBar> miniBarBean = getUniqueBean(syringe, MiniBar.class);
            InjectionPoint eventInjection = findInjectionPoint(miniBarBean, "miniBarEvent");

            assertNotNull(eventInjection);
            assertEquals(1, eventInjection.getQualifiers().size());
            assertTrue(eventInjection.getQualifiers().contains(Any.Literal.INSTANCE));

            CreationalContext<MiniBar> miniBarCc = syringe.getBeanManager().createCreationalContext(miniBarBean);
            MiniBar miniBar = miniBarBean.create(miniBarCc);
            miniBar.stock();

            assertTrue(billing.isActive());
            assertEquals(16, billing.getMiniBarValue());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectedEventAcceptsEventObject() throws Exception {
        Syringe syringe = newSyringe();
        try {
            Billing billing = getContextualReference(syringe, Billing.class);
            billing.reset();

            Bean<MiniBar> miniBarBean = getUniqueBean(syringe, MiniBar.class);
            CreationalContext<MiniBar> miniBarCc = syringe.getBeanManager().createCreationalContext(miniBarBean);
            MiniBar miniBar = miniBarBean.create(miniBarCc);

            Field eventField = miniBar.getClass().getDeclaredField("miniBarEvent");
            ParameterizedType eventFieldType = (ParameterizedType) eventField.getGenericType();
            assertEquals(1, eventFieldType.getActualTypeArguments().length);
            assertEquals(MiniBar.class, eventFieldType.getActualTypeArguments()[0]);
            assertEquals(Event.class, eventFieldType.getRawType());

            Method fireMethod = null;
            @SuppressWarnings("unchecked")
            Class<Event<Item>> eventFieldClass = (Class<Event<Item>>) eventFieldType.getRawType();
            for (Method method : eventFieldClass.getMethods()) {
                if (method.getName().equals("fire") && !method.isSynthetic()) {
                    if (fireMethod != null) {
                        fail("Expecting exactly one method on Event named 'fire'");
                    }
                    fireMethod = method;
                }
            }
            if (fireMethod == null) {
                fail("Expecting exactly one method on Event named 'fire'");
            }

            assertEquals(1, fireMethod.getParameterTypes().length);
            assertEquals(1, fireMethod.getGenericParameterTypes().length);
            Type fireMethodArgumentType = fireMethod.getGenericParameterTypes()[0];
            @SuppressWarnings("rawtypes")
            Type eventClassParameterizedType = ((TypeVariable) fireMethod.getGenericParameterTypes()[0])
                    .getGenericDeclaration().getTypeParameters()[0];
            assertEquals(eventClassParameterizedType, fireMethodArgumentType);

            miniBar.stock();
            assertTrue(billing.isActive());
            assertEquals(16, billing.getMiniBarValue());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectedEventCanHaveBindings() {
        Syringe syringe = newSyringe();
        try {
            Billing billing = getContextualReference(syringe, Billing.class);
            billing.reset();

            Bean<MiniBar> miniBarBean = getUniqueBean(syringe, MiniBar.class);
            InjectionPoint eventInjection = findInjectionPoint(miniBarBean, "itemLiftedEvent");

            assertNotNull(eventInjection);
            assertEquals(1, eventInjection.getQualifiers().size());
            assertTrue(eventInjection.getQualifiers().contains(new Lifted.LiftedLiteral()));

            CreationalContext<MiniBar> miniBarCc = syringe.getBeanManager().createCreationalContext(miniBarBean);
            MiniBar miniBar = miniBarBean.create(miniBarCc);
            miniBar.stock();
            Item chocolate = miniBar.getItemByName("Chocolate");
            assertNotNull(chocolate);
            miniBar.liftItem(chocolate);
            assertEquals(chocolate.getPrice(), billing.getCharge());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectedEventCanSpecifyBindingsDynamically() {
        Syringe syringe = newSyringe();
        try {
            Billing billing = getContextualReference(syringe, Billing.class);
            billing.reset();
            Housekeeping housekeeping = getContextualReference(syringe, Housekeeping.class);

            Bean<MiniBar> miniBarBean = getUniqueBean(syringe, MiniBar.class);
            InjectionPoint eventInjection = findInjectionPoint(miniBarBean, "itemEvent");

            assertNotNull(eventInjection);
            assertEquals(1, eventInjection.getQualifiers().size());
            assertTrue(eventInjection.getQualifiers().contains(Any.Literal.INSTANCE));

            CreationalContext<MiniBar> miniBarCc = syringe.getBeanManager().createCreationalContext(miniBarBean);
            MiniBar miniBar = miniBarBean.create(miniBarCc);
            miniBar.stock();
            Item water = miniBar.liftItemByName("16 oz Water");
            miniBar.restoreItem(water);

            assertEquals(1, billing.getCharge());
            assertEquals(1, housekeeping.getItemsTainted().size());
            assertTrue(housekeeping.getItemsTainted().contains(water));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventProvidesMethodForFiringEventsWithCombinationOfTypeAndBindings() {
        Syringe syringe = newSyringe();
        try {
            DoggiePoints points = getContextualReference(syringe, DoggiePoints.class);
            points.reset();

            DogWhisperer master = getContextualReference(syringe, DogWhisperer.class);
            master.issueTamingCommand();
            assertEquals(1, points.getNumTamed());
            assertEquals(0, points.getNumPraiseReceived());

            master.givePraise();
            assertEquals(1, points.getNumTamed());
            assertEquals(1, points.getNumPraiseReceived());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventSelectedFiresAndObserversNotified() {
        Syringe syringe = newSyringe();
        try {
            Housekeeping houseKeeping = getContextualReference(syringe, Housekeeping.class);
            houseKeeping.reset();
            MiniBar miniBar = getContextualReference(syringe, MiniBar.class);

            Item chocolate = new Item("Chocolate", 5);
            Item crackers = new Item("Crackers", 2);

            miniBar.getItemEvent().fire(chocolate);
            assertEquals(1, houseKeeping.getItemActivity().size());
            assertEquals(chocolate, houseKeeping.getItemActivity().get(0));

            miniBar.getItemEvent().select(new Lifted.LiftedLiteral()).fire(crackers);
            assertEquals(2, houseKeeping.getItemActivity().size());
            assertEquals(crackers, houseKeeping.getItemActivity().get(1));
            assertEquals(1, houseKeeping.getItemsMissing().size());
            assertEquals(crackers, houseKeeping.getItemsMissing().iterator().next());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventFireThrowsExceptionIfEventObjectTypeContainsUnresolvableTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            MiniBar miniBar = getContextualReference(syringe, MiniBar.class);
            assertThrows(IllegalArgumentException.class, new Runnable() {
                @Override
                public void run() {
                    fireIllegalEvent(miniBar);
                }
            }::run);
        } finally {
            syringe.shutdown();
        }
    }

    private <T> void fireIllegalEvent(MiniBar miniBar) {
        miniBar.itemEvent.fire(new Item_Illegal<T>("12 oz Beer", 6));
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                FireEventTest.class,
                Billing.class,
                MiniBar.class,
                Housekeeping.class,
                Item.class,
                Lifted.class,
                Restored.class,
                Bar.class,
                Foo.class,
                Item_Illegal.class,
                DogWhisperer.class,
                DoggiePoints.class,
                TamingCommand.class,
                Praise.class,
                Tame.class,
                Role.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        ((BeanManagerImpl) syringe.getBeanManager()).getContextManager().activateRequest();
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> beanClass) {
        Set<Bean<?>> beans = (Set) syringe.getBeanManager().getBeans(beanClass);
        Bean<?> bean = syringe.getBeanManager().resolve(beans);
        return (Bean<T>) bean;
    }

    private InjectionPoint findInjectionPoint(Bean<?> bean, String memberName) {
        for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
            if (injectionPoint.getMember().getName().equals(memberName)) {
                return injectionPoint;
            }
        }
        return null;
    }

    @RequestScoped
    public static class Billing {
        private boolean active;
        private int charge;
        private int miniBarValue;
        private final Set<Item> itemsPurchased = new HashSet<Item>();

        void billForItem(@Observes @Lifted Item item) {
            if (itemsPurchased.add(item)) {
                charge += item.getPrice();
            }
        }

        int getCharge() {
            return charge;
        }

        int getMiniBarValue() {
            return miniBarValue;
        }

        boolean isActive() {
            return active;
        }

        void activate(@Observes @Any MiniBar minibar) {
            active = true;
            miniBarValue = 0;
            for (Item item : minibar.getItems()) {
                miniBarValue += item.getPrice();
            }
        }

        void reset() {
            active = false;
            itemsPurchased.clear();
            charge = 0;
            miniBarValue = 0;
        }
    }

    @Dependent
    public static class MiniBar {
        private final Set<Item> items = new HashSet<Item>();

        @Inject
        @Any
        Event<MiniBar> miniBarEvent;

        @Inject
        @Lifted
        Event<Item> itemLiftedEvent;

        @Inject
        @Any
        Event<Item> itemEvent;

        Event<Item> getItemEvent() {
            return itemEvent;
        }

        Set<Item> getItems() {
            return items;
        }

        Item getItemByName(String name) {
            for (Item item : items) {
                if (item.getName().equals(name)) {
                    return item;
                }
            }
            return null;
        }

        Item liftItemByName(String name) {
            Item item = getItemByName(name);
            if (item != null) {
                liftItem(item);
            }
            return item;
        }

        void liftItem(Item item) {
            if (!items.contains(item)) {
                throw new IllegalArgumentException("No such item");
            }
            itemLiftedEvent.fire(item);
            items.remove(item);
        }

        void restoreItem(Item item) {
            if (items.contains(item)) {
                throw new IllegalArgumentException("Item already restored");
            }
            itemEvent.select(new Restored.Literal()).fire(item);
        }

        void stock() {
            stockNoNotify();
            miniBarEvent.fire(this);
        }

        void stockNoNotify() {
            items.add(new Item("Chocolate", 5));
            items.add(new Item("16 oz Water", 1));
            items.add(new Item("Disposable Camera", 10));
        }
    }

    @RequestScoped
    static class Housekeeping {
        private final Set<Item> itemsTainted = new HashSet<Item>();
        private final Set<Item> itemsMissing = new HashSet<Item>();
        private final List<Item> itemActivity = new ArrayList<Item>();

        void onItemRemoved(@Observes @Lifted Item item) {
            itemsMissing.add(item);
            itemsTainted.remove(item);
        }

        void onItemRestored(@Observes @Restored Item item) {
            itemsMissing.remove(item);
            itemsTainted.add(item);
        }

        void onItemActivity(@Observes @Any Item item) {
            itemActivity.add(item);
        }

        Set<Item> getItemsTainted() {
            return itemsTainted;
        }

        Set<Item> getItemsMissing() {
            return itemsMissing;
        }

        List<Item> getItemActivity() {
            return itemActivity;
        }

        void minibarStocked(@Observes @Any MiniBar minibar) {
            reset();
        }

        void reset() {
            itemActivity.clear();
            itemsMissing.clear();
            itemsTainted.clear();
        }
    }

    public static class Item {
        private final String name;
        private final int price;

        public Item(String name, int price) {
            this.name = name;
            this.price = price;
        }

        String getName() {
            return name;
        }

        int getPrice() {
            return price;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Lifted {
        String value() default "";

        class LiftedLiteral extends AnnotationLiteral<Lifted> implements Lifted {
            private static final long serialVersionUID = 1L;
            private final String value;

            LiftedLiteral() {
                this("");
            }

            LiftedLiteral(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Restored {
        class Literal extends AnnotationLiteral<Restored> implements Restored {
            private static final long serialVersionUID = 1L;
        }
    }

    @Dependent
    public static class Bar {
        @Inject
        BeanManager beanManager;

        <T extends Number> void fireWithTypeVariable() {
            beanManager.getEvent().select(new TypeLiteral<Foo<T>>() {
            }).fire(new Foo<T>());
        }
    }

    static class Foo<T> {
    }

    public static class Item_Illegal<T> extends Item {
        Item_Illegal(String name, int price) {
            super(name, price);
        }
    }

    @Dependent
    public static class DogWhisperer {
        @Inject
        @Any
        @Tame
        @Role("Master")
        Event<TamingCommand> tamingEvent;

        @Inject
        @Any
        Event<Praise> praiseEvent;

        void issueTamingCommand() {
            tamingEvent.fire(new TamingCommand());
        }

        void givePraise() {
            praiseEvent.fire(new Praise());
        }
    }

    @RequestScoped
    static class DoggiePoints {
        private int numPraiseReceived;
        private int numTamed;

        void praiseReceived(@Observes @Any Praise praise) {
            numPraiseReceived++;
        }

        void tamed(@Observes @Tame @Role("Master") TamingCommand tamed) {
            numTamed++;
        }

        int getNumPraiseReceived() {
            return numPraiseReceived;
        }

        int getNumTamed() {
            return numTamed;
        }

        void reset() {
            numPraiseReceived = 0;
            numTamed = 0;
        }
    }

    static class TamingCommand {
    }

    static class Praise {
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Tame {
    }

    @Target({FIELD, PARAMETER, METHOD, TYPE})
    @Retention(RUNTIME)
    @Qualifier
    public @interface Role {
        String value();
    }
}
