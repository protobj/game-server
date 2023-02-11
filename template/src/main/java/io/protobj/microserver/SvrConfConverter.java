package io.protobj.microserver;

import com.beust.jcommander.IStringConverter;

public class SvrConfConverter implements IStringConverter<ServerConf> {
    @Override
    public ServerConf convert(String s) {
        // League#1#0-111
        String[] split = s.split("#");
        ServerConf serverConf = new ServerConf();
        serverConf.setSvrType(ServerType.valueOf(split[0]));
        serverConf.setSvrId(Integer.parseInt(split[1]));
        if (split.length > 2) {
            serverConf.setSlots(split[2]);
        }
        return serverConf;
    }
}
