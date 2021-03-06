package com.mesosphere.sdk.http.queries;

import com.mesosphere.sdk.specification.ConfigFileSpec;
import com.mesosphere.sdk.specification.DefaultConfigFileSpec;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.ServiceSpec;
import com.mesosphere.sdk.specification.TaskSpec;
import com.mesosphere.sdk.state.ConfigStore;
import com.mesosphere.sdk.state.ConfigStoreException;
import com.mesosphere.sdk.storage.StorageError.Reason;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ArtifactQueriesTest {

    @Mock private ConfigStore<ServiceSpec> mockConfigStore;
    @Mock private ServiceSpec mockServiceSpec;
    @Mock private PodSpec mockPodSpec;
    @Mock private TaskSpec mockTaskSpec;

    @Before
    public void beforeAll() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetTemplateBadUUID() throws ConfigStoreException {
        assertEquals(400, ArtifactQueries.getTemplate(mockConfigStore, "bad uuid", "pod", "task", "conffile").getStatus());
    }

    @Test
    public void testGetTemplateServiceConfigNotFound() throws ConfigStoreException {
        UUID uuid = UUID.randomUUID();
        when(mockConfigStore.fetch(uuid)).thenThrow(new ConfigStoreException(Reason.NOT_FOUND, "hi"));
        assertEquals(404, ArtifactQueries.getTemplate(mockConfigStore, uuid.toString(), "pod", "task", "conffile").getStatus());
    }

    @Test
    public void testGetTemplateServiceConfigReadFailed() throws ConfigStoreException {
        UUID uuid = UUID.randomUUID();
        when(mockConfigStore.fetch(uuid)).thenThrow(new ConfigStoreException(Reason.STORAGE_ERROR, "hi"));
        assertEquals(500, ArtifactQueries.getTemplate(mockConfigStore, uuid.toString(), "pod", "task", "conffile").getStatus());
    }

    @Test
    public void testGetTemplatePodNotFound() throws ConfigStoreException {
        UUID uuid = UUID.randomUUID();
        when(mockConfigStore.fetch(uuid)).thenReturn(mockServiceSpec);
        when(mockServiceSpec.getPods()).thenReturn(Collections.emptyList());
        assertEquals(404, ArtifactQueries.getTemplate(mockConfigStore, uuid.toString(), "pod", "task", "conffile").getStatus());
    }

    @Test
    public void testGetTemplateTaskNotFound() throws ConfigStoreException {
        UUID uuid = UUID.randomUUID();
        when(mockConfigStore.fetch(uuid)).thenReturn(mockServiceSpec);
        when(mockServiceSpec.getPods()).thenReturn(Arrays.asList(mockPodSpec));
        when(mockPodSpec.getType()).thenReturn("pod");
        when(mockPodSpec.getTasks()).thenReturn(Collections.emptyList());
        assertEquals(404, ArtifactQueries.getTemplate(mockConfigStore, uuid.toString(), "pod", "task", "conffile").getStatus());
    }

    @Test
    public void testGetTemplateConfigNameNotFound() throws ConfigStoreException {
        UUID uuid = UUID.randomUUID();
        when(mockConfigStore.fetch(uuid)).thenReturn(mockServiceSpec);
        when(mockServiceSpec.getPods()).thenReturn(Arrays.asList(mockPodSpec));
        when(mockPodSpec.getType()).thenReturn("pod");
        when(mockPodSpec.getTasks()).thenReturn(Arrays.asList(mockTaskSpec));
        when(mockTaskSpec.getName()).thenReturn("task");
        when(mockTaskSpec.getConfigFiles()).thenReturn(Collections.emptyList());
        assertEquals(404, ArtifactQueries.getTemplate(mockConfigStore, uuid.toString(), "pod", "task", "conffile").getStatus());
    }

    @Test
    public void testGetTemplateSuccess() throws ConfigStoreException {
        UUID uuid = UUID.randomUUID();
        when(mockConfigStore.fetch(uuid)).thenReturn(mockServiceSpec);
        when(mockServiceSpec.getPods()).thenReturn(Arrays.asList(mockPodSpec));
        when(mockPodSpec.getType()).thenReturn("pod");
        when(mockPodSpec.getTasks()).thenReturn(Arrays.asList(mockTaskSpec));
        when(mockTaskSpec.getName()).thenReturn("task");
        ConfigFileSpec configSpec = DefaultConfigFileSpec.newBuilder()
                .name("conffile")
                .relativePath("../conf/confpath.xml")
                .templateContent("content goes here")
                .build();
        when(mockTaskSpec.getConfigFiles()).thenReturn(Arrays.asList(configSpec));
        Response r = ArtifactQueries.getTemplate(mockConfigStore, uuid.toString(), "pod", "task", "conffile");
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, r.getMediaType());
        assertEquals(configSpec.getTemplateContent(), r.getEntity());
    }
}
