/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.core.executor.batch;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.shardingsphere.core.constant.ConnectionMode;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.executor.ShardingExecuteGroup;
import io.shardingsphere.core.executor.StatementExecuteUnit;
import io.shardingsphere.core.executor.sql.SQLExecuteUnit;
import io.shardingsphere.core.executor.sql.execute.SQLExecuteCallback;
import io.shardingsphere.core.executor.sql.execute.SQLExecuteTemplate;
import io.shardingsphere.core.executor.sql.execute.threadlocal.ExecutorDataMap;
import io.shardingsphere.core.executor.sql.execute.threadlocal.ExecutorExceptionHandler;
import io.shardingsphere.core.executor.sql.prepare.SQLExecutePrepareCallback;
import io.shardingsphere.core.executor.sql.prepare.SQLExecutePrepareTemplate;
import io.shardingsphere.core.jdbc.core.connection.ShardingConnection;
import io.shardingsphere.core.routing.RouteUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Prepared statement executor to process add batch.
 * 
 * @author zhangliang
 * @author maxiaoguang
 * @author panjuan
 */
@RequiredArgsConstructor
public abstract class BatchPreparedStatementExecutor {
    
    private final DatabaseType dbType;
    
    private SQLType sqlType;
    
    private int batchCount;
    
    private final int resultSetType;
    
    private final int resultSetConcurrency;
    
    private final int resultSetHoldability;
    
    private final boolean returnGeneratedKeys;
    
    private final ShardingConnection connection;
    
    private final Collection<RouteUnit> routeUnits = new LinkedList<>();
    
    private final SQLExecuteTemplate sqlExecuteTemplate;
    
    private final SQLExecutePrepareTemplate sqlExecutePrepareTemplate;
    
    @Getter
    private final List<ResultSet> resultSets = new LinkedList<>();
    
    @Getter
    private final List<PreparedStatement> statements = new LinkedList<>();
    
    @Getter
    private final List<List<Object>> parameterSets = new LinkedList<>();
    
    @Getter
    private final Collection<SQLExecuteUnit> executeUnits = new LinkedList<>();
    
    public BatchPreparedStatementExecutor(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability, final boolean returnGeneratedKeys,
                                          final ShardingConnection shardingConnection) throws SQLException {
        this.dbType = shardingConnection.getShardingDataSource().getShardingContext().getDatabaseType();
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.returnGeneratedKeys = returnGeneratedKeys;
        this.connection = shardingConnection;
        sqlExecuteTemplate = new SQLExecuteTemplate(connection.getShardingDataSource().getShardingContext().getExecuteEngine());
        sqlExecutePrepareTemplate = new SQLExecutePrepareTemplate(connection.getShardingDataSource().getShardingContext().getMaxConnectionsSizePerQuery());
    }
    
    /**
     * Add batch for route units.
     *
     * @param batchCount batch count
     * @param routeUnits route units
     * @throws SQLException sql exception
     */
    public void addBatchForRouteUnits(final int batchCount, final Collection<RouteUnit> routeUnits) throws SQLException {
        handleOldRouteUnits(new LinkedList<>(this.routeUnits));
        handleNewRouteUnits(new LinkedList<>(routeUnits), batchCount);
    }
    
    private void handleOldRouteUnits(final Collection<RouteUnit> oldRouteUnits) {
        oldRouteUnits.retainAll(routeUnits);
        for (final RouteUnit each : oldRouteUnits) {
            addParametersForExecuteUnit(each);
        }
    }
    
    private void addParametersForExecuteUnit(final RouteUnit each) {
        Optional<SQLExecuteUnit> preparedBatchStatementOptional = Iterators.tryFind(executeUnits.iterator(), new Predicate<SQLExecuteUnit>() {
            
            @Override
            public boolean apply(final SQLExecuteUnit input) {
                return input.getRouteUnit().equals(each);
            }
        });
        if (preparedBatchStatementOptional.isPresent()) {
            preparedBatchStatementOptional.get().getRouteUnit().getSqlUnit().getParameterSets().add(each.getSqlUnit().getParameterSets().get(0));
        }
    }
    
    private void handleNewRouteUnits(final Collection<RouteUnit> newRouteUnits, final int batchCount) throws SQLException {
        this.batchCount = batchCount;
        newRouteUnits.removeAll(this.routeUnits);
        List<BatchPreparedStatementExecuteUnit> newExecuteUnits = createNewExecuteUnits(newRouteUnits, batchCount);
        this.routeUnits.addAll(newRouteUnits);
        this.executeUnits.addAll(newExecuteUnits);
    }
    
