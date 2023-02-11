package org.protobj.mock.module.role;

import com.guangyu.cd003.projects.gs.module.role.msg.RespRoleInfo;

public class RoleData {
    public RespRoleInfo respRoleInfo;

    public void handle(RespRoleInfo respRoleInfo) {
        this.respRoleInfo = respRoleInfo;
    }
}
