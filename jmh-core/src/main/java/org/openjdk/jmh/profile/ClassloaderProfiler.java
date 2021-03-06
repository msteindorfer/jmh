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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClassloaderProfiler implements InternalProfiler {

    private long loadedClasses = -1;
    private long unloadedClasses = -1;

    @Override
    public String getDescription() {
        return "Classloader profiling via standard MBeans";
    }

    @Override
    public boolean checkSupport(List<String> msgs) {
        return true;
    }

    @Override
    public String label() {
        return "cl";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
        try {
            loadedClasses = cl.getLoadedClassCount();
        } catch (UnsupportedOperationException e) {
            loadedClasses = -1;
        }
        try {
            unloadedClasses = cl.getUnloadedClassCount();
        } catch (UnsupportedOperationException e) {
            unloadedClasses = -1;
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        long loaded;
        long unloaded;
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();

        int loadedClassCount;
        try {
            loadedClassCount = cl.getLoadedClassCount();
            loaded = loadedClassCount - loadedClasses;
        } catch (UnsupportedOperationException e) {
            loaded = -1;
            loadedClassCount = -1;
        }

        long unloadedClassCount;
        try {
            unloadedClassCount = cl.getUnloadedClassCount();
            unloaded = unloadedClassCount - unloadedClasses;
        } catch (UnsupportedOperationException e) {
            unloaded = -1;
            unloadedClassCount = -1;
        }

        return Arrays.asList(
                new ProfilerResult("@classload.loaded.profiled", loaded, "classes", AggregationPolicy.SUM),
                new ProfilerResult("@classload.unloaded.profiled", unloaded, "classes", AggregationPolicy.SUM),
                new ProfilerResult("@classload.loaded.total", loadedClassCount, "classes", AggregationPolicy.MAX),
                new ProfilerResult("@classload.unloaded.total", unloadedClassCount, "classes", AggregationPolicy.MAX)
        );
    }

}
