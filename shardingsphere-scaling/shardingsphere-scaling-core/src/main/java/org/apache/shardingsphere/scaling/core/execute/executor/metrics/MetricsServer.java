/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.scaling.core.execute.executor.metrics;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;

/**
 * Prometheus metrics server.
 */
public final class MetricsServer {
    
    public static final MetricsServer INSTANCE = new MetricsServer();
    
    private HTTPServer httpServer;
    
    private boolean isStarted;
    
    private MetricsServer() {
    
    }
    
    /**
     * Start.
     *
     * @param port port
     * @throws IOException io exception
     */
    public void start(final int port) throws IOException {
        if (!isStarted) {
            httpServer = new HTTPServer(port);
            DefaultExports.initialize();
        }
    }
    
    /**
     * Stop.
     */
    public void stop() {
        if (isStarted) {
            httpServer.stop();
        }
    }
}
