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

public class ImportDataConfig {
    private String database;
    private String collection;
    private String file;
    private Boolean dropOnImport = true;
    private Boolean upsertOnImport = true;
    private long timeout = 200000;

    public ImportDataConfig() {
    }

    public ImportDataConfig(final String database, final String collection, final String file, final Boolean dropOnImport, final Boolean upsertOnImport, final long timeout) {
        this.database = database;
        this.collection = collection;
        this.file = file;
        this.dropOnImport = dropOnImport;
        this.upsertOnImport = upsertOnImport;
        this.timeout = timeout;
    }

    public String getDatabase() {

        return database;
    }

    public String getCollection() {
        return collection;
    }

    public String getFile() {
        return file;
    }

    public Boolean getDropOnImport() {
        return dropOnImport;
    }

    public Boolean getUpsertOnImport() {
        return upsertOnImport;
    }

    public long getTimeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return "ImportDataConfig{" +
            "database='" + database + '\'' +
            ", collection='" + collection + '\'' +
            ", file='" + file + '\'' +
            ", dropOnImport=" + dropOnImport +
            ", upsertOnImport=" + upsertOnImport +
            ", timeout=" + timeout +
            '}';
    }
}
