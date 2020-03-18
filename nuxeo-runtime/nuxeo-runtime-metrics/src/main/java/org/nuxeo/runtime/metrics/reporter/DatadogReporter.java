/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.metrics.reporter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.coursera.metrics.datadog.DefaultMetricNameFormatter;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.coursera.metrics.datadog.transport.Transport;
import org.coursera.metrics.datadog.transport.UdpTransport;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;
import org.nuxeo.runtime.metrics.reporter.patch.NuxeoDatadogReporter;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;

/**
 * Patches the coursera implementation to support metric with tags.
 *
 * @since 11.1
 */
public class DatadogReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(DatadogReporter.class);

    protected NuxeoDatadogReporter reporter;

    protected String hostname;

    protected List<String> tags;

    @Override
    public void init(long pollInterval, Map<String, String> options) {
        super.init(pollInterval, options);
        hostname = getHostname();
        tags = getTags();
    }

    private String getHostname() {
        String value = options.get("hostname");
        if (!isBlank(value)) {
            return value;
        }
        value = getHostnameFromNuxeoUrl();
        if (!isBlank(value)) {
            return value;
        }
        return getCurrentHostname();
    }

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        Transport transport;
        if (getOptionAsBoolean("udp", false)) {
            String host = requireOption("host", " when using UDP");
            int port = getOptionAsInt("port", 8125);
            log.warn("Connecting using UDP {}:{} reporting every {}s from {}", host, port, pollInterval, hostname);
            transport = new UdpTransport.Builder().withStatsdHost(host).withPort(port).withRetryingLookup(true).build();
        } else {
            String apiKey = requireOption("apiKey", " when using HTTP");
            transport = new HttpTransport.Builder().withApiKey(apiKey).build();
            log.debug("Connecting using HTTP transport using apiKey reporting every {}s from {}", pollInterval,
                    hostname);
            log.warn("Connecting using HTTP transport using apiKey reporting every {}s from {}", pollInterval,
                    hostname);
        }
        reporter = NuxeoDatadogReporter.forRegistry(registry)
                                       .withHost(hostname)
                                       .withTags(tags)
                                       .withTransport(transport)
                                       .withExpansions(getExpansions(deniedExpansions))
                                       .filter(filter)
                                       .withMetricNameFormatter(new DefaultMetricNameFormatter())
                                       .build();
        reporter.start(getPollInterval(), TimeUnit.SECONDS);
    }

    protected List<String> getTags() {
        String value = getOption("tags", "nuxeo");
        return Arrays.asList(value.split(","));
    }

    protected EnumSet<NuxeoDatadogReporter.Expansion> getExpansions(Set<MetricAttribute> deniedExpansions) {
        if (deniedExpansions.isEmpty()) {
            return NuxeoDatadogReporter.Expansion.ALL;
        } else {
            return NuxeoDatadogReporter.Expansion.ALL.stream()
                                                     .filter(e -> deniedExpansions.contains(e.toString()))
                                                     .collect(Collectors.toCollection(() -> EnumSet.noneOf(
                                                             NuxeoDatadogReporter.Expansion.class)));
        }
    }

    @Override
    public void stop() {
        log.debug("Stop reporting");
        reporter.stop();
    }

}
