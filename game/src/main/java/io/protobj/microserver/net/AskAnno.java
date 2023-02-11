package io.protobj.microserver.net;

import java.lang.annotation.*;

/**
 * Created on 2021/7/3.
 *
 * @author chen qiang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AskAnno {
}
