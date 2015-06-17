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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.apache.maven.plugins.annotations.Parameter;
import java.util.HashMap;

public class ReplSetInitiateConfig {
    private String _id;
    private Integer version;
    private MembersConfig[] members;
    private SettingsConfig settings;

    public ReplSetInitiateConfig() {
    }

    public ReplSetInitiateConfig(String _id, Integer version, MembersConfig[] members, SettingsConfig settings) {
        this._id = _id;
        this.version = version;
        this.members = members;
        this.settings = settings;
    }

    public String get_id() {
        return _id;
    }

    public Integer getVersion() {
        return version;
    }

    public MembersConfig[] getMembers() {
        return members;
    }

    public SettingsConfig getSettings() {
        return settings;
    }

    public BasicDBObject makeCommand() {
        final BasicDBObject cmd = new BasicDBObject();
        if( _id != null && !_id.isEmpty())
            cmd.put("_id", _id);
        if(version != null)
            cmd.put("version", version);
        if( members != null && members.length > 0) {
            final BasicDBList dbMembers = new BasicDBList();
            for(final MembersConfig member : members )
                dbMembers.add(member.makeCommand());
            cmd.put("members", dbMembers);
        }
        if(settings != null)
            cmd.put("settings", settings.makeCommand());
        return cmd;
    }

    public static class MembersConfig {
        private Integer _id;
        private String host;
        private Boolean arbiterOnly;
        private Boolean buildIndexes;
        private Boolean hidden;
        private Integer priority;
        private HashMap<String, String> tags;
        private Integer slaveDelay;
        private Integer votes;

        public MembersConfig() {
        }

        public MembersConfig(Integer _id, String host, Boolean arbiterOnly, Boolean buildIndexes, Boolean hidden, Integer priority, HashMap<String, String> tags, Integer slaveDelay, Integer votes) {
            this._id = _id;
            this.host = host;
            this.arbiterOnly = arbiterOnly;
            this.buildIndexes = buildIndexes;
            this.hidden = hidden;
            this.priority = priority;
            this.tags = tags;
            this.slaveDelay = slaveDelay;
            this.votes = votes;
        }

        public Integer get_id() {
            return _id;
        }

        public String getHost() {
            return host;
        }

        public Boolean getArbiterOnly() {
            return arbiterOnly;
        }

        public Boolean getBuildIndexes() {
            return buildIndexes;
        }

        public Boolean getHidden() {
            return hidden;
        }

        public Integer getPriority() {
            return priority;
        }

        public HashMap<String, String> getTags() {
            return tags;
        }

        public Integer getSlaveDelay() {
            return slaveDelay;
        }

        public Integer getVotes() {
            return votes;
        }

        public BasicDBObject makeCommand() {
            final BasicDBObject cmd = new BasicDBObject();
            if(_id != null)
                cmd.put("_id", _id);
            if(host != null && !host.isEmpty())
                cmd.put("host", host);
            if(arbiterOnly != null)
                cmd.put("arbiterOnly", arbiterOnly);
            if(hidden != null)
                cmd.put("hidden", arbiterOnly);
            if(priority != null)
                cmd.put("priority", priority);
            if( tags != null && tags.size() > 0)
                cmd.put("tags", new BasicDBObject(tags));
            if(slaveDelay != null)
                cmd.put("slaveDelay", slaveDelay);
            if(votes != null)
                cmd.put("votes", votes);
            return cmd;
        }
    }

    public static class SettingsConfig {
        private Boolean chainingAllowed;
        private Integer heartbeatTimeoutSecs;
        private GetLastErrorModesConfig[] getLastErrorModes;

        //TODO: implement getLastErrorDefaults

        public SettingsConfig() {
        }

        public SettingsConfig(Boolean chainingAllowed, Integer heartbeatTimeoutSecs, GetLastErrorModesConfig[] getLastErrorModes) {
            this.chainingAllowed = chainingAllowed;
            this.heartbeatTimeoutSecs = heartbeatTimeoutSecs;
            this.getLastErrorModes = getLastErrorModes;
        }

        public Boolean getChainingAllowed() {
            return chainingAllowed;
        }

        public Integer getHeartbeatTimeoutSecs() {
            return heartbeatTimeoutSecs;
        }

        public GetLastErrorModesConfig[] getGetLastErrorModes() {
            return getLastErrorModes;
        }

        public BasicDBObject makeCommand() {
            final BasicDBObject cmd = new BasicDBObject();
            if(chainingAllowed != null)
                cmd.put("chainingAllowed", chainingAllowed);
            if(heartbeatTimeoutSecs != null)
                cmd.put("heartbeatTimeoutSecs", heartbeatTimeoutSecs);
            if(getLastErrorModes != null && getLastErrorModes.length > 0) {
                final BasicDBObject dbGetLastErrorModes = new BasicDBObject();
                for( final GetLastErrorModesConfig getLastErrorMode : getLastErrorModes) {
                    getLastErrorMode.makeCommand(dbGetLastErrorModes);
                }
                cmd.put("getLastErrorModes", dbGetLastErrorModes);
            }
            return cmd;
        }

        public static class GetLastErrorModesConfig {
            private String writeConcern;
            private HashMap<String, String> tags;

            public GetLastErrorModesConfig() {
            }

            public GetLastErrorModesConfig(String writeConcern, HashMap<String, String> tags) {
                this.writeConcern = writeConcern;
                this.tags = tags;
            }

            public String getWriteConcern() {
                return writeConcern;
            }

            public HashMap<String, String> getTags() {
                return tags;
            }

            public void makeCommand(final BasicDBObject cmd) {
                if( tags != null && !tags.isEmpty()) {
                    final BasicDBObject dbTags = new BasicDBObject();
                    dbTags.putAll(tags);
                    cmd.put(writeConcern, dbTags);
                }
                else
                    cmd.put(writeConcern, new BasicDBObject());
            }
        }
    }
}
