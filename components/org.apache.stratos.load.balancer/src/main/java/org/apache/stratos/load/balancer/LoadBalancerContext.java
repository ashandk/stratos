/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.synapse.config.SynapseConfiguration;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines load balancer context information.
 */
public class LoadBalancerContext {

    private static final Log log = LogFactory.getLog(LoadBalancerContext.class);
    private static volatile LoadBalancerContext instance;

    private SynapseConfiguration synapseConfiguration;
    private ConfigurationContext configCtxt;
    private AxisConfiguration axisConfiguration;
    private UserRegistry configRegistry;
    private UserRegistry governanceRegistry;
    private DependencyManagementService dependencyManager;

    // <TenantId, SynapseEnvironmentService> Map
    private Map<Integer, SynapseEnvironmentService> synapseEnvironmentServiceMap;
    // <ServiceName, ServiceContext> Map
    private Map<String, ServiceContext> serviceContextMap;
    // <ClusterId, ClusterContext> Map
    private Map<String, ClusterContext> clusterContextMap;
    // <Hostname, Cluster> Map
    private Map<String, Cluster> clusterMap;

    private LoadBalancerContext() {
        synapseEnvironmentServiceMap = new ConcurrentHashMap<Integer, SynapseEnvironmentService>();
        serviceContextMap = new ConcurrentHashMap<String, ServiceContext>();
        clusterContextMap = new ConcurrentHashMap<String, ClusterContext>();
        clusterMap = new ConcurrentHashMap<String, Cluster>();
    }

    public static LoadBalancerContext getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerContext.class){
                if (instance == null) {
                    instance = new LoadBalancerContext ();
                }
            }
        }
        return instance;
    }

    public void clear() {
        synapseEnvironmentServiceMap.clear();
        serviceContextMap.clear();
        clusterContextMap.clear();
        clusterMap.clear();
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    private RealmService realmService;

    public SynapseConfiguration getSynapseConfiguration() throws TenantAwareLoadBalanceEndpointException{
        assertNull("SynapseConfiguration", synapseConfiguration);
        return synapseConfiguration;
    }

    public void setSynapseConfiguration(SynapseConfiguration synapseConfiguration) {
        this.synapseConfiguration = synapseConfiguration;
    }

    public AxisConfiguration getAxisConfiguration() throws TenantAwareLoadBalanceEndpointException {
        assertNull("AxisConfiguration", axisConfiguration);
        return axisConfiguration;
    }

    public void setAxisConfiguration(AxisConfiguration axisConfiguration) {
        this.axisConfiguration = axisConfiguration;
    }

    public UserRegistry getConfigRegistry() throws TenantAwareLoadBalanceEndpointException {
        assertNull("Registry", configRegistry);
        return configRegistry;
    }

    public void setConfigRegistry(UserRegistry configRegistry) {
        this.configRegistry = configRegistry;
    }

    public DependencyManagementService getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(DependencyManagementService dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    private void assertNull(String name, Object object) throws TenantAwareLoadBalanceEndpointException {
        if (object == null) {
            String message = name + " reference in the proxy admin config holder is null";
            log.error(message);
            throw new TenantAwareLoadBalanceEndpointException(message);
        }
    }

    public UserRegistry getGovernanceRegistry() {
        return governanceRegistry;
    }

    public void setGovernanceRegistry(UserRegistry governanceRegistry) {
        this.governanceRegistry = governanceRegistry;
    }

    public SynapseEnvironmentService getSynapseEnvironmentService(int tenantId) {
        return synapseEnvironmentServiceMap.get(tenantId);
    }

    public void addSynapseEnvironmentService(int tenantId, SynapseEnvironmentService synapseEnvironmentService) {
        synapseEnvironmentServiceMap.put(tenantId, synapseEnvironmentService);
    }

    public void removeSynapseEnvironmentService(int tenantId) {
        synapseEnvironmentServiceMap.remove(tenantId);
    }

    public Map<Integer, SynapseEnvironmentService> getSynapseEnvironmentServiceMap() {
        return synapseEnvironmentServiceMap;
    }

    public ConfigurationContext getConfigCtxt() {
        return configCtxt;
    }

    public void setConfigCtxt(ConfigurationContext configCtxt) {
        this.configCtxt = configCtxt;
    }

    // ServiceContextMap methods START
    public Collection<ServiceContext> getServiceContexts() {
        return serviceContextMap.values();
    }

    public ServiceContext getServiceContext(String serviceName) {
        return serviceContextMap.get(serviceName);
    }

    public void addServiceContext(ServiceContext serviceContext) {
        serviceContextMap.put(serviceContext.getServiceName(), serviceContext);
    }

    public void removeServiceContext(String serviceName) {
        serviceContextMap.remove(serviceName);
    }
    // ServiceContextMap methods END

    // ClusterContextMap methods START
    public Collection<ClusterContext> getClusterContexts() {
        return clusterContextMap.values();
    }

    public ClusterContext getClusterContext(String clusterId) {
        return clusterContextMap.get(clusterId);
    }

    public void addClusterContext(ClusterContext clusterContext) {
        clusterContextMap.put(clusterContext.getClusterId(), clusterContext);
    }

    public void removeClusterContext(String clusterId) {
        clusterContextMap.remove(clusterId);
    }
    // ClusterContextMap methods END

    // ClusterMap methods START
    public Cluster getCluster(String hostName) {
        return clusterMap.get(hostName);
    }

    public boolean clusterExists(String hostName) {
        return clusterMap.containsKey(hostName);
    }

    public void addCluster(Cluster cluster) {
        for(String hostName : cluster.getHostNames()) {
            addCluster(hostName, cluster);
        }
    }

    public void addCluster(String hostName, Cluster cluster) {
        clusterMap.put(hostName, cluster);
    }

    public void removeCluster(String hostName) {
        clusterMap.remove(hostName);
    }
    // ClusterMap methods END
}
