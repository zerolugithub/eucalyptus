/*
 * This code was generated by AWS Flow Framework Annotation Processor.
 * Refer to Amazon Simple Workflow Service documentation at http://aws.amazon.com/documentation/swf 
 *
 * Any changes made directly to this file will be lost when 
 * the code is regenerated.
 */
 package com.eucalyptus.loadbalancing.workflow;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;

/**
 * Generated from {@link com.eucalyptus.loadbalancing.workflow.UpgradeLoadBalancerWorkflow}. 
 * Used to start workflow executions or send signals from outside of the scope of a workflow.
 * Created through {@link UpgradeLoadBalancerWorkflowClientExternalFactory#getClient}.
 * <p>
 * When starting child workflow from a parent workflow use {@link UpgradeLoadBalancerWorkflowClient} instead.
 */
public interface UpgradeLoadBalancerWorkflowClientExternal extends WorkflowClientExternal
{

    /**
     * Generated from {@link com.eucalyptus.loadbalancing.workflow.UpgradeLoadBalancerWorkflow#upgradeLoadBalancer}
     */
    void upgradeLoadBalancer();

    /**
     * Generated from {@link com.eucalyptus.loadbalancing.workflow.UpgradeLoadBalancerWorkflow#upgradeLoadBalancer}
     */
    void upgradeLoadBalancer(StartWorkflowOptions optionsOverride);

    /**
     * Generated from {@link com.eucalyptus.loadbalancing.workflow.UpgradeLoadBalancerWorkflow#getState}
     */
    com.eucalyptus.loadbalancing.workflow.ElbWorkflowState getState() ;
}