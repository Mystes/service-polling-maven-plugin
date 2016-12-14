/*
 * Copyright 2016 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.mystes.maven;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mojo( name = "poll", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class PollingMojo extends AbstractMojo {

    @Parameter(required = true)
    private String url;

    @Parameter(required = false, defaultValue = "120")
    private int timeout;

    @Parameter(defaultValue = "GET")
    private String requestType;

    @Parameter
    private List<Integer> acceptedStatusCodes;

    @Parameter(defaultValue = "1000")
    private int pollingIntervalMillis;

    @Parameter(defaultValue = "false")
    private boolean skip;


    public void execute() throws MojoExecutionException {
        if (skip) {
            return;
        }

        checkAcceptedStatusCodes();

        getLog().info("Waiting for HTTP " + acceptedStatusCodes.toString() + " from " + url);

        CloseableHttpClient client = getClient();

        HttpUriRequest getRequest = getHttpMethod();

        long startTime = System.currentTimeMillis();

        try {
            HttpResponse response = null;

            while(true) {
                checkTimeout(startTime);
                try {
                    response = client.execute(getRequest);
                } catch (IOException ignored) {
                    getLog().info("No connection. Retrying...");
                    Thread.sleep(pollingIntervalMillis);
                    if(response != null) {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                    continue;
                }

                if(acceptedStatusCodes.contains(response.getStatusLine().getStatusCode())) {
                    getLog().info("We got correct response code: " + response.getStatusLine().getStatusCode());
                    EntityUtils.consumeQuietly(response.getEntity());
                    break;
                } else {
                    getLog().info("Got response, but status code was not in allowed codes. Status code was: " + response.getStatusLine().getStatusCode());
                    getLog().info("Continuing until we get timeout.");
                    Thread.sleep(pollingIntervalMillis);
                }
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Interrupted!");
        }

        getLog().info("Resuming!");
    }

    private void checkAcceptedStatusCodes() {
        if(acceptedStatusCodes == null) {
            acceptedStatusCodes = new ArrayList<Integer>();
        }

        if (acceptedStatusCodes.size() == 0) {
            getLog().info("No HTTP Status codes supplied. Assuming 200.");
            acceptedStatusCodes.add(200);
        }
    }

    private CloseableHttpClient getClient() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());

            return HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } catch (Exception e) {
            getLog().error("Failed to create client that trusts self signed certificates. Returning normal client.");
            return HttpClients.createMinimal();
        }
    }

    private void checkTimeout(long startTime) throws MojoExecutionException {
        long timeNow = System.currentTimeMillis();
        long elapsed = timeNow - startTime;

        if (elapsed / 1000 > this.timeout) {
            throw new MojoExecutionException("Timed out while waiting for service at " + this.url);
        }
    }

    private HttpUriRequest getHttpMethod() throws MojoExecutionException {
        int connTimeout = 5000;
        RequestConfig conf = RequestConfig.custom()
                .setConnectionRequestTimeout(connTimeout)
                .setConnectTimeout(connTimeout)
                .setSocketTimeout(connTimeout)
                .build();

        switch (RequestType.valueOf(requestType)) {
            case GET:
                HttpGet req = new HttpGet(url);
                req.setConfig(conf);
                return req;
            case POST:
                HttpPost postReq = new HttpPost(url);
                postReq.setConfig(conf);
                return postReq;
        }

        throw new MojoExecutionException("Method not implemented.");
    }

    private enum RequestType {
        GET,
        POST
    }
}
