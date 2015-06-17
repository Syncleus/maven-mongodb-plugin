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
package com.syncleus.maven.plugins.mongodb;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.EmbedMongoDB;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;

@RunWith(MockitoJUnitRunner.class)
public class StartMongoMojoTest {

    @Rule
    public TemporaryFolder createSchemaFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File rootFolder;
    private File rootFolderWithError;

    @Test
    public void testStartMongo() throws MojoFailureException, MojoExecutionException, IOException {
        initFolder();
        try {
            new StartMongoMojoForTest(37017, false, "3.0.2", null, null, null, 0, false, "console", null, "utf-8", "http://fastdl.mongodb.org/", null, null, false, null, 0,  "uuid", null, 0, new MavenProject(), null, null, null, false, null, new InitializerConfig[]{new InitializerConfig(new File[]{rootFolder}, "myDB")}, false).execute();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Should not fail!");
        }
    }

    @Test
    public void testStartMongoNoInitalizationDatabase() throws MojoFailureException, MojoExecutionException, IOException {
        initFolder();

        thrown.expect(MojoExecutionException.class);
        thrown.expectMessage("Database name is missing");

        new StartMongoMojo(47017, false, "3.0.2", null, null, null, 0, false,"console", null, "utf-8", "http://fastdl.mongodb.org/", null, null, false, null, 0,  "uuid", null, 0, new MavenProject(), null, null, null, false, null, new InitializerConfig[]{new InitializerConfig(new File[]{rootFolder}, null)}, false).execute();
    }

    @Test
    public void testStartMongoBadInitializationInstructions() throws IOException, MojoFailureException, MojoExecutionException {
        DB database = Mockito.mock(DB.class);
        initFolderWithError();

        CommandResult result = new EmbedMongoDB("myDB").notOkErrorResult("Error while executing instructions from file '" + rootFolderWithError.listFiles()[0].getName());
        BDDMockito.given(database.doEval(Matchers.anyString(), Matchers.<Object>anyVararg())).willReturn(result);

        thrown.expect(MojoExecutionException.class);
        thrown.expectMessage("Error while executing instructions from file '" + rootFolderWithError.listFiles()[0].getName());

        new StartMongoMojoForTest(57017, false, "3.0.2", null, null, null, 0, false, "console", null, "utf-8", "http://fastdl.mongodb.org/", null, null, false, null, 0,  "uuid", null, 0, new MavenProject(), null, null, null, false, null, new InitializerConfig[]{new InitializerConfig(new File[]{rootFolderWithError}, "myDB")}, false, database).execute();
    }

    private void initFolder() throws IOException {
        File instructionsFile = createSchemaFolder.newFile();
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(instructionsFile));
            out.write("db.dropDatabase();\n");
            out.write("db.users.createIndex( { email: 1 }, { unique: true } );\n");
        } finally {
            if (out != null) {
                out.close();
            }
        }
        rootFolder = instructionsFile.getParentFile();
        rootFolder.mkdir();
    }

    private void initFolderWithError() throws IOException {
        File instructionsFile = createSchemaFolder.newFile();
        BufferedWriter reader = null;
        try {
            reader = new BufferedWriter(new FileWriter(instructionsFile));
            reader.write("db.unknownInstruction();\n");
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        rootFolderWithError = instructionsFile.getParentFile();
        rootFolderWithError.mkdir();
    }

    static class StartMongoMojoForTest extends StartMongoMojo {

        private final DB database;

        public StartMongoMojoForTest(int port, boolean randomPort, String version, File databaseDirectory, String bindIp, String proxyHost, int proxyPort, boolean wait, String logging, String logFile, String logFileEncoding, String downloadPath, String proxyUser, String proxyPassword, boolean authEnabled, String replSet, int oplogSize, String executableNaming, String artifactDirectory, Integer syncDelay, MavenProject project, String[] features, ImportDataConfig[] imports, String defaultImportDatabase, Boolean parallelImport, Integer setPort, InitializerConfig[] initalizations, boolean skip) throws UnknownHostException {
            this(port, randomPort, version, databaseDirectory, bindIp, proxyHost, proxyPort, wait, logging, logFile, logFileEncoding, downloadPath, proxyUser, proxyPassword, authEnabled, replSet, oplogSize, executableNaming, artifactDirectory, syncDelay, project, features, imports, defaultImportDatabase, parallelImport, setPort, initalizations, skip, new EmbedMongoDB("myDB"));
        }

        public StartMongoMojoForTest(int port, boolean randomPort, String version, File databaseDirectory, String bindIp, String proxyHost, int proxyPort, boolean wait, String logging, String logFile, String logFileEncoding, String downloadPath, String proxyUser, String proxyPassword, boolean authEnabled, String replSet, int oplogSize, String executableNaming, String artifactDirectory, Integer syncDelay, MavenProject project, String[] features, ImportDataConfig[] imports, String defaultImportDatabase, Boolean parallelImport, Integer setPort, InitializerConfig[] initalizations, boolean skip, DB database) {
            super(port, randomPort, version, databaseDirectory, bindIp, proxyHost, proxyPort, wait, logging, logFile, logFileEncoding, downloadPath, proxyUser, proxyPassword, authEnabled, replSet, oplogSize, executableNaming, artifactDirectory, syncDelay, project, features, imports, defaultImportDatabase, parallelImport, setPort, initalizations, skip);
            this.database = database;
        }

        @Override
        DB connectToMongoAndGetDB(String databaseName) throws MojoExecutionException {
            return database;
        }
    }
}
