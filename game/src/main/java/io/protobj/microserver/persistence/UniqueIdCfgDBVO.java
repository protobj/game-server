package io.protobj.microserver.persistence;

import com.guangyu.cd003.projects.microserver.loader.MicroServerModuleLoader;
import com.pv.common.utilities.cfg.csv.BaseCfg;
import com.pv.common.utilities.common.BeanFactory;
import com.pv.common.utilities.persistence.DBObj;
import com.pv.framework.gs.core.cfg.base.CfgDBVO;

public abstract class UniqueIdCfgDBVO<PK, T extends BaseCfg> extends CfgDBVO<T> {

    protected PK id;

    public UniqueIdCfgDBVO(Class<T> cfgClass) {
        super(cfgClass);
    }

    public UniqueIdCfgDBVO(Class<T> cfgClass, int cid) {
        super(cfgClass, cid);
    }

    public UniqueIdCfgDBVO(Class<T> cfgClass, T cfg) {
        super(cfgClass, cfg);
    }

    public UniqueIdCfgDBVO(Class<T> cfgClass, DBObj dbobj) {
        super(cfgClass, dbobj);
    }

    @Override
    public PK getPrimaryKey() {
        return this.id;
    }

    @Override
    public void setPrimaryKey(Object pk) {
        this.id = (PK) pk;
    }

    @Override
    public T getCfg() {
        T innerCfg = getInnerCfg();
        if (innerCfg != null) {
            return innerCfg;
        }
        T cfg = BeanFactory.getBean(MicroServerModuleLoader.class).getCfg(getCfgClass(), getCid());
        setInnerCfg(cfg);
        return cfg;
    }
}
