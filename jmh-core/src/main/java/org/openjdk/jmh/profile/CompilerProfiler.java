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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Result;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CompilerProfiler implements InternalProfiler {

    private long startCompTime = -1;

    @Override
    public String getDescription() {
        return "JIT compiler profiling via standard MBeans";
    }

    @Override
    public String label() {
        return "comp";
    }

    @Override
    public boolean checkSupport(List<String> msgs) {
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        if (comp.isCompilationTimeMonitoringSupported()) {
            return true;
        } else {
            msgs.add("The MXBean is available, but compilation time monitoring is disabled.");
            return false;
        }
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        try {
            startCompTime = comp.getTotalCompilationTime();
        } catch (UnsupportedOperationException e) {
            startCompTime = -1;
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        long compTime = -startCompTime;
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        try {
            compTime += comp.getTotalCompilationTime();
        } catch (UnsupportedOperationException e) {
            compTime = -1;
        }

        return Arrays.asList(
                new ProfilerResult("@compiler.time.profiled", compTime, "ms", AggregationPolicy.SUM),
                new ProfilerResult("@compiler.time.total", comp.getTotalCompilationTime(), "ms", AggregationPolicy.MAX)
        );
    }

}
