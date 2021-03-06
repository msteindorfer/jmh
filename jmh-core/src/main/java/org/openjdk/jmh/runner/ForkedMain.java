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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.runner.link.BinaryLinkClient;
import org.openjdk.jmh.runner.options.Options;

import java.io.IOException;

/**
 * Main program entry point for forked JVM instance
 */
class ForkedMain {

    private static volatile boolean gracefullyFinished;
    private static volatile Exception exception;

    /**
     * Application main entry point
     *
     * @param argv Command line arguments
     */
    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            throw new IllegalArgumentException("Expected two arguments for forked VM");
        } else {
            try {
                // This assumes the exact order of arguments:
                //   1) host name to back-connect
                //   2) host port to back-connect
                String hostName = argv[0];
                int hostPort = Integer.valueOf(argv[1]);

                // establish the link to host VM and pull the options
                BinaryLinkClient link = new BinaryLinkClient(hostName, hostPort);
                addShutdownHook(link);

                Options options = link.requestOptions();

                // dump outputs into binary link
                System.setErr(link.getErrStream());
                System.setOut(link.getOutStream());

                // run!
                ForkedRunner runner = new ForkedRunner(options, link);
                runner.run();

                gracefullyFinished = true;
            } catch (Exception ex) {
                exception = ex;
                System.exit(1);
            }
        }
    }

    private static void addShutdownHook(final BinaryLinkClient link) {
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        if (!gracefullyFinished) {
                            Exception ex = exception;
                            if (ex == null) {
                                ex = new IllegalStateException(
                                        "<failure: VM prematurely exited before JMH had finished with it, " +
                                        "explicit System.exit was called?>");
                            }

                            String msg = ex.getMessage();

                            if (link != null) {
                                try {
                                    link.getOutputFormat().println(msg);
                                    link.pushException(new BenchmarkException(ex));
                                } catch (Exception e) {
                                    // last resort
                                    ex.printStackTrace(System.err);
                                }
                            } else {
                                // last resort
                                ex.printStackTrace(System.err);
                            }
                        }

                        if (link != null) {
                            try {
                                link.close();
                            } catch (IOException e) {
                                // swallow
                            }
                        }
                    }
                }
        );

    }

}
