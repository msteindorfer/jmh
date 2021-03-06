/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.results;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class contains all info returned by benchmark iteration or/and collected during benchmark iteration.
 */
public class IterationResult implements Serializable {
    private static final long serialVersionUID = 960397066774710819L;

    private final BenchmarkParams benchmarkParams;
    private final IterationParams params;
    private final List<Result> primaryResults;
    private final Multimap<String, Result> secondaryResults;
    private String scoreUnit;

    public IterationResult(BenchmarkParams benchmarkParams, IterationParams params) {
        this.benchmarkParams = benchmarkParams;
        this.params = params;
        this.primaryResults = new ArrayList<Result>();
        this.secondaryResults = new TreeMultimap<String, Result>();
    }

    public void addResults(Collection<? extends Result> rs) {
        for (Result r : rs) {
            addResult(r);
        }
    }

    public void addResult(Result result) {
        if (result.getRole().isPrimary()) {
            if (scoreUnit == null) {
                scoreUnit = result.getScoreUnit();
            } else {
                if (!scoreUnit.equals(result.getScoreUnit())) {
                    throw new IllegalStateException("Adding the result with another score unit!");
                }
            }
            primaryResults.add(result);
        }

        if (result.getRole().isSecondary()) {
            secondaryResults.put(result.getLabel(), result);
        }
    }

    public Collection<Result> getRawPrimaryResults() {
        return primaryResults;
    }

    public Multimap<String, Result> getRawSecondaryResults() {
        return secondaryResults;
    }

    public Map<String, Result> getSecondaryResults() {
        Map<String, Result> answer = new TreeMap<String, Result>();
        for (String label : secondaryResults.keys()) {
            Collection<Result> results = secondaryResults.get(label);

            Result next = results.iterator().next();

            @SuppressWarnings("unchecked")
            Aggregator<Result> aggregator = next.getThreadAggregator();
            Result result = aggregator.aggregate(results);
            answer.put(label, result);
        }
        return answer;
    }

    public Result getPrimaryResult() {
        Result next = primaryResults.iterator().next();

        @SuppressWarnings("unchecked")
        Aggregator<Result> aggregator = next.getThreadAggregator();
        return aggregator.aggregate(primaryResults);
    }

    public IterationParams getParams() {
        return params;
    }

    public BenchmarkParams getBenchmarkParams() {
        return benchmarkParams;
    }

    public String getScoreUnit() {
        return scoreUnit;
    }

}
