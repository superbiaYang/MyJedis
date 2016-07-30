package indi.superbiayang.redis;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author SuperbiaYang Transfer between string and object Complex object will
 *         be transfer to json
 */
class StringUtil {
	public static String toString(Object obj) throws IOException {
		if (String.class == obj.getClass()) {
			return String.valueOf(obj);
		} else if (Boolean.class == obj.getClass()
				|| obj.getClass() == Boolean.TYPE) {
			return String.valueOf(obj);
		} else if (Integer.class == obj.getClass()
				|| obj.getClass() == Integer.TYPE) {
			return String.valueOf(obj);
		} else if (Long.class == obj.getClass() || obj.getClass() == Long.TYPE) {
			return String.valueOf(obj);
		} else if (Float.class == obj.getClass()
				|| obj.getClass() == Float.TYPE) {
			return String.valueOf(obj);
		} else if (Double.class == obj.getClass()
				|| obj.getClass() == Double.TYPE) {
			return String.valueOf(obj);
		} else {
			return bean2JsonOrThrow(obj);
		}
	}
	
	public static Object parseString(String str, Class<?> clazz)
			throws IOException {
		if (String.class == clazz) {
			return clazz.cast(str);
		} else if (Boolean.class == clazz || clazz == Boolean.TYPE) {
			return clazz.cast(Boolean.parseBoolean(str));
		} else if (Integer.class == clazz || clazz == Integer.TYPE) {
			return clazz.cast(Integer.parseInt(str));
		} else if (Long.class == clazz || clazz == Long.TYPE) {
			return clazz.cast(Long.parseLong(str));
		} else if (Float.class == clazz || clazz == Float.TYPE) {
			return clazz.cast(Float.parseFloat(str));
		} else if (Double.class == clazz || clazz == Double.TYPE) {
			return clazz.cast(Double.parseDouble(str));
		} else {
			return clazz.cast(json2BeanOrThrow(str, clazz));
		}
	}
	
	public static <T> void parseString(String str, T target, Class<? extends T> clazz)
			throws IOException {
		target = clazz.cast(parseString(str, clazz));
	}
	
	private static JavaType analysisType(Type type) {
		ObjectMapper mapper = new ObjectMapper();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> clazz = (Class<?>) parameterizedType.getRawType();
            Type[] typeParams = parameterizedType.getActualTypeArguments();
            JavaType[] javaTypes = new JavaType[typeParams.length];
            for (int i = 0; i < typeParams.length; i++) {
                javaTypes[i] = analysisType(typeParams[i]);
            }
            return mapper.getTypeFactory().constructParametricType(clazz, javaTypes);
        }
        return mapper.getTypeFactory().constructType(type);
    }

    private static Object json2BeanOrThrow(String jsonStr, Type type) throws IOException {
    	ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonStr, analysisType(type));
    }
    
    public static String bean2JsonOrThrow(Object obj) throws IOException {
        StringWriter sw = new StringWriter();

        JsonGenerator gen = new JsonFactory().createGenerator(sw);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(gen, obj);
        gen.close();
        return sw.toString();
    }
}
