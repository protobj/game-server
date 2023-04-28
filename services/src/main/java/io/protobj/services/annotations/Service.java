package io.protobj.services.annotations;

import io.protobj.services.router.ServiceLookup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Service {

    //服务类型
    int st() default 0;

    //服务编号
    int ix() default 0;

//    Class<? extends ServiceLookup> router();
}
