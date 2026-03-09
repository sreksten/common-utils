package com.threeamigos.common.util.implementations.injection.wildfly;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * WildFly Extension for the Syringe CDI container.
 *
 * <p>This class implements the WildFly {@link Extension} interface to register
 * the "syringe" subsystem.
 *
 * @author Stefano Reksten
 */
public class SyringeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "syringe";
    public static final String NAMESPACE = "urn:jboss:domain:syringe:1.0";

    private static final ModelVersion VERSION = ModelVersion.create(1, 0, 0);

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("syringe");

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, VERSION);
        registration.registerSubsystemModel(new SyringeSubsystemDefinition());
        registration.registerXMLElementWriter(new SyringeParser());
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, new SyringeParser());
    }

    /**
     * Definition of the syringe subsystem.
     */
    private static class SyringeSubsystemDefinition extends SimpleResourceDefinition {
        SyringeSubsystemDefinition() {
            super(PathElement.pathElement("subsystem", SUBSYSTEM_NAME),
                    new NonResolvingResourceDescriptionResolver(),
                    new SyringeSubsystemAdd(),
                    new SyringeSubsystemRemove());
        }

        @Override
        public void registerOperations(org.jboss.as.controller.registry.ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        }
    }

    /**
     * Handler for adding the syringe subsystem.
     */
    private static class SyringeSubsystemAdd extends AbstractAddStepHandler {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final SyringeService service = new SyringeService();
            context.getServiceTarget()
                    .addService(SERVICE_NAME, service)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            context.addStep(new AbstractDeploymentChainStep() {
                @Override
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new SyringeDependencyProcessor());
                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS, new SyringeDeploymentProcessor());
                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WELD_BEAN_MANAGER, new SyringeJndiBinderProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    /**
     * Handler for removing the syringe subsystem.
     */
    private static class SyringeSubsystemRemove extends AbstractRemoveStepHandler {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
            context.removeService(SERVICE_NAME);
        }
    }

    /**
     * XML parser and marshaller for the syringe subsystem.
     */
    private static class SyringeParser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            ModelNode address = new ModelNode();
            address.add("subsystem", SUBSYSTEM_NAME);
            address.protect();

            ModelNode addOperation = new ModelNode();
            addOperation.get("operation").set("add");
            addOperation.get("address").set(address);
            list.add(addOperation);

            while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                // Parse child elements if any
                reader.next();
            }
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(SyringeExtension.NAMESPACE, false);
            writer.writeEndElement();
        }
    }
}
