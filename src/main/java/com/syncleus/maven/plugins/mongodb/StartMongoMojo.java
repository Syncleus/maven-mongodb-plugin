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
package com.syncleus.maven.plugins.mongodb;

import com.mongodb.*;
import com.syncleus.maven.plugins.mongodb.log.Loggers;
import com.syncleus.maven.plugins.mongodb.log.Loggers.LoggingStyle;
import de.flapdoodle.embed.mongo.*;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
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
     */
    @Parameter(property = "mongodb.authEnabled", defaultValue = "false")
    private boolean authEnabled;

    /**
     * Sets a value for the --replSet
     */
    @Parameter(property = "mongodb.replSet")
    private String replSet;

    /**
     * Set the size for the MongoDB oplog
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

    /**
     * A list of imports to be performed.
     *
     * @since 1.0.0
     */
    @Parameter
    private ImportDataConfig[] imports;

    /**
     * Default database to use when importing.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.defaultImportDatabase")
    private String defaultImportDatabase;

    /**
     * Specifies whether import operations should be forked in parallel or done sequentially.
     *
     * @since 1.0.0
     */
    @Parameter(property = "mongodb.parallel", defaultValue = "false")
    private boolean parallelImport;

    /**
     * A collection of initialization scripts to be run across the databases.
     *
     * @since 1.0.0
     */
    @Parameter
    private InitializerConfig[] initalizations;

    @Parameter
    private ReplSetInitiateConfig replSetInitiate;

    /**
     * Not a mojo configuration parameter, this is used internally.
     */
    private Integer setPort = null;

    /**
     * Not a mojo configuration parameter, this is used itnernally/
     */
    private Set<Feature> setFeatures = null;

    public StartMongoMojo() {
    }

    // this constructor is only used for unit testing purposes.
    StartMongoMojo(int port,
                   boolean randomPort,
                   String version,
                   File databaseDirectory,
                   String bindIp,
                   String proxyHost,
                   int proxyPort,
                   boolean wait,
                   String logging,
                   String logFile,
                   String logFileEncoding,
                   String downloadPath,
                   String proxyUser,
                   String proxyPassword,
                   boolean authEnabled,
                   String replSet,
                   int oplogSize,
                   String executableNaming,
                   String artifactDirectory,
                   Integer syncDelay,
                   MavenProject project,
                   String[] features,
                   ImportDataConfig[] imports,
                   String defaultImportDatabase,
                   boolean parallelImport,
                   Integer setPort,
                   InitializerConfig[] initalizations,
                   boolean skip) {

        super(skip);
        this.port = port;
        this.randomPort = randomPort;
        this.version = version;
        this.databaseDirectory = databaseDirectory;
        this.bindIp = bindIp;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.wait = wait;
        this.logging = logging;
        this.logFile = logFile;
        this.logFileEncoding = logFileEncoding;
        this.downloadPath = downloadPath;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.authEnabled = authEnabled;
        this.replSet = replSet;
        this.oplogSize = oplogSize;
        this.executableNaming = executableNaming;
        this.artifactDirectory = artifactDirectory;
        this.syncDelay = syncDelay;
        this.project = project;
        this.features = features;
        this.imports = imports;
        this.defaultImportDatabase = defaultImportDatabase;
        this.parallelImport = parallelImport;
        this.setPort = setPort;
        this.initalizations = initalizations;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start() throws MojoExecutionException, MojoFailureException {

        if (this.proxyHost != null && this.proxyHost.length() > 0) {
            this.addProxySelector();
        }

        final MongodExecutable executable;
        try {
            final IRuntimeConfig runtimeConfig = createRuntimeConfig();

            getPort();

            final IMongodConfig config = createMongodConfig();


            executable = MongodStarter.getInstance(runtimeConfig).prepare(config);
        } catch (final DistributionException e) {
            throw new MojoExecutionException("Failed to download MongoDB distribution: " + e.withDistribution(), e);
        }

        final MongodProcess mongod;
        try {
            mongod = executable.start();
        } catch (final IOException e) {
            throw new MojoExecutionException("Unable to start the mongod", e);
        }

        startReplSetInitiate();
        startImport();
        startInitialization();

        this.executeWait();
        if(getPluginContext() != null)
            getPluginContext().put(MONGOD_CONTEXT_PROPERTY_NAME, mongod);
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
                .net(new Net(bindIp, getPort(), Network.localhostIsIPv6()))
                .replication(new Storage(getDataDirectory(), replSet, oplogSize));

            configBuilder.cmdOptions(this.createCmdOptions().build());

            return configBuilder.build();
        } catch (final UnknownHostException e) {
            throw new MojoExecutionException("Unable to determine if localhost is ipv6", e);
        } catch (final IOException e) {
            throw new MojoExecutionException("Unable to Config MongoDB: ", e);
        }
    }

    private MongoCmdOptionsBuilder createCmdOptions() {
        MongoCmdOptionsBuilder config = new MongoCmdOptionsBuilder();
        config = this.configureSyncDelay(config);
        config = this.configureTextSearch(config);
        return config;
    }

    private MongoCmdOptionsBuilder configureSyncDelay(final MongoCmdOptionsBuilder config) {
        if (this.syncDelay == null)
            return config.defaultSyncDelay();
        return config.syncDelay(this.syncDelay);
    }

    private MongoCmdOptionsBuilder configureTextSearch(final MongoCmdOptionsBuilder config) {
        if(getFeatures().contains(Feature.TEXT_SEARCH))
            return config.enableTextSearch(true);
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
        if (executableNaming == null)
            throw new IllegalStateException("executableNaming should never be null!");
        else if (executableNaming.equals("uuid"))
            naming = new UUIDTempNaming();
        else if (executableNaming.equals("user"))
            naming = new UserTempNaming();
        else
            throw new MojoFailureException("Unexpected executable naming type encountered: \"" + executableNaming + "\"");

        de.flapdoodle.embed.process.config.store.DownloadConfigBuilder downloadConfig = new DownloadConfigBuilder().defaultsForCommand(Command.MongoD).downloadPath(downloadPath);
        if (artifactDirectory != null) {
            final IDirectory storePath = new FixedPath(artifactDirectory);
            downloadConfig = downloadConfig.artifactStorePath(storePath);
        }
        return new ArtifactStoreBuilder().defaults(Command.MongoD).download(downloadConfig.build()).executableNaming(naming).build();
    }

    private IFeatureAwareVersion createVersion() {

        final Feature[] features = getFeatures().toArray(new Feature[getFeatures().size()]);

        if (this.version == null || this.version.equals("")) {
            if (features.length == 0)
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

        if (features.length == 0)
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

    private Set<Feature> getFeatures() {
        if( setFeatures != null )
            return setFeatures;
        final HashSet<Feature> featuresSet = new HashSet<Feature>();
        if (this.features != null && this.features.length > 0) {
            for (final String featureString : this.features)
                featuresSet.add(Feature.valueOf(featureString.toUpperCase()));
        }
        this.setFeatures = featuresSet;
        return featuresSet;
    }

    private int getPort() {
        if (setPort != null)
            return setPort;

        if (randomPort)
            setPort = PortUtils.allocateRandomPort();
        else
            setPort = Integer.valueOf(port);
        project.getProperties().put("mongodb.port", String.valueOf(setPort));
        return setPort;
    }

    private String getDataDirectory() {
        if (databaseDirectory != null) {
            return databaseDirectory.getAbsolutePath();
        } else {
            return null;
        }
    }

    private void startImport() throws MojoExecutionException {
        if (imports == null || imports.length == 0)
            return;

        final List<MongoImportProcess> pendingMongoProcess = new ArrayList<MongoImportProcess>();

        getLog().info("Default import database: " + defaultImportDatabase);

        for (final ImportDataConfig importData : imports) {

            getLog().info("Import " + importData);

            verify(importData);
            String database = importData.getDatabase();

            if (StringUtils.isBlank(database)) {
                database = defaultImportDatabase;
            }

            try {
                final IMongoImportConfig mongoImportConfig = new MongoImportConfigBuilder()
                    .version(createVersion())
                    .net(new Net(bindIp, getPort(), Network.localhostIsIPv6()))
                    .db(database)
                    .collection(importData.getCollection())
                    .upsert(importData.getUpsertOnImport())
                    .dropCollection(importData.getDropOnImport())
                    .importFile(importData.getFile())
                    .jsonArray(true)
                    .timeout(new Timeout(importData.getTimeout()))
                    .build();

                final MongoImportExecutable mongoImport = MongoImportStarter.getDefaultInstance().prepare(mongoImportConfig);

                final MongoImportProcess importProcess = mongoImport.start();

                if (parallelImport)
                    pendingMongoProcess.add(importProcess);
                else
                    waitFor(importProcess);
            } catch (final IOException e) {
                throw new MojoExecutionException("Unexpected IOException encountered", e);
            }

        }

        for (final MongoImportProcess importProcess : pendingMongoProcess)
            waitFor(importProcess);

    }

    private void waitFor(final MongoImportProcess importProcess) throws MojoExecutionException {
        try {
            final int code = importProcess.waitFor();

            if (code != 0)
                throw new MojoExecutionException("Cannot import '" + importProcess.getConfig().getImportFile() + "'");

            getLog().info("Import return code: " + code);
        } catch (final InterruptedException e) {
            throw new MojoExecutionException("Thread execution interrupted", e);
        }

    }

    private void verify(final ImportDataConfig config) {
        Validate.notBlank(config.getFile(), "Import file is required\n\n" +
            "<imports>\n" +
            "\t<import>\n" +
            "\t\t<file>[my file]</file>\n" +
            "...");
        Validate.isTrue(StringUtils.isNotBlank(defaultImportDatabase) || StringUtils.isNotBlank(config.getDatabase()), "Database is required you can either define a defaultImportDatabase or a <database> on import tags");
        Validate.notBlank(config.getCollection(), "Collection is required\n\n" +
            "<imports>\n" +
            "\t<import>\n" +
            "\t\t<collection>[my file]</collection>\n" +
            "...");

    }


    private void startInitialization() throws MojoExecutionException, MojoFailureException {
        if (initalizations == null || initalizations.length == 0)
            return;

        for (final InitializerConfig initConfig : this.initalizations) {
            final DB db = connectToMongoAndGetDB(initConfig.getDatabaseName());

            for (final File scriptFile : initConfig.getScripts()) {
                if (scriptFile.isDirectory())
                    this.processScriptDirectory(db, scriptFile);
                else
                    this.processScriptFile(db, scriptFile);
            }
        }
    }

    @Deprecated
    DB connectToMongoAndGetDB(final String databaseName) throws MojoExecutionException {
        if (databaseName == null || databaseName.trim().length() == 0) {
            throw new MojoExecutionException("Database name is missing");
        }

        final MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", getPort()));
        getLog().info("Connected to MongoDB");
        return mongoClient.getDB(databaseName);
    }

    @Deprecated
    private void processScriptDirectory(final DB db, final File scriptDirectory) throws MojoExecutionException {
        final File[] files = scriptDirectory.listFiles();
        getLog().info("Folder " + scriptDirectory.getAbsolutePath() + " contains " + files.length + " file(s):");
        for (final File file : files) {
            this.processScriptFile(db, file);
        }
        getLog().info("Data initialized with success");
    }

    @Deprecated
    private void processScriptFile(final DB db, final File scriptFile) throws MojoExecutionException {
        Scanner scanner = null;
        final StringBuilder instructions = new StringBuilder();
        try {
            scanner = new Scanner(scriptFile);
            while (scanner.hasNextLine()) {
                instructions.append(scanner.nextLine()).append("\n");
            }
        } catch (final FileNotFoundException e) {
            throw new MojoExecutionException("Unable to find file with name '" + scriptFile.getName() + "'", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        final CommandResult result;
        try {
            final String evalString = "(function() {" + instructions.toString() + "})();";
            result = db.doEval(evalString, new Object[0]);
        } catch (final MongoException e) {
            throw new MojoExecutionException("Unable to execute file with name '" + scriptFile.getName() + "'", e);
        }
        if (!result.ok()) {
            getLog().error("- file " + scriptFile.getName() + " parsed with error: " + result.getErrorMessage());
            throw new MojoExecutionException("Error while executing instructions from file '" + scriptFile.getName() + "': " + result.getErrorMessage(), result.getException());
        }
        getLog().info("- file " + scriptFile.getName() + " parsed successfully");
    }

    private void startReplSetInitiate() throws MojoExecutionException, MojoFailureException {
        if(replSetInitiate == null)
            return;

        final MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", getPort()));
        getLog().info("Connected to MongoDB");
        final DB db = mongoClient.getDB("admin");

        getLog().info("would have initated: " + replSetInitiate.makeCommand().toString());
        db.command(new BasicDBObject("replSetInitiate", replSetInitiate.makeCommand()));
    }
}
