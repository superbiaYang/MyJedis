package indi.superbiayang.redis;

import java.util.List;
import java.util.NoSuchElementException;

import org.dom4j.Element;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author SuperbiaYang
 * Factory of RedisSession
 */
public class RedisFactory {
    private static RedisFactory instance;
    private JedisPool pool;

    private RedisFactory() {
        pool = null;
    }

    void init(Element root) {
        JedisPoolConfig config = new JedisPoolConfig();
        String ip = "localhost";
        int port = 6379;
        List<?> properties = root.elements("property");
        for (Object element : properties) {
            Element property = (Element) element;
            String name = property.attribute("name").getValue();
            switch (name) {
                case "ip":
                    ip = property.getText();
                    break;
                case "port":
                    port = Integer.parseInt(property.getText());
                    break;
                case "maxTotal":
                    config.setMaxTotal(Integer.parseInt(property.getText()));
                    break;
                case "blockWhenExhausted":
                    config.setBlockWhenExhausted(Boolean.parseBoolean(property.getText()));
                    break;
                case "maxWaitMillis":
                    config.setMaxWaitMillis(Integer.parseInt(property.getText()));
                    break;
                case "testOnBorrow":
                    config.setTestOnBorrow(Boolean.parseBoolean(property.getText()));
                    break;
            }
        }
        pool = new JedisPool(config, ip, port);
    }

    public static RedisFactory getInstance() {
        if (instance == null) {
            instance = new RedisFactory();
        }
        return instance;
    }
    
    Jedis getJedis() {
        try {
            return this.pool.getResource();
        } catch (JedisException e) {
            if (e.getCause() instanceof NoSuchElementException &&
                    e.getCause().getMessage().equals("Pool exhausted")) {
                this.pool.addObjects(1);
                return getJedis();
            }
            throw e;
        }

    }

    public RedisSession newSession() {
        return new RedisSession(this.getJedis());
    }
}
