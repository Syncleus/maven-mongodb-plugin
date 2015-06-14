/**
 * Copyright: (c) Syncleus, Inc.
 *
 * You may redistribute and modify this source code under the terms and
 * conditions of the Open Source Community License - Type C version 1.0
 * or any later version as published by Syncleus, Inc. at www.syncleus.com.
 * There should be a copy of the license included with this file. If a copy
 * of the license is not included you are granted no right to distribute or
 * otherwise use this file except through a legal and valid license. You
 * should also contact Syncleus, Inc. at the information below if you cannot
 * find a license:
 *
 * Syncleus, Inc.
 * 2604 South 12th Street
 * Philadelphia, PA 19148
 */
/*
 * Derived from APACHE LICENSE version 2.0 source as indicated at
 * https://github.com/joelittlejohn/embedmongo-maven-plugin as of 5/16/2015.
 * Original source was Copyright (c) 2012 Joe Littlejohn
 */
package com.syncleus.maven.plugins.mongodb.log;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.config.MongodProcessOutputConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.NamedOutputStreamProcessor;

public class Loggers {

    public static ProcessOutput file(final String logFile, final String encoding) {
        final FileOutputStreamProcessor file = new FileOutputStreamProcessor(logFile, encoding);

        return new ProcessOutput(
            new NamedOutputStreamProcessor("[mongod output]", file),
            new NamedOutputStreamProcessor("[mongod error]", file),
            new NamedOutputStreamProcessor("[mongod commands]", file));
    }

    public static ProcessOutput console() {
        return MongodProcessOutputConfig.getDefaultInstance(Command.MongoD);
    }

    public static ProcessOutput none() {
        final NoopStreamProcessor noop = new NoopStreamProcessor();
        return new ProcessOutput(noop, noop, noop);
    }

    public enum LoggingStyle {
        FILE, CONSOLE, NONE
    }
}
