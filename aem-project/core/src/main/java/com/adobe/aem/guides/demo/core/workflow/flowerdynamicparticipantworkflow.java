package com.adobe.aem.guides.demo.core.workflow;


import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ParticipantStepChooser.class,
    property = {
        ParticipantStepChooser.SERVICE_PROPERTY_LABEL + "=flowerdynamicparticipantworkflow"
    }
)
public class flowerdynamicparticipantworkflow implements ParticipantStepChooser {

    private static final Logger LOG = LoggerFactory.getLogger(flowerdynamicparticipantworkflow.class);

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args)
            throws WorkflowException {

        // Example logic: assign to the workflow initiator
        String initiator = workItem.getWorkflow().getInitiator();
        LOG.info("Assigning to workflow initiator: {}", initiator);

        // You can put any logic here:
        // - Check payload path
        // - Read a page property
        // - Look at previous step history
        // - Use arguments from the step configuration (args)

        return initiator;   // or return a group ID like "content-approvers"
    }
}