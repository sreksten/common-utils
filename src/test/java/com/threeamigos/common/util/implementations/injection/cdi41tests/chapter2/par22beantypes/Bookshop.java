package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * During discovery, as we miss a META-INF/beans.xml file, the search is conducted in
 * {@link BeanArchiveMode} IMPLICIT,
 * that is, to be discovered, a bean should have a CDI 4.1 related annotation.
 */
@ApplicationScoped
public class Bookshop extends Business implements Shop<Book> {
}
