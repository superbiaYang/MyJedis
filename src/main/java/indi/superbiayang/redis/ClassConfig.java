package indi.superbiayang.redis;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

/**
 * @author SuperbiaYang
 * In order to optimized the runtime efficiency
 */
class ClassConfig {
    private static ClassConfig ourInstance = new ClassConfig();

    public static ClassConfig getInstance() {
        return ourInstance;
    }

    private ClassConfig() {
    }

    private Map<Class<?>, EntityConfig> configs = new HashMap<>();

    void init(Element root) throws RedisException, ClassNotFoundException {
        List<?> properties = root.elements("mapping");
        for (Object element : properties) {
            Element mapping = (Element) element;
            String className = mapping.attribute("class").getValue();
            Class<?> c = Class.forName(className);
            if (!c.isAnnotationPresent(RedisEntity.class)) {
                throw new RedisException("Class " + className + " is not present annotation RedisEntity");
            }
            EntityConfig config = new EntityConfig();
            Field[] fields = c.getDeclaredFields();
            List<Field> dataFields = new LinkedList<>();
            List<Field> volatileFields = new LinkedList<>();
            for (Field field : fields) {
                if (field.isAnnotationPresent(RedisKey.class)) {
                    config.setKeyField(field);
                    config.setBasicKey(isBasicType(field));
                } else if (field.isAnnotationPresent(RedisData.class)) {
                    dataFields.add(field);
                    RedisData annotation = field.getAnnotation(RedisData.class);
                    if (annotation.stability() == RedisData.Stability.VOLATILE) {
                        volatileFields.add(field);
                    }
                }
            }
            if (config.getKeyField() == null) {
                throw new RedisException("Can't find redis key of redis entity " + className);
            }
            config.setDataFields(dataFields);
            config.setVolatileFields(volatileFields);
            configs.put(c, config);
        }
    }

    EntityConfig getConfig(Class<?> c) {
        return configs.get(c);
    }

    public static boolean isBasicType(Field field) {
        if (String.class == field.getType()) {
            return true;
        } else if (Boolean.class == field.getType() || field.getType() == Boolean.TYPE) {
            return true;
        } else if (Integer.class == field.getType() || field.getType() == Integer.TYPE) {
            return true;
        } else if (Long.class == field.getType() || field.getType() == Long.TYPE) {
            return true;
        } else if (Float.class == field.getType() || field.getType() == Float.TYPE) {
            return true;
        } else if (Double.class == field.getType() || field.getType() == Double.TYPE) {
            return true;
        } else {
            return false;
        }
    }
}

class EntityConfig {
    private Field keyField;
    private boolean basicKey;
    private List<Field> dataFields;
    private List<Field> volatileFields;

    public Field getKeyField() {
        return keyField;
    }

    public void setKeyField(Field keyField) {
        this.keyField = keyField;
    }

    public boolean isBasicKey() {
        return basicKey;
    }

    public void setBasicKey(boolean basicKey) {
        this.basicKey = basicKey;
    }

    public List<Field> getDataFields() {
        return dataFields;
    }

    public void setDataFields(List<Field> dataFields) {
        this.dataFields = dataFields;
    }

    public List<Field> getVolatileFields() {
        return volatileFields;
    }

    public void setVolatileFields(List<Field> volatileFields) {
        this.volatileFields = volatileFields;
    }
}