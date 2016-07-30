package indi.superbiayang.redis;

import java.lang.annotation.*;

/**
 * @author SuperbiaYang
 * Annotation for the class that need to be saved in redis
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisEntity {
}
