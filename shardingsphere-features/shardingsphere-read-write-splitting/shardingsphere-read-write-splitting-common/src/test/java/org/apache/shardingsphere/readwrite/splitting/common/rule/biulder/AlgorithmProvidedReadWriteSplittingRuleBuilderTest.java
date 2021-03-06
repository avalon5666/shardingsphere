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

package org.apache.shardingsphere.readwrite.splitting.common.rule.biulder;

import org.apache.shardingsphere.infra.rule.builder.ShardingSphereRuleBuilder;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.spi.ordered.OrderedSPIRegistry;
import org.apache.shardingsphere.readwrite.splitting.api.rule.ReadWriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.common.algorithm.config.AlgorithmProvidedReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.common.rule.ReadWriteSplittingRule;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class AlgorithmProvidedReadWriteSplittingRuleBuilderTest {
    
    static {
        ShardingSphereServiceLoader.register(ShardingSphereRuleBuilder.class);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void assertBuild() {
        AlgorithmProvidedReadWriteSplittingRuleConfiguration algorithmProvidedRuleConfig = mock(AlgorithmProvidedReadWriteSplittingRuleConfiguration.class);
        ReadWriteSplittingDataSourceRuleConfiguration ruleConfig = new ReadWriteSplittingDataSourceRuleConfiguration(
                "name", "writeDataSourceName", Collections.singletonList("name"), "loadBalancerName");
        when(algorithmProvidedRuleConfig.getDataSources()).thenReturn(Collections.singletonList(ruleConfig));
        ShardingSphereRuleBuilder builder = OrderedSPIRegistry.getRegisteredServices(
                Collections.singletonList(algorithmProvidedRuleConfig), ShardingSphereRuleBuilder.class).get(algorithmProvidedRuleConfig);
        assertThat(builder.build(algorithmProvidedRuleConfig), instanceOf(ReadWriteSplittingRule.class));
    }
}
