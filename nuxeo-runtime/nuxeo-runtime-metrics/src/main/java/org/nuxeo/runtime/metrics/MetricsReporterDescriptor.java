/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 11.1
 */
@XObject("reporter")
public class MetricsReporterDescriptor implements Descriptor {

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@name")
    public String name;

    @XNode("@class")
    public Class<? extends MetricsReporter> klass;

    // use a string in order to be able to use variable
    @XNode("@pollInterval")
    protected String pollInterval;

    public long getPollInterval() {
        return Long.valueOf(pollInterval);
    }

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @Override
    public String getId() {
        return name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public MetricsReporter newInstance() {
        if (!MetricsReporter.class.isAssignableFrom(klass)) {
            throw new IllegalArgumentException(
                    "Cannot create reporter: " + getId() + ", class must implement MetricsReporter");
        }
        try {
            MetricsReporter ret = klass.getDeclaredConstructor().newInstance();
            ret.init(getPollInterval(), getOptions());
            return ret;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot create reporter: " + getId(), e);
        }
    }
}
