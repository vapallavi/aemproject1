package com.adobe.aem.guides.demo.core.listeners;


import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
    }
)
public class PageReplicationLogger implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PageReplicationLogger.class);

    @Override
    public void handleEvent(Event event) {
        ReplicationAction action = ReplicationAction.fromEvent(event);
        if (action == null) {
            return;
        }

        String path = action.getPath();
        ReplicationActionType type = action.getType();

        if (type == ReplicationActionType.ACTIVATE) {
            LOG.info("PAGE PUBLISHED: {}", path);
        } else if (type == ReplicationActionType.DEACTIVATE) {
            LOG.info("PAGE UNPUBLISHED: {}", path);
        }
    }
}
