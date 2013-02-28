/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.rest.repr;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.IteratorUtils.emptyIterator;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo4j.server.rest.domain.JsonHelper.jsonToMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.cypher.javacompat.PlanDescription;
import org.neo4j.cypher.javacompat.ProfilerStatistics;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.repr.formats.JsonFormat;

public class CypherResultRepresentationTest
{

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSerializeProfilingResult() throws Exception
    {
        // Given
        String name = "Kalle";

        PlanDescription plan = getMockDescription( name );
        PlanDescription childPlan = getMockDescription( "child" );
        when( plan.getChildren() ).thenReturn( asList( childPlan ) );
        when( plan.hasProfilerStatistics() ).thenReturn( true );

        ProfilerStatistics stats = mock( ProfilerStatistics.class );
        when( stats.getDbHits() ).thenReturn( 13l );
        when( stats.getRows() ).thenReturn( 25l );

        when( plan.getProfilerStatistics() ).thenReturn( stats );

        ExecutionResult result = mock(ExecutionResult.class);
        when(result.iterator()).thenReturn( emptyIterator());
        when(result.columns()).thenReturn(new ArrayList<String>());
        when( result.executionPlanDescription() ).thenReturn( plan );

        // When
        Map<String, Object> serialized = serialize( new CypherResultRepresentation( result, /*includeStats=*/false, true ) );

        // Then
        Map<String, Object> serializedPlan = (Map<String, Object>) serialized.get( "plan" );
        assertThat( (String) serializedPlan.get( "name" ), equalTo( name ) );
        assertThat( (Integer) serializedPlan.get( "rows" ), is( 25 ) );
        assertThat( (Integer) serializedPlan.get( "dbHits" ), is( 13 ) );

        List<Map<String, Object>> children = (List<Map<String, Object>>) serializedPlan.get( "children" );
        assertThat( children.size(), is( 1 ) );

        Map<String, Object> args = (Map<String, Object>) serializedPlan.get( "args" );
        assertThat( (String) args.get( "argumentKey" ), is( "argumentValue" ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldNotIncludePlanUnlessAskedFor() throws Exception
    {
        // Given
        ExecutionResult result = mock(ExecutionResult.class);
        when(result.iterator()).thenReturn( emptyIterator());
        when(result.columns()).thenReturn(new ArrayList<String>());

        // When
        Map<String, Object> serialized = serialize( new CypherResultRepresentation( result, /*includeStats=*/false, false ) );

        // Then
        assertFalse( "Didn't expect to see a plan here", serialized.containsKey( "plan" ) );
    }

    private PlanDescription getMockDescription( String name )
    {
        PlanDescription plan = mock( PlanDescription.class );
        when( plan.getName() ).thenReturn( name );
        when( plan.getArguments() ).thenReturn( MapUtil.map( "argumentKey", "argumentValue" ) );
        return plan;
    }

    private Map<String, Object> serialize( CypherResultRepresentation repr ) throws URISyntaxException, JsonParseException
    {
        OutputFormat format = new OutputFormat( new JsonFormat(), new URI( "http://localhost/" ), null );
        return jsonToMap( format.assemble( repr ) );
    }

}
