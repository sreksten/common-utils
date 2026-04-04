package com.threeamigos.common.util.implementations.injection.cditcktests.event.implicit;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImplicitEventTest {

    private static final TypeLiteral<Event<StudentRegisteredEvent>> STUDENT_REGISTERED_EVENT_LITERAL =
            new TypeLiteral<Event<StudentRegisteredEvent>>() {
            };
    private static final TypeLiteral<Event<CourseFullEvent>> COURSE_FULL_EVENT_LITERAL =
            new TypeLiteral<Event<CourseFullEvent>>() {
            };
    private static final TypeLiteral<Event<AwardEvent>> AWARD_EVENT_LITERAL =
            new TypeLiteral<Event<AwardEvent>>() {
            };

    @Test
    void testImplicitEventExistsForEachEventType() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, STUDENT_REGISTERED_EVENT_LITERAL).size());
            assertEquals(1, getBeans(syringe, COURSE_FULL_EVENT_LITERAL).size());
            assertEquals(1, getBeans(syringe, AWARD_EVENT_LITERAL).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testImplicitEventHasAllExplicitBindingTypes() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, AWARD_EVENT_LITERAL, Any.Literal.INSTANCE, new HonorsLiteral()).size());
        } finally {
            syringe.shutdown();
        }
    }

    // Intentionally no @Test to mirror original TCK class.
    void testImplicitEventHasAnyBinding() {
        Syringe syringe = newSyringe();
        try {
            assertTrue(getUniqueBean(syringe, STUDENT_REGISTERED_EVENT_LITERAL).getQualifiers().contains(Any.Literal.INSTANCE));
            assertTrue(getUniqueBean(syringe, COURSE_FULL_EVENT_LITERAL).getQualifiers().contains(Any.Literal.INSTANCE));
            assertTrue(getUniqueBean(syringe, AWARD_EVENT_LITERAL).getQualifiers().contains(Any.Literal.INSTANCE));
            assertTrue(getUniqueBean(syringe, AWARD_EVENT_LITERAL, new HonorsLiteral()).getQualifiers().contains(Any.Literal.INSTANCE));
            assertTrue(getUniqueBean(syringe, AWARD_EVENT_LITERAL, Any.Literal.INSTANCE, new HonorsLiteral()) ==
                    getUniqueBean(syringe, AWARD_EVENT_LITERAL, new HonorsLiteral()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testImplicitEventHasDependentScope() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(Dependent.class, getUniqueBean(syringe, STUDENT_REGISTERED_EVENT_LITERAL).getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testImplicitEventHasNoName() {
        Syringe syringe = newSyringe();
        try {
            assertNull(getUniqueBean(syringe, STUDENT_REGISTERED_EVENT_LITERAL).getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testImplicitEventHasImplementation() {
        Syringe syringe = newSyringe();
        try {
            StudentDirectory directory = contextualReference(syringe, StudentDirectory.class);
            directory.reset();
            Registration registration = contextualReference(syringe, Registration.class);
            assertNotNull(registration.getInjectedCourseFullEvent());
            assertNotNull(registration.getInjectedStudentRegisteredEvent());

            Event<StudentRegisteredEvent> event = registration.getInjectedStudentRegisteredEvent();
            Student student = new Student("Dan");
            event.fire(new StudentRegisteredEvent(student));

            assertEquals(1, directory.getStudents().size());
            assertTrue(directory.getStudents().contains(student));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testImplicitEventIsPassivationCapable() throws IOException, ClassNotFoundException {
        Syringe syringe = newSyringe();
        try {
            Registration registration = contextualReference(syringe, Registration.class);
            Event<StudentRegisteredEvent> event = registration.getInjectedStudentRegisteredEvent();
            assertTrue(Serializable.class.isAssignableFrom(event.getClass()));

            byte[] serializedEvent = passivate(event);
            @SuppressWarnings("unchecked")
            Event<StudentRegisteredEvent> eventCopy = (Event<StudentRegisteredEvent>) activate(serializedEvent);

            StudentDirectory directory = contextualReference(syringe, StudentDirectory.class);
            directory.reset();
            Student student = new Student("Dan");
            eventCopy.fire(new StudentRegisteredEvent(student));

            assertEquals(1, directory.getStudents().size());
            assertTrue(directory.getStudents().contains(student));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Registration.class, StudentDirectory.class,
                Awards.class, Student.class, StudentRegisteredEvent.class, Course.class, CourseFullEvent.class,
                AwardEvent.class, Honors.class, HonorsLiteral.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        ((BeanManagerImpl) syringe.getBeanManager()).getContextManager().activateRequest();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Set<Bean<T>> getBeans(Syringe syringe, TypeLiteral<T> type, java.lang.annotation.Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(type.getType(), qualifiers);
    }

    private <T> Bean<T> getUniqueBean(Syringe syringe, TypeLiteral<T> type, java.lang.annotation.Annotation... qualifiers) {
        Set<Bean<T>> beans = getBeans(syringe, type, qualifiers);
        return (Bean<T>) syringe.getBeanManager().resolve((Set) beans);
    }

    private <T> T contextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }

    private byte[] passivate(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(baos);
        outputStream.writeObject(object);
        outputStream.flush();
        return baos.toByteArray();
    }

    private Object activate(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return inputStream.readObject();
    }

    @Dependent
    static class Registration {
        @Inject
        @Any
        private Event<StudentRegisteredEvent> studentRegisteredEvent;

        private Event<CourseFullEvent> courseFullEvent;

        @Inject
        Registration(Event<CourseFullEvent> courseFullEvent) {
            this.courseFullEvent = courseFullEvent;
        }

        Event<StudentRegisteredEvent> getInjectedStudentRegisteredEvent() {
            return studentRegisteredEvent;
        }

        Event<CourseFullEvent> getInjectedCourseFullEvent() {
            return courseFullEvent;
        }
    }

    @RequestScoped
    static class StudentDirectory {
        private final Set<Student> students = new java.util.HashSet<Student>();

        void addStudent(@Observes @Any StudentRegisteredEvent event) {
            students.add(event.getStudent());
        }

        Set<Student> getStudents() {
            return students;
        }

        void reset() {
            students.clear();
        }
    }

    @Dependent
    static class Awards {
        @Inject
        @Any
        @Honors
        Event<AwardEvent> honorsAwardEvent;

        void grantHonorsStatus(Student student) {
            honorsAwardEvent.fire(new AwardEvent(student));
        }
    }

    static class Student {
        private final String name;

        Student(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class StudentRegisteredEvent {
        private final Student student;

        StudentRegisteredEvent(Student student) {
            this.student = student;
        }

        Student getStudent() {
            return student;
        }
    }

    static class Course {
    }

    static class CourseFullEvent {
        private final Course course;

        CourseFullEvent(Course course) {
            this.course = course;
        }

        Course getCourse() {
            return course;
        }
    }

    static class AwardEvent {
        private final Student student;

        AwardEvent(Student student) {
            this.student = student;
        }

        Student getStudent() {
            return student;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Honors {
    }

    public static class HonorsLiteral extends AnnotationLiteral<Honors> implements Honors {
        private static final long serialVersionUID = 1L;
    }
}
