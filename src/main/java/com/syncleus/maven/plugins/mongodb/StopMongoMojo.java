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

import de.flapdoodle.embed.mongo.MongodProcess;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.inject.Inject;

/**
 * When invoked, this goal stops an instance of mojo that was started by this
 * plugin.
 *
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopMongoMojo extends AbstractMongoMojo {

    public StopMongoMojo() {
        super();
    }

    StopMongoMojo(boolean skip) {
        super(skip);
    }

    @Override
    public void start() throws MojoExecutionException, MojoFailureException {
        final MongodProcess mongod = (MongodProcess) getPluginContext().get(StartMongoMojo
            .MONGOD_CONTEXT_PROPERTY_NAME);

        if (mongod != null) {
            mongod.stop();
        } else {
            throw new MojoFailureException("No mongod process found, it appears embedmongo:start was not called");
        }
    }
}
