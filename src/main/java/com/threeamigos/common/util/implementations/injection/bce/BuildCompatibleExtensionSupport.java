package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Support contract for Build Compatible Extensions (BCE).
 *
 * <p>This class documents the currently supported BCE phase subset,
 * how BCE phases map to Syringe lifecycle checkpoints, and which BCE APIs are
 * intentionally unsupported.
 */
public final class BuildCompatibleExtensionSupport {

    private BuildCompatibleExtensionSupport() {
    }

    /**
     * Initial BCE subset currently supported.
     */
    public enum SupportedPhase {
        DISCOVERY,
        ENHANCEMENT,
        REGISTRATION,
        SYNTHESIS,
        VALIDATION
    }

    /**
     * Syringe lifecycle checkpoints to which BCE phases are mapped.
     */
    public enum SyringeCheckpoint {
        AFTER_BEFORE_BEAN_DISCOVERY,
        AFTER_PROCESS_ANNOTATED_TYPES,
        AFTER_PROCESS_BEAN_EVENTS,
        AFTER_AFTER_BEAN_DISCOVERY,
        BEFORE_AFTER_DEPLOYMENT_VALIDATION
    }

    private static final List<SupportedPhase> SUPPORTED_PHASE_ORDER = Collections.unmodifiableList(
        Arrays.asList(
            SupportedPhase.DISCOVERY,
            SupportedPhase.ENHANCEMENT,
            SupportedPhase.REGISTRATION,
            SupportedPhase.SYNTHESIS,
            SupportedPhase.VALIDATION
        )
    );

    private static final Map<SupportedPhase, SyringeCheckpoint> PHASE_TO_CHECKPOINT;

    static {
        Map<SupportedPhase, SyringeCheckpoint> mapping = new LinkedHashMap<>();
        mapping.put(SupportedPhase.DISCOVERY, SyringeCheckpoint.AFTER_BEFORE_BEAN_DISCOVERY);
        mapping.put(SupportedPhase.ENHANCEMENT, SyringeCheckpoint.AFTER_PROCESS_ANNOTATED_TYPES);
        mapping.put(SupportedPhase.REGISTRATION, SyringeCheckpoint.AFTER_PROCESS_BEAN_EVENTS);
        mapping.put(SupportedPhase.SYNTHESIS, SyringeCheckpoint.AFTER_AFTER_BEAN_DISCOVERY);
        mapping.put(SupportedPhase.VALIDATION, SyringeCheckpoint.BEFORE_AFTER_DEPLOYMENT_VALIDATION);
        PHASE_TO_CHECKPOINT = Collections.unmodifiableMap(mapping);
    }

    private static final Set<String> UNSUPPORTED_BCE_APIS = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "Full spec-coverage guarantees for all language model edge cases",
            "Additional phase-specific model/config parameters beyond current service/invoker/synthesis support",
            "Additional BCE APIs beyond currently supported phase/synthesis/invoker/runtime surfaces"
        ))
    );

    public static List<SupportedPhase> supportedPhasesInOrder() {
        return SUPPORTED_PHASE_ORDER;
    }

    public static Map<SupportedPhase, SyringeCheckpoint> phaseToCheckpointMapping() {
        return PHASE_TO_CHECKPOINT;
    }

    public static Set<String> unsupportedApis() {
        return UNSUPPORTED_BCE_APIS;
    }

    public static boolean hasSupportedPhaseAnnotation(AnnotatedElement element) {
        if (element == null) {
            return false;
        }
        return AnnotationsEnum.hasDiscoveryAnnotation(element) ||
            AnnotationsEnum.hasEnhancementAnnotation(element) ||
            AnnotationsEnum.hasRegistrationAnnotation(element) ||
            AnnotationsEnum.hasSynthesisAnnotation(element) ||
            AnnotationsEnum.hasValidationAnnotation(element);
    }
}
