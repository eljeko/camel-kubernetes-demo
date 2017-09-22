/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.ocp.demo;

import io.fabric8.kubernetes.api.model.Pod;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;


@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends SpringBootServletInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean servlet = new ServletRegistrationBean(
                new CamelHttpTransportServlet(), "/camel-kubernetes/*");
        servlet.setName("CamelServlet");
        return servlet;
    }

    @Component
    class RestApi extends RouteBuilder {


        @Override
        public void configure() {
            /*
            try {
        		getContext().setTracing(Boolean.parseBoolean(env.getProperty("ENABLE_TRACER", "false")));
    		} catch (Exception e) {
    			LOG.error("Failed to parse the ENABLE_TRACER value: {}", env.getProperty("ENABLE_TRACER", "false"));
    		}   */

            restConfiguration().component("servlet")
                .bindingMode(RestBindingMode.json);

            rest("/pods/").description("Customer account Service")
                .produces(MediaType.APPLICATION_JSON_VALUE)

            // Handle CORS Pre-flight requests
            .options("/")
                .route().id("accountOptions").end()
            .endRest()

            .get("/").description("Get Pods")
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .bindingMode(RestBindingMode.json)
                .outType(Pod.class)
                .route().id("podRoute")
                    .setBody(simple("null"))
                    .log("Finding pods")
                    .removeHeaders("CamelHttp*")
                    .to("kubernetes://{{kubernetes-master-url}}?category=pods&namespace=test-custom&namespaceName=test-custom&oauthToken={{kubernetes-oauth-token:}}&operation=listPods")
                    .log("We currently have ${body.size()} pods")
                .end()
            .endRest();

        }
        
    }

}