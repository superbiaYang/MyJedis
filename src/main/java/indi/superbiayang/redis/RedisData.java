package indi.superbiayang.redis;

import java.lang.annotation.*;

/**
 * @author SuperbiaYang
 * Annotation for the field that need to be saved in redis
 * The class of the field should be annotated as RedisEntity
 * Supported variable type
 * int, long, double, float, boolean
 * Integer, Long, Double, Float, Boolean, String
 * Object that can be serialized and deserialized to json 
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisData {
    /**
     * If the field is annotated as VOLATILE then it will not be loaded when call loadVolatileData() in EntityUtil
     */
    Stability stability() default Stability.STABLE;

    enum Stability {
        STABLE,
        VOLATILE
    }
}
