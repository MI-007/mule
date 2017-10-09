/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.config.bootstrap.ArtifactType.APP;
import static org.mule.config.bootstrap.ArtifactType.DOMAIN;

import org.mule.api.MuleContext;
import org.mule.config.bootstrap.ArtifactType;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ArtifactDeploymentListenerAdapterTestCase
{

    private static final String ARTIFACT_NAME = "artifactName";

    @Mock
    private ArtifactDeploymentListener listener;

    @Mock
    private MuleContext context;

    private ArtifactDeploymentListenerAdapter adapter;

    @Before
    public void setUp() throws Exception
    {
        adapter = new ArtifactDeploymentListenerAdapter(listener);
    }

    @Test
    public void adapt() throws Exception
    {
        adapter.adapt();
        DeploymentListener applicationDeploymentListener = adapter.getAdaptedApplicationDeploymentListener();
        DeploymentListener domainDeploymentListener = adapter.getAdaptedDomainDeploymentListener();
        assertDeploymentListenerInvocations(applicationDeploymentListener, APP);
        assertDeploymentListenerInvocations(domainDeploymentListener, DOMAIN);
    }

    private void assertDeploymentListenerInvocations(DeploymentListener deploymentListener, ArtifactType artifactType)
    {
        deploymentListener.onDeploymentStart(ARTIFACT_NAME);
        verify(listener).onDeploymentStart(ARTIFACT_NAME, artifactType);
        deploymentListener.onDeploymentFailure(ARTIFACT_NAME, null);
        verify(listener).onDeploymentFailure(ARTIFACT_NAME, artifactType, null);
        deploymentListener.onDeploymentSuccess(ARTIFACT_NAME);
        verify(listener).onDeploymentSuccess(ARTIFACT_NAME, artifactType);
        deploymentListener.onUndeploymentStart(ARTIFACT_NAME);
        verify(listener).onUndeploymentStart(ARTIFACT_NAME, artifactType);
        deploymentListener.onUndeploymentFailure(ARTIFACT_NAME, null);
        verify(listener).onUndeploymentFailure(ARTIFACT_NAME, artifactType, null);
        deploymentListener.onUndeploymentSuccess(ARTIFACT_NAME);
        verify(listener).onUndeploymentSuccess(ARTIFACT_NAME, artifactType);
        deploymentListener.onMuleContextCreated(ARTIFACT_NAME, context);
        verify(listener).onMuleContextCreated(ARTIFACT_NAME, context);
        deploymentListener.onMuleContextConfigured(ARTIFACT_NAME, context);
        verify(listener).onMuleContextConfigured(ARTIFACT_NAME, context);
        deploymentListener.onMuleContextInitialised(ARTIFACT_NAME, context);
        verify(listener).onMuleContextInitialised(ARTIFACT_NAME, context);
        reset(listener);
    }

}