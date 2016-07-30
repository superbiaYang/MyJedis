package indi.superbiayang.redis;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;

/**
 * @author SuperbiaYang
 * Load the configuration based on the config.xml
 * Including redis connection configuration and mapping
 */
public class RedisConfigLoader {
    public static void load(String configLocation) {
        SAXReader reader = new SAXReader();
        try {
            InputStream stream = RedisFactory.class.getResourceAsStream(configLocation);
            Document document = reader.read(stream);
            Element root = document.getRootElement();
            RedisFactory.getInstance().init(root);
            ClassConfig.getInstance().init(root);
        } catch (DocumentException | ClassNotFoundException | RedisException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
