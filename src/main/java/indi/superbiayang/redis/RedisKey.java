package indi.superbiayang.redis;

import java.lang.annotation.*;

/**
 * @author SuperbiaYang
 * Annotation for the field that will be used as key of the RedisEntity when saving to the redis server
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisKey {

}
