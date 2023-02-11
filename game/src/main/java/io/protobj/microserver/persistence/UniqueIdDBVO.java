package io.protobj.microserver.persistence;

import com.pv.common.utilities.persistence.DBObj;
import com.pv.common.utilities.persistence.DBVO;


public abstract class UniqueIdDBVO<T> extends DBVO {

    protected T id;

    public UniqueIdDBVO() {
        super();
    }

    public UniqueIdDBVO(DBObj dbObj) {
        super(dbObj);
    }

    @Override
    public T getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(Object pk) {
        this.id = (T) pk;
    }

    @Override
    public Integer getCid() {
        return null;
    }

    @Override
    public void setCid(Integer cid) {

    }
    @Override
    protected Object[] initRelKeys() {
        return new Object[0];
    }

    @Override
    protected void setRelKeysFromFields(Object[] objects) {

    }

    @Override
    protected void setFieldsFromRelKeys(Object[] objects) {

    }
    
    
    @Override
    protected Object initPrimaryKey() {
        return getPrimaryKey();
    }
}
