/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */
package org.wso2.carbon.apimgt.gateway.handlers.ext;


import java.util.Map;

public class APIManagerCacheExtensionHandlerWrapper extends APIManagerCacheExtensionHandler {
    private boolean tenantFlowStarted = false;
    private boolean tenantFlowFinished = false;
    private Map<String, String> tokenCache;
    private Map<String, String> invalidTokenCache;

    public APIManagerCacheExtensionHandlerWrapper(Map<String, String> tokenCache, Map<String, String>
			invalidTokenCache) {
        this.tokenCache = tokenCache;
        this.invalidTokenCache = invalidTokenCache;
    }

    @Override
    protected void startTenantFlow(String tenantDomain) {
        tenantFlowStarted = true;
    }

    @Override
    protected void endTenantFlow() {
        tenantFlowFinished = true;
    }

    @Override
    protected String getCachedTenantDomain(String token) {
        return tokenCache.get(token);
    }

    @Override
    protected void removeCacheEntryFromGatewayCache(String key) {
        tokenCache.remove(key);
    }

    @Override
    protected void putInvalidTokenEntryIntoInvalidTokenCache(String cachedToken, String tenantDomain) {
        invalidTokenCache.put(cachedToken, tenantDomain);
    }

    public boolean isTenantFlowStarted() {
        return tenantFlowStarted;
    }

    public boolean isTenantFlowFinished() {
        return tenantFlowFinished;
    }

}
