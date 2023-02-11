package org.protobj.mock.module.legion;

import com.guangyu.cd003.projects.gs.module.legion.cons.LegionActionType;
import com.guangyu.cd003.projects.gs.module.legion.msg.RespLegion;
import com.guangyu.cd003.projects.gs.module.legion.msg.state.RespLegionStateIdle;
import io.netty.util.internal.ObjectPool;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class LegionState {

    private static ObjectPool<LegionState> OBJ_POOL = ObjectPool.newPool(LegionState::new);

    public static LegionState newLegionState() {
        return OBJ_POOL.get();
    }

    private ObjectPool.Handle<LegionState> handle;

    protected LegionState(ObjectPool.Handle<LegionState> handle) {
        this.handle = handle;
    }

    public static final int LEGION_ON_CALC_ING = -1;//正在计算

    public RespLegion legion;

    public long stateStartTime;

    public void setCalcing() {
        if (this.legion.actionType == LEGION_ON_CALC_ING) {
            return;
        }
        this.legion.actionType = LEGION_ON_CALC_ING;
        this.stateStartTime = System.currentTimeMillis();
        printState();
    }

    private void printState() {
        if (true) {
            return;
        }
        System.err.println(ExceptionUtils.getStackTrace(new Exception(getStateString())));
    }


    public void setState(int actionType) {
        this.legion.actionType = actionType;
        this.stateStartTime = System.currentTimeMillis();
        printState();
    }

    public String getStateString() {

        if (this.legion.actionType == LEGION_ON_CALC_ING) {
            return "LEGION_ON_CALC_ING";
        }
        for (LegionActionType value : LegionActionType.values()) {
            if (this.legion.actionType == value.getType()) {
                return value.name();
            }
        }
        throw new IllegalArgumentException("错误的state类型 :" + this.legion.actionType);
    }

    public boolean isIdle() {
        return legion.actionType == LegionActionType.WAIT.getType() || legion.state instanceof RespLegionStateIdle;
    }

    public void recycle() {
        this.stateStartTime = 0;
        legion = null;
        if (handle != null) {
            handle.recycle(this);
            handle = null;
        }
    }

    public int getCurState() {
        return this.legion.actionType;
    }

    public void update(RespLegion legion) {
        boolean onCalc = this. legion != null && this.legion.actionType == LEGION_ON_CALC_ING;

        this.legion = legion;
        if (onCalc) {
            this.legion.actionType = LEGION_ON_CALC_ING;
        } else {
            setState(legion.actionType);
        }
    }
}
