package io.protobj.microserver;

import com.beust.jcommander.Parameter;

import java.util.List;

public class BootConfig {
    @Parameter(names = {"-h", "--help"}, help = true, description = "help是帮助命令", order = 1)
    private boolean help;
    @Parameter(names = {"-svr", "--server"}, required = true, description = "描述服务器类型及id ", listConverter = SvrConfConverter.class, order = 2)
    private List<ServerConf> serverConfList;

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public List<ServerConf> getSvrConfList() {
        return serverConfList;
    }

    public void setSvrConfList(List<ServerConf> serverConfList) {
        this.serverConfList = serverConfList;
    }

}
