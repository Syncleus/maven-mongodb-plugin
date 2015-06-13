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

import com.syncleus.maven.plugins.mongodb.log.Loggers;
import com.syncleus.maven.plugins.mongodb.log.Loggers.LoggingStyle;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.exceptions.DistributionException;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.extract.UserTempNaming;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.IArtifactStore;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;

/**
 * When invoked, this goal starts an instance of mongo. The required binaries
 * are downloaded if no mongo release is found in <code>~/.embedmongo</code>.
 *
 * @see <a
 * href="http://github.com/flapdoodle-oss/embedmongo.flapdoodle.de">http://github.com/flapdoodle-oss/embedmongo.flapdoodle.de</a>
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartMongoMojo extends AbstractMongoMojo {

    private static final String PACKAGE_NAME = StartMongoMojo.class.getPackage().getName();
    public static final String MONGOD_CONTEXT_PROPERTY_NAME = PACKAGE_NAME + ".mongod";

    /**
     * The port MongoDB should run on.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.port", defaultValue = "27017")
    private int port;

    /**
     * Whether a random free port should be used for MongoDB instead of the one
     * specified by {@code port}. If {@code randomPort} is {@code true}, the
     * random port chosen will be available in the Maven project property
     * {@code embedmongo.port}.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.randomPort", defaultValue = "false")
    private boolean randomPort;

    /**
     * The version of MongoDB to run e.g. 2.1.1, 1.6 v1.8.2, V2_0_4,
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.version")
    private String version;

    /**
     * The location of a directory that will hold the MongoDB data files.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.databaseDirectory")
    private File databaseDirectory;

    /**
     * An IP address for the MongoDB instance to be bound to during its
     * execution.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.bindIp")
    private String bindIp;

    /**
     * A proxy hostname to be used when downloading MongoDB distributions.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.proxyHost")
    private String proxyHost;

    /**
     * A proxy port to be used when downloading MongoDB distributions.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.proxyPort")
    private int proxyPort;

    /**
     * Block immediately and wait until MongoDB is explicitly stopped (eg:
     * {@literal <ctrl-c>}). This option makes this goal similar in spirit to
     * something like jetty:run, useful for interactive debugging.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.wait", defaultValue = "false")
    private boolean wait;

    /**
     * Specifies where log output goes to. Must be one of the following: file, console, none.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.logging", defaultValue = "console")
    private String logging;

    /**
     * The file to log the output to.
     *
     * @since 1.0.0
     */
    @Parameter(property = "logFile", defaultValue = "mongodb.log")
    private String logFile;

    /**
     * Log file encoding type to use.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.logFileEncoding", defaultValue = "utf-8")
    private String logFileEncoding;

    /**
     * The base URL to be used when downloading MongoDB
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.downloadPath", defaultValue = "http://fastdl.mongodb.org/")
    private String downloadPath;

    /**
     * The proxy user to be used when downloading MongoDB
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.proxyUser")
    private String proxyUser;

    /**
     * The proxy password to be used when downloading MondoDB
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.proxyPassword")
    private String proxyPassword;

    /**
     * Should authorization be enabled for MongoDB
     *
     */
    @Parameter(property = "mongodb.authEnabled", defaultValue = "false")
    private boolean authEnabled;

    /**
     * Sets a value for the --replSet
     *
     */
    @Parameter(property = "mongodb.replSet")
    private String replSet;

    /**
     * Set the size for the MongoDB oplog
     *
     */
    @Parameter(property = "mongodb.oplogSize", defaultValue = "0")
    private int oplogSize;

    /**
     * Specifies the executable naming policy used. Must be one of the following values: uuid, user.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.exeutableNaming", defaultValue = "uuid")
    private String executableNaming;

    /**
     * Specifies the directory to which MongoDB executables are stores.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.artifactDirectory")
    private String artifactDirectory;

    /**
     * Specifies the sync delay for MongoDB, 0 never syncs to disk, no value indicates default.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.syncDelay")
    private Integer syncDelay;

    /**
     * The maven project.
     *
     * @since 1.0.0
     */
    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    /**
     * A list of the MongoDB features enabled.
     *
     * @since 1.0.0
     */
    @Parameter
    private String[] features;

    @Override
    @SuppressWarnings("unchecked")
    public void start() throws MojoExecutionException, MojoFailureException {

        if (this.proxyHost != null && this.proxyHost.length() > 0) {
            this.addProxySelector();
        }

        final MongodExecutable executable;
        try {
            final IRuntimeConfig runtimeConfig = createRuntimeConfig();

            if (randomPort)
                port = PortUtils.allocateRandomPort();
            savePortToProjectProperties();

            final IMongodConfig config = createMongodConfig();


            executable = MongodStarter.getInstance(runtimeConfig).prepare(config);
        }
        catch (final DistributionException e) {
            throw new MojoExecutionException("Failed to download MongoDB distribution: " + e.withDistribution(), e);
        }

        try {
            final MongodProcess mongod = executable.start();

            this.executeWait();

            getPluginContext().put(MONGOD_CONTEXT_PROPERTY_NAME, mongod);
        } catch (final IOException e) {
            throw new MojoExecutionException("Unable to start the mongod", e);
        }
    }

    private void executeWait() {
        if (wait) {
            while (true) {
                try {
                    TimeUnit.MINUTES.sleep(5);
                } catch (final InterruptedException e) {
                    break;
                }
            }
        }
    }

    private IMongodConfig createMongodConfig() throws MojoExecutionException {
        try {
            MongodConfigBuilder configBuilder = new MongodConfigBuilder()
                .version(createVersion())
                .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                .replication(new Storage(getDataDirectory(), replSet, oplogSize));

            configBuilder = this.configureSyncDelay(configBuilder);

            return configBuilder.build();
        }
        catch (UnknownHostException e) {
            throw new MojoExecutionException("Unable to determine if localhost is ipv6", e);
        }
        catch (IOException e) {
            throw new MojoExecutionException("Unable to Config MongoDB: ", e);
        }
    }

    private MongodConfigBuilder configureSyncDelay(final MongodConfigBuilder config) {
        if(this.syncDelay == null) {
            return config.cmdOptions(new MongoCmdOptionsBuilder()
                    .defaultSyncDelay()
                    .build());
        }
        else if(this.syncDelay > 0) {
            return config.cmdOptions(new MongoCmdOptionsBuilder()
                    .syncDelay(this.syncDelay)
                    .build());
        }
        else
            return config;
    }

    private ProcessOutput getOutputConfig() throws MojoFailureException {

        final LoggingStyle loggingStyle = LoggingStyle.valueOf(logging.toUpperCase());

        switch (loggingStyle) {
            case CONSOLE:
                return Loggers.console();
            case FILE:
                return Loggers.file(logFile, logFileEncoding);
            case NONE:
                return Loggers.none();
            default:
                throw new MojoFailureException("Unexpected logging style encountered: \"" + logging + "\" -> " +
                    loggingStyle);
        }

    }

    private IRuntimeConfig createRuntimeConfig() throws MojoFailureException {
        final ICommandLinePostProcessor commandLinePostProcessor;
        if (authEnabled) {
            commandLinePostProcessor = new ICommandLinePostProcessor() {
                @Override
                public List<String> process(final Distribution distribution, final List<String> args) {
                    args.remove("--noauth");
                    args.add("--auth");
                    return args;
                }
            };
        } else {
            commandLinePostProcessor = new ICommandLinePostProcessor.Noop();
        }

        return new RuntimeConfigBuilder()
            .defaults(Command.MongoD)
            .processOutput(getOutputConfig())
            .artifactStore(createArtifactStore())
            .commandLinePostProcessor(commandLinePostProcessor)
            .build();
    }

    private IArtifactStore createArtifactStore() throws MojoFailureException {
        final ITempNaming naming;
        if(executableNaming == null)
            throw new IllegalStateException("executableNaming should never be null!");
        else if(executableNaming.equals("uuid"))
            naming = new UUIDTempNaming();
        else if(executableNaming.equals("user"))
            naming = new UserTempNaming();
        else
            throw new MojoFailureException("Unexpected executable naming type encountered: \"" + executableNaming + "\"");

        de.flapdoodle.embed.process.config.store.DownloadConfigBuilder downloadConfig = new DownloadConfigBuilder().defaultsForCommand(Command.MongoD).downloadPath(downloadPath);
        if(artifactDirectory != null ) {
            IDirectory storePath = new FixedPath(artifactDirectory);
            downloadConfig = downloadConfig.artifactStorePath(storePath);
        }
        return new ArtifactStoreBuilder().defaults(Command.MongoD).download(downloadConfig.build()).executableNaming(naming).build();
    }

    private IFeatureAwareVersion createVersion() {

        final Feature[] features = getFeatures();

        if(this.version == null || this.version.equals("")) {
            if(features.length == 0)
                return Version.Main.PRODUCTION;
            this.version = Version.Main.PRODUCTION.asInDownloadPath();
        }

        String versionEnumName = this.version.toUpperCase().replaceAll("\\.", "_");

        if (versionEnumName.charAt(0) != 'V') {
            versionEnumName = "V" + versionEnumName;
        }

        IVersion determinedVersion;
        try {
            determinedVersion = Version.valueOf(versionEnumName);
        } catch (final IllegalArgumentException e) {
            getLog().warn("Unrecognised MongoDB version '" + this.version + "', this might be a new version that we don't yet know about. Attemping download anyway...");
            determinedVersion = new IVersion() {
                @Override
                public String asInDownloadPath() {
                    return version;
                }
            };
        }

        if(features.length == 0)
            return Versions.withFeatures(determinedVersion);
        else
            return Versions.withFeatures(determinedVersion, features);
    }

    private void addProxySelector() {

        // Add authenticator with proxyUser and proxyPassword
        if (proxyUser != null && proxyPassword != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                }
            });
        }

        final ProxySelector defaultProxySelector = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(final URI uri) {
                if (uri.getHost().equals("fastdl.mongodb.org")) {
                    return singletonList(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
                } else {
                    return defaultProxySelector.select(uri);
                }
            }

            @Override
            public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
            }
        });
    }

    private Feature[] getFeatures() {
        final HashSet<Feature> featuresSet = new HashSet<Feature>();
        if(this.features != null && this.features.length > 0) {
            for(final String featureString : this.features)
                featuresSet.add(Feature.valueOf(featureString.toUpperCase()));
        }
        return (Feature[]) featuresSet.toArray();
    }

    /**
     * Saves port to the {@link MavenProject#getProperties()} (with the property
     * name {@code mongodb.port}) to allow others (plugins, tests, etc) to
     * find the randomly allocated port.
     */
    private void savePortToProjectProperties() {
        project.getProperties().put("mongodb.port", String.valueOf(port));
    }

    private String getDataDirectory() {
        if (databaseDirectory != null) {
            return databaseDirectory.getAbsolutePath();
        } else {
            return null;
        }
    }

}