    private List<BatchPreparedStatementExecuteUnit> createNewExecuteUnits(final Collection<RouteUnit> newRouteUnits, final int batchCount) throws SQLException {
        List<BatchPreparedStatementExecuteUnit> result = new LinkedList<>();
        for (RouteUnit each : newRouteUnits) {
            PreparedStatement preparedStatement = createPreparedStatement(connection.getConnection(each.getDataSourceName()), each.getSqlUnit().getSql());
            preparedStatement.addBatch();
            BatchPreparedStatementExecuteUnit executeUnit = new BatchPreparedStatementExecuteUnit(each, preparedStatement, ConnectionMode.CONNECTION_STRICTLY);
            executeUnit.mapAddBatchCount(batchCount);
            result.add(executeUnit);
        }
        return result;
    }
    
    /**
     * Init.
     *
     * @param sqlType sql type
     */
    public void init(final SQLType sqlType) {
        this.sqlType = sqlType;
    }
    
    private Collection<ShardingExecuteGroup<SQLExecuteUnit>> obtainExecuteGroups() throws SQLException {
        return sqlExecutePrepareTemplate.getExecuteUnitGroups(routeUnits, new SQLExecutePrepareCallback() {
        
            @Override
            public Connection getConnection(final String dataSourceName) throws SQLException {
                return connection.getConnection(dataSourceName);
            }
        
            @Override
            public SQLExecuteUnit createSQLExecuteUnit(final Connection connection, final RouteUnit routeUnit, final ConnectionMode connectionMode) throws SQLException {
                PreparedStatement preparedStatement = createPreparedStatement(connection, routeUnit.getSqlUnit().getSql());
                statements.add(preparedStatement);
                parameterSets.add(routeUnit.getSqlUnit().getParameterSets().get(0));
                return new StatementExecuteUnit(routeUnit, preparedStatement, connectionMode);
            }
        });
    }
    
    /**
     * Execute batch.
     * 
     * @return execute results
     * @throws SQLException SQL exception
     */
    public int[] executeBatch() throws SQLException {
        final boolean isExceptionThrown = ExecutorExceptionHandler.isExceptionThrown();
        final Map<String, Object> dataMap = ExecutorDataMap.getDataMap();
        SQLExecuteCallback<int[]> callback = new SQLExecuteCallback<int[]>(sqlType, isExceptionThrown, dataMap) {
            
            @Override
            protected int[] executeSQL(final SQLExecuteUnit sqlExecuteUnit) throws SQLException {
                return sqlExecuteUnit.getStatement().executeBatch();
            }
        };
        return accumulate(executeCallback(callback));
    }
    
    private int[] accumulate(final List<int[]> results) {
        int[] result = new int[batchCount];
        int count = 0;
        for (BatchPreparedStatementExecuteUnit each : getBatchPreparedStatementUnitGroups()) {
            for (Entry<Integer, Integer> entry : each.getJdbcAndActualAddBatchCallTimesMap().entrySet()) {
                int value = null == results.get(count) ? 0 : results.get(count)[entry.getValue()];
                if (DatabaseType.Oracle == dbType) {
                    result[entry.getKey()] = value;
                } else {
                    result[entry.getKey()] += value;
                }
            }
            count++;
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private <T> List<T> executeCallback(final SQLExecuteCallback<T> executeCallback) throws SQLException {
        return sqlExecuteTemplate.executeGroup((Collection) executeGroups, executeCallback);
    }
    
    private PreparedStatement createPreparedStatement(final Connection connection, final String sql) throws SQLException {
        return returnGeneratedKeys ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    private Collection<BatchPreparedStatementExecuteUnit> getBatchPreparedStatementUnitGroups() {
        Collection<BatchPreparedStatementExecuteUnit> result = new LinkedList<>();
        for (ShardingExecuteGroup<SQLExecuteUnit> each : executeGroups) {
            result.addAll(Lists.transform(each.getInputs(), new Function<SQLExecuteUnit, BatchPreparedStatementExecuteUnit>() {
                
                @Override
                public BatchPreparedStatementExecuteUnit apply(final SQLExecuteUnit input) {
                    return (BatchPreparedStatementExecuteUnit) input;
                }
            }));
        }
        return result;
    }
}

