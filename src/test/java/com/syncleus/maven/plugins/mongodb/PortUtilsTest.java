/**
 * Copyright: (c) Syncleus, Inc.
 * <p/>
 * You may redistribute and modify this source code under the terms and
 * conditions of the Open Source Community License - Type C version 1.0
 * or any later version as published by Syncleus, Inc. at www.syncleus.com.
 * There should be a copy of the license included with this file. If a copy
 * of the license is not included you are granted no right to distribute or
 * otherwise use this file except through a legal and valid license. You
 * should also contact Syncleus, Inc. at the information below if you cannot
 * find a license:
 * <p/>
 * Syncleus, Inc.
 * 2604 South 12th Street
 * Philadelphia, PA 19148
 */
package com.syncleus.maven.plugins.mongodb;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PortUtilsTest {

    private final ScheduledExecutorService testPooledExecutor = Executors.newScheduledThreadPool(20);

    @After
    public void tearDown() throws Exception {
        testPooledExecutor.shutdown();
    }

    /**
     * This test executes method
     * {@link PortUtils#allocateRandomPort()}
     * many times concurrently to make sure that port allocation works correctly
     * under stress.
     */
    @Test
    public void testAllocateRandomPort() throws Exception {
        final int testAllocationCount = 10000;
        final CountDownLatch allocationsCounter = new CountDownLatch(testAllocationCount);

        final Runnable allocatePort = new Runnable() {
            @Override
            public void run() {
                int port = -1;
                try {
                    port = PortUtils.allocateRandomPort();
                    new ServerSocket(port);
                    // port has been bound successfully
                } catch (final IOException e) {
                    throw new RuntimeException("Port " + port + " cannot be bind!");
                } finally {
                    allocationsCounter.countDown();
                }
            }
        };

        final Random randomGenerator = new Random();
        for (int i = 0; i < testAllocationCount; i++) {
            // schedule execution a little to in the future to simulate less predictable environment
            testPooledExecutor.schedule(allocatePort, randomGenerator.nextInt(10), TimeUnit.MILLISECONDS);
        }
        allocationsCounter.await(10, TimeUnit.SECONDS);
    }

}
