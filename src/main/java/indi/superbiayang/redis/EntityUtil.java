package indi.superbiayang.redis;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

/**
 * @author SuperbiaYang
 * An operation class for RedisEntity
 */
class EntityUtil<T> {
    T entity;
    EntityConfig config;

    EntityUtil(T entity) throws RedisException {
        Class<?> c = entity.getClass();
        this.config = ClassConfig.getInstance().getConfig(c);
        if (this.config == null) {
            throw new RedisException("Invalid class "+ c.getClass().getName());
        }
        this.entity = entity;
    }

    EntityUtil(Class<T> c, Object key) throws RedisException {
        this.config = ClassConfig.getInstance().getConfig(c);
        if (this.config == null) {
            throw new RedisException("Invalid class "+ c.getClass().getName());
        }
        try {
            this.entity = c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RedisException("Failed to create class " + c.getName(), e);
        }

        Field field = config.getKeyField();
        setValue(field, key);
    }

    T getEntity() {
        return entity;
    }

    void save(Jedis jedis) throws RedisException {
        List<Field> list = config.getDataFields();
        Map<String, String> hash = new HashMap<>();
        Set<String> del = new HashSet<>();
        for(Field field:list) {
            Object val = getVal(field);
            if (val == null) {
                del.add(field.getName());
            } else {
                try {
                    hash.put(field.getName(), StringUtil.toString(val));
                } catch (IOException e) {
                    throw new RedisException("Can't transfer val to string ", e);
                }
            }
        };
        String key = genRedisKey();
        if (!del.isEmpty()) {
            jedis.hdel(key, del.toArray(new String[del.size()]));
        }
        if (!hash.isEmpty()) {
            jedis.hmset(key, hash);
        }
    }

    boolean load(Jedis jedis) throws RedisException {
        String key = genRedisKey();
        List<Field> list = config.getDataFields();
        Map<String, String> allValue = jedis.hgetAll(key);
        if (allValue.isEmpty()) {
            //Not in the redis
            return false;
        }
        for (Field field : list) {
            Object val = null;
            String str = allValue.get(field.getName());
            if (str != null) {
                try {
                    val= StringUtil.parseString(str, (Class<?>) field.getGenericType());
                } catch (IOException e) {
                    throw new RedisException("Can't parse string "+ str, e);
                }
            }
            setValue(field, val);
        }
        return true;
    }

    boolean loadVolatileData(Jedis jedis) throws RedisException {
        String key = genRedisKey();
        if (!jedis.exists(key)) {
        	//The data has been removed
            return false;
        }
        List<Field> list = config.getVolatileFields();
        Map<Field, Object> cache = new HashMap<>();
        for (Field field : list) {
            Object val = null;
            String str = jedis.hget(key, field.getName());
            if (str != null) {
                try {
                    val = StringUtil.parseString(str, field.getType());
                } catch (IOException e) {
                	throw new RedisException("Can't parse string "+ str, e);
                }
            }
            cache.put(field, val);
        }
        for (Field field : list) {
        	setValue(field, cache.get(field));
        }
        return true;
    }

    void delete(Jedis jedis) throws RedisException {
        jedis.del(genRedisKey());
    }

    private Object getVal(Field field) throws RedisException {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            return callGetter(field);
        }
    }

    private Object callGetter(Field field) throws RedisException {
        String fieldName = field.getName();
        String getterStr = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method getter = entity.getClass().getMethod(getterStr);
            return getter.invoke(entity);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            if (field.getType() == Boolean.class || field.getType() == Boolean.TYPE) {
                String isStr = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                try {
                    Method is = entity.getClass().getMethod(isStr);
                    return is.invoke(entity);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e1) {
                    throw new RedisException("None of " + fieldName + ", " + getterStr + "(), " + isStr + "()  accessible");
                }
            }
            throw new RedisException("Neither " + fieldName + " nor " + getterStr + "() is accessible");
        }
    }

    private void setValue(Field field, Object val) throws RedisException {
        try {
            field.set(entity, val);
        } catch (IllegalAccessException e) {
            callSetter(entity, field, val);
        }
    }

    private void callSetter(Object obj, Field field, Object val) throws RedisException {
        String fieldName = field.getName();
        String setterStr = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method setter = obj.getClass().getMethod(setterStr, field.getType());
            setter.invoke(obj, val);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RedisException("Neither " + fieldName + " nor " + setterStr + "() is accessible");
        }
    }

    private String genRedisKey() throws RedisException {
        Field field = config.getKeyField();
        Object key = getVal(field);
        if (key == null) {
            throw new RedisException("Key should not be null");
        }
        Class<?> c = entity.getClass();
        StringBuilder keyBuilder = new StringBuilder(c.getName());
        keyBuilder.append("$");
        if (config.isBasicKey()) {
            keyBuilder.append(key);
        } else {
            try {
                keyBuilder.append(StringUtil.bean2JsonOrThrow(key));
            } catch (IOException e) {
                throw new RedisException("Can't generate json of complex key " + field.getName()
                        + " of class " + c.getName(), e);
            }
        }
        return keyBuilder.toString();
    }
}
