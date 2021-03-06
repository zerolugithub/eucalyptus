/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.instanceusage;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.SimpleQueueClientManager;
import com.eucalyptus.portal.awsusage.QueuedEvent;
import com.eucalyptus.portal.awsusage.QueuedEvents;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.BillingActivityException;
import com.eucalyptus.portal.workflow.InstanceLog;
import com.eucalyptus.portal.workflow.InstanceLogActivities;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@ComponentPart(Portal.class)
public class InstanceLogActivitiesImpl implements InstanceLogActivities {
  private static Logger LOG     =
          Logger.getLogger(  InstanceLogActivitiesImpl.class );

  // key: account_id, value: map of (key: aggregation_key (e.g., instance type, platform, ... ), value: SQS queue)
  @Override
  public Map<String, String> distributeEvents(String globalQueue) throws BillingActivityException {
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(globalQueue, true).stream()
              .map( m -> QueuedEvents.MessageToEvent.apply(m.getBody()) )
              .filter( e -> e != null )
              .collect(Collectors.toList())
      );
    }catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    final Map<String, List<QueuedEvent>> accountEvents =
            events.stream()
                    .filter(e -> "InstanceUsage".equals(e.getEventType()))
                    .collect( groupingBy( e -> e.getAccountId() ,
                            Collectors.mapping( Function.identity() ,
                                    Collectors.toList())) );

    final Map<String, String> queueMap = Maps.newHashMap();
    for (final String accountId : accountEvents.keySet() ) {
      try {
        final String queueName = String.format("%s-instances-%s", accountId,
                UUID.randomUUID().toString().substring(0, 13));
        // create a new temporary queue
        sqClient.createQueue(
                queueName,
                Maps.newHashMap(
                        ImmutableMap.of(
                                "MessageRetentionPeriod", "120",
                                "MaximumMessageSize", "4096",
                                "VisibilityTimeout", "10")
                ));
        accountEvents.get(accountId).stream()
                .forEach(
                        e -> {
                          try {
                            sqClient.sendMessage(queueName, QueuedEvents.EventToMessage.apply(e));
                          } catch (final Exception ex) {
                            ;
                          }
                        }
                );
        queueMap.put(accountId, queueName);
      } catch (final Exception ex) {
        LOG.error("Failed to copy SQS message into a new queue", ex);
      }
    }

    return queueMap;
  }

  @Override
  public void persist(final String accountId, final String queueName) throws BillingActivityException {
    final SimpleQueueClientManager sqClient = SimpleQueueClientManager.getInstance();
    final List<QueuedEvent> events = Lists.newArrayList();
    try {
      events.addAll(sqClient.receiveAllMessages(queueName, false).stream()
              .map(m -> QueuedEvents.MessageToEvent.apply(m.getBody()))
              .filter(e -> e != null)
              .collect(Collectors.toList())
      );
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to receive queue messages", ex);
    }

    try {
      final List<InstanceLog> logs = InstanceLogReaders.readLogs(events).stream()
              .map( e -> e.build() )
              .filter( e -> e.isPresent() )
              .map( e -> e.get() )
              .collect(Collectors.toList());
      InstanceLogs.getInstance().append(logs);
    } catch (final Exception ex) {
      LOG.error("Failed to persist instance hour record", ex);
    }
  }

  @Override
  public void deleteQueues(List<String> queues) throws BillingActivityException {
    for (final String queue : queues) {
      try {
        SimpleQueueClientManager.getInstance().deleteQueue(queue);
      } catch (final Exception ex) {
        LOG.error("Failed to delete the temporary queue (" + queue +")", ex);
      }
    }
  }
}
