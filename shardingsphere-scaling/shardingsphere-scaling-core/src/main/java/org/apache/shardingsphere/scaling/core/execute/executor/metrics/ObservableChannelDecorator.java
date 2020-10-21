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

import io.prometheus.client.Counter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.scaling.core.execute.executor.channel.Channel;
import org.apache.shardingsphere.scaling.core.execute.executor.record.Record;

import java.util.List;

/**
 * Observable channel decorator.
 */
@RequiredArgsConstructor
public final class ObservableChannelDecorator implements Channel {
    
    private static final Counter PUSH_COUNTER = Counter.build().name("pushNumberOfChannel").help("push number of channel").labelNames("jobId", "taskId").create().register();
    
    private static final Counter FETCH_COUNTER = Counter.build().name("fetchNumberOfChannel").help("fetch number of channel").labelNames("jobId", "taskId").create().register();
    
    private final String jobId;
    
    private final String taskId;
    
    private final Channel channel;
    
    @Override
    public void pushRecord(final Record dataRecord) throws InterruptedException {
        channel.pushRecord(dataRecord);
        PUSH_COUNTER.labels(jobId, taskId).inc();
    }
    
    @Override
    public List<Record> fetchRecords(final int batchSize, final int timeout) {
        List<Record> result = channel.fetchRecords(batchSize, timeout);
        FETCH_COUNTER.labels(jobId, taskId).inc(result.size());
        return result;
    }
    
    @Override
    public void ack() {
        channel.ack();
    }
    
    @Override
    public void close() {
        channel.close();
    }
}
