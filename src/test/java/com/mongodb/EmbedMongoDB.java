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
package com.mongodb;

import org.bson.BsonDocument;

import java.net.UnknownHostException;
import java.util.Set;

public class EmbedMongoDB extends DB {

    public EmbedMongoDB(String name) throws UnknownHostException {
        super(new EmbedMongoClient(), name);
    }

    public CommandResult notOkErrorResult(String message) {
        CommandResult commandResult = new CommandResult(new BsonDocument(), new ServerAddress("localhost"));
        commandResult.put("errmsg", message);
        commandResult.put("ok", 0);
        return commandResult;
    }

    @Override
    public CommandResult doEval(String code, Object... args) {
        CommandResult commandResult = new CommandResult(new BsonDocument(), new ServerAddress("localhost"));
        commandResult.put("ok", 1.0);
        commandResult.put("retval", "null");
        return commandResult;
    }

    @Override
    protected DBCollection doGetCollection(String name) {
        return null;
    }

    @Override
    public Set<String> getCollectionNames() {
        return null;
    }
}
