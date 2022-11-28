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

package org.apache.shardingsphere.test.sql.parser.internal.asserts.statement.ral.impl.queryable;

import org.apache.shardingsphere.distsql.parser.statement.ral.queryable.ConvertYamlConfigurationStatement;
import org.apache.shardingsphere.test.sql.parser.internal.asserts.SQLCaseAssertContext;
import org.apache.shardingsphere.test.sql.parser.internal.cases.parser.jaxb.statement.ral.ConvertYamlConfigurationStatementTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Convert yaml configuration statement assert.
 */
public final class ConvertYamlConfigurationStatementAssert {
    
    /**
     * Assert convert yaml configuration statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual actual convert yaml configuration statement
     * @param expected expected convert yaml configuration statement test case
     */
    public static void assertIs(final SQLCaseAssertContext assertContext, final ConvertYamlConfigurationStatement actual, final ConvertYamlConfigurationStatementTestCase expected) {
        if (null == expected) {
            assertNull(assertContext.getText("Actual statement should no exist."), actual);
        } else {
            assertThat(actual.getFilePath(), is(expected.getFilePath()));
        }
    }
}
