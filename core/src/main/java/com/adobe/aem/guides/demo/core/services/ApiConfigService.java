package com.adobe.aem.guides.demo.core.services;

import com.adobe.aem.guides.demo.core.config.ApiConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = ApiConfigService.class, immediate = true)
@Designate(ocd = ApiConfig.class)
public class ApiConfigService {

    private String clientId;
    private String apiToken;
    private String pagePath;

    @Activate
    protected void activate(ApiConfig config) {
        this.clientId = config.clientId();
        this.apiToken = config.apiToken();
        this.pagePath = config.pagePath();
    }

    public String getClientId() {
        return clientId;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getPagePath() {
        return pagePath;
    }
}