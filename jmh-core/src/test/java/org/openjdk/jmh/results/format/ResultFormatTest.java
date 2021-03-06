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
package org.openjdk.jmh.results.format;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * These tests seal the machine-readable format.
 * Any change to these tests should be discussed with the maintainers first!
 */
public class ResultFormatTest {

    private Collection<RunResult> getStub() {
        Collection<RunResult> results = new TreeSet<RunResult>(RunResult.DEFAULT_SORT_COMPARATOR);

        Random r = new Random(12345);
        for (int b = 0; b < r.nextInt(10); b++) {
            WorkloadParams ps = new WorkloadParams();
            for (int p = 0; p < 5; p++) {
                ps.put("param" + p, "value" + p, p);
            }
            BenchmarkParams params = new BenchmarkParams(
                    "benchmark_" + b,
                    JSONResultFormat.class.getName() + ".benchmark_" + b + "_" + Mode.Throughput,
                    false,
                    r.nextInt(1000),
                    new int[]{ r.nextInt(1000) },
                    r.nextInt(1000),
                    r.nextInt(1000),
                    new IterationParams(IterationType.WARMUP,      r.nextInt(1000), TimeValue.seconds(r.nextInt(1000)), 1),
                    new IterationParams(IterationType.MEASUREMENT, r.nextInt(1000), TimeValue.seconds(r.nextInt(1000)), 1),
                    Mode.Throughput,
                    ps,
                    TimeUnit.SECONDS, 1,
                    Utils.getCurrentJvm(),
                    Collections.<String>emptyList(),
                    TimeValue.days(1));

            Collection<BenchmarkResult> benchmarkResults = new ArrayList<BenchmarkResult>();
            for (int f = 0; f < r.nextInt(10); f++) {
                Collection<IterationResult> iterResults = new ArrayList<IterationResult>();
                for (int c = 0; c < r.nextInt(10); c++) {
                    IterationResult res = new IterationResult(params, params.getMeasurement());
                    res.addResult(new ThroughputResult(ResultRole.PRIMARY, "test", r.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary1", r.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary2", r.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    iterResults.add(res);
                }
                benchmarkResults.add(new BenchmarkResult(iterResults));
            }
            results.add(new RunResult(benchmarkResults));
        }
        return results;
    }

    private void compare(String actualFile, String goldenFile) throws IOException {
        BufferedReader actualReader = new BufferedReader(new FileReader(actualFile));
        BufferedReader goldenReader = new BufferedReader(new InputStreamReader(ResultFormatTest.class.getResourceAsStream("/org/openjdk/jmh/results/format/" + goldenFile)));
        while (true) {
            String goldenLine = goldenReader.readLine();
            String actualLine = actualReader.readLine();
            Assert.assertEquals("Mismatch", goldenLine, actualLine);
            if (goldenLine == null && actualLine == null) break;
        }
    }

    @Test
    public void jsonTest() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        ResultFormatFactory.getInstance(
                    ResultFormatType.JSON,
                    actualFile)
                .writeOut(getStub());

        compare(actualFile, "output-golden.json");
    }

    @Test
    public void jsonTest_Stream() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        PrintWriter pw = new PrintWriter(actualFile);
        ResultFormatFactory.getInstance(
                    ResultFormatType.JSON,
                    pw)
                .writeOut(getStub());
        pw.close();

        compare(actualFile, "output-golden.json");
    }

    @Test
    public void csvTest() throws IOException {
        String actualFile = FileUtils.tempFile("jmh").getAbsolutePath();

        ResultFormatFactory.getInstance(
                    ResultFormatType.CSV,
                    actualFile)
                .writeOut(getStub());

        compare(actualFile, "output-golden.csv");
    }

    @Test
    public void csvTest_Stream() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        PrintWriter pw = new PrintWriter(actualFile);
        ResultFormatFactory.getInstance(
                    ResultFormatType.CSV,
                    pw)
                .writeOut(getStub());
        pw.close();

        compare(actualFile, "output-golden.csv");
    }

    @Test
    public void scsvTest() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        ResultFormatFactory.getInstance(
                    ResultFormatType.SCSV,
                    actualFile)
                .writeOut(getStub());

        compare(actualFile, "output-golden.scsv");
    }

    @Test
    public void scsvTest_Stream() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        PrintWriter pw = new PrintWriter(actualFile);
        ResultFormatFactory.getInstance(
                    ResultFormatType.SCSV,
                    actualFile)
                .writeOut(getStub());
        pw.close();

        compare(actualFile, "output-golden.scsv");
    }


    @Test
    public void latexTest() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        ResultFormatFactory.getInstance(
                    ResultFormatType.LATEX,
                    actualFile)
                .writeOut(getStub());

        compare(actualFile, "output-golden.latex");
    }

    @Test
    public void latexTest_Stream() throws IOException {
        String actualFile = FileUtils.tempFile("test").getAbsolutePath();

        PrintWriter pw = new PrintWriter(actualFile);
        ResultFormatFactory.getInstance(
                    ResultFormatType.LATEX,
                    actualFile)
                .writeOut(getStub());
        pw.close();

        compare(actualFile, "output-golden.latex");
    }

}
