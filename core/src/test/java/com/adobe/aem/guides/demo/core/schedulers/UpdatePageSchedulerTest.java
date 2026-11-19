package com.adobe.aem.guides.demo.core.schedulers;

import com.adobe.aem.guides.demo.core.services.ApiConfigService;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePageSchedulerTest {

    @Mock
    private ResourceResolverFactory resolverFactory;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource pageResource;

    @Mock
    private Resource jcrContent;

    @Mock
    private ModifiableValueMap properties;

    @Mock
    private ApiConfigService apiConfigService;

    @InjectMocks
    private UpdatePageScheduler scheduler;

    @Test
    void runWritesApiConfigOntoPage() throws Exception {
        when(apiConfigService.getPagePath()).thenReturn("/content/demo");
        when(apiConfigService.getClientId()).thenReturn("cid");
        when(apiConfigService.getApiToken()).thenReturn("tok");
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resolver);
        when(resolver.getResource("/content/demo")).thenReturn(pageResource);
        when(pageResource.getChild("jcr:content")).thenReturn(jcrContent);
        when(jcrContent.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        scheduler.run();

        verify(properties).put("clientId", "cid");
        verify(properties).put("apiToken", "tok");
        verify(resolver).commit();
        verify(resolver).close();
    }
}
