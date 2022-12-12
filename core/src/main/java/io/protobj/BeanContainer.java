package io.protobj;

public interface BeanContainer {
    <T> T getBeanByType(Class<T> clazz);


}
