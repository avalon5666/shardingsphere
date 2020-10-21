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

import org.apache.shardingsphere.scaling.core.execute.executor.channel.AckCallback;
import org.apache.shardingsphere.scaling.core.execute.executor.channel.Channel;
import org.apache.shardingsphere.scaling.core.execute.executor.channel.DistributionChannel;
import org.apache.shardingsphere.scaling.core.execute.executor.channel.MemoryChannel;

/**
 * Observable channel factory.
 */
public final class ObservableChannelFactory {
    
    /**
     * Create observable memory channel.
     *
     * @param jobId job id
     * @param taskId task id
     * @param ackCallback ack callback
     * @return channel
     */
    public static Channel createMemoryChannel(final int jobId, final String taskId, final AckCallback ackCallback) {
        AckCallback callback = new ObservableAckCallbackDecorator(Integer.toString(jobId), taskId, ackCallback);
        return new ObservableChannelDecorator(Integer.toString(jobId), taskId, new MemoryChannel(callback));
    }
    
    /**
     * Create observable memory channel.
     *
     * @param jobId job id
     * @param taskId task id
     * @param channelNumber channel number
     * @param ackCallback ack callback
     * @return channel
     */
    public static Channel createDistributionChannel(final int jobId, final String taskId, final int channelNumber, final AckCallback ackCallback) {
        AckCallback callback = new ObservableAckCallbackDecorator(Integer.toString(jobId), taskId, ackCallback);
        return new ObservableChannelDecorator(Integer.toString(jobId), taskId, new DistributionChannel(channelNumber, callback));
    }
}
