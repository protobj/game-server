package io.protobj.redisaccessor;

public interface FieldDesc {

    short fieldType();//


    //是否有多个，是的话id必须自增
    default boolean many() {
        return false;
    }

    //主字段字段类型
    KeyDesc keyDesc();

}
