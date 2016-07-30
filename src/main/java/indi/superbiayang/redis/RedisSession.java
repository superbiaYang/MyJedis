package indi.superbiayang.redis;

import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;

/**
 * @author SuperbiaYang Start a new session use redis Use
 *         RedisFactory.getInstance().newSession(); to initialize Close the
 *         session when finished Recommend£º try (RedisSession session =
 *         RedisFactory.getInstance().newSession()) { //do something with
 *         session } In this case you needn't to call close() Other wise you
 *         need call close() manually
 */
public class RedisSession implements AutoCloseable {
	Jedis jedis;

	Set<RedisSet<?>> user = new HashSet<>();

	RedisSession(Jedis jedis) {
		this.jedis = jedis;
	}

	Jedis borrowJedis(RedisSet<?> user) {
		this.user.add(user);
		return jedis;
	}

	public void save(Object obj) throws RedisException {
		EntityUtil<?> util = new EntityUtil<>(obj);
		util.save(jedis);
	}

	public <T> T load(Class<T> c, Object key) throws RedisException {
		EntityUtil<T> util = new EntityUtil<>(c, key);
		if (!util.load(jedis)) {
			return null;
		}
		return util.getEntity();
	}

	public boolean loadVolatileData(Object obj) throws RedisException {
		EntityUtil<?> util = new EntityUtil<>(obj);
		return util.loadVolatileData(jedis);
	}

	public void delete(Class<?> c, Object key) throws RedisException {
		EntityUtil<?> util = new EntityUtil<>(c, key);
		util.delete(jedis);
	}

	@Override
	public void close() throws Exception {
		this.user.forEach(RedisSet::returnJeids);
		this.user.clear();
		this.jedis.close();
		this.jedis = null;
	}
}