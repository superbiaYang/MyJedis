package indi.superbiayang.redis;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * @author SuperbiaYang A set implement set with redis Call setSession with a
 *         valid RedisSession before any operation of the store This set is
 *         thread-safe
 */
public class RedisSet<E> implements Set<E> {

	String key = null;

	ThreadLocal<Jedis> jedisThreadLocal = new ThreadLocal<>();

	public void setKey(String key) {
		this.key = "SET#" + key;
	}

	public void setSession(RedisSession session) {
		jedisThreadLocal.set(session.borrowJedis(this));
	}

	void returnJeids() {
		jedisThreadLocal.remove();
	}

	private void validateEnvironment() {
		try {
			if (key == null) {
				throw new RedisException("Key is null");
			}

			if (jedisThreadLocal.get() == null) {
				throw new RedisException("setSession first");
			}
		} catch (RedisException e) {
			throw new RuntimeException(e);
		}
	}

	private void validateEnvironment(RedisSet<E> dstSet) {
		try {
			if (key == null || dstSet.key == null) {
				throw new RedisException("Key is null");
			}
			if (jedisThreadLocal.get() == null) {
				throw new RedisException("setSession first");
			}
		} catch (RedisException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	/**
	 * 
	 */
	public int size() {
		validateEnvironment();
		return Integer.class.cast(jedisThreadLocal.get().scard(key));
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		validateEnvironment();

		try {
			return jedisThreadLocal.get()
					.sismember(key, StringUtil.toString(o));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * The set on the redis is volatile other redis client may modify this So
	 * iterator is forbidden here
	 */
	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		validateEnvironment();
		return jedisThreadLocal.get().smembers(key).toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		validateEnvironment();
		String[] strings = new String[a.length];
		jedisThreadLocal.get().smembers(key).toArray(strings);
		for (int i = 0; i < strings.length; i++) {
			try {
				StringUtil.parseString(strings[i], a[i], a[i].getClass());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return a;
	}

	@Override
	public boolean add(E e) {
		validateEnvironment();
		try {
			return jedisThreadLocal.get().sadd(key, StringUtil.toString(e)) == 1;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean remove(Object o) {
		validateEnvironment();
		try {
			return jedisThreadLocal.get().srem(key, StringUtil.toString(o)) != 0;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		validateEnvironment();
		try {
			for (Object o : c) {
				if (!jedisThreadLocal.get().sismember(key,
						StringUtil.toString(o))) {
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		validateEnvironment();
		if (!(c instanceof RedisSet)) {
			throw new UnsupportedOperationException(
					"RedisSet<E> can only addAll with RedisSet<E>");
		}
		RedisSet<?> set = RedisSet.class.cast(c);
		return jedisThreadLocal.get().sunionstore(this.key, this.key, set.key) != 0;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		validateEnvironment();
		if (!(c instanceof RedisSet)) {
			throw new UnsupportedOperationException(
					"RedisSet<E> can only retainAll with RedisSet<E>");
		}
		RedisSet<?> set = RedisSet.class.cast(c);
		return jedisThreadLocal.get().sinterstore(this.key, this.key, set.key) != 0;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		validateEnvironment();
		if (!(c instanceof RedisSet)) {
			throw new UnsupportedOperationException(
					"RedisSet<E> can only removeAll with RedisSet<E>");
		}
		RedisSet<?> set = RedisSet.class.cast(c);
		return jedisThreadLocal.get().sdiffstore(this.key, this.key, set.key) != 0;
	}

	@Override
	public void clear() {
		validateEnvironment();
		jedisThreadLocal.get().del(key);
	}

	public E moveRandTo(RedisSet<E> dstSet, Class<E> clazz) {
		this.validateEnvironment(dstSet);

		E ret = null;
		do {
			String str = jedisThreadLocal.get().srandmember(this.key);
			if (str == null) {
				return null;
			}
			Long result = jedisThreadLocal.get().smove(this.key, dstSet.key,
					str);
			if (result == 0) {
				// In an extreme case, another client has operate the same set
				// in same time
				continue;
			}
			try {
				StringUtil.parseString(str, ret, clazz);
			} catch (IOException e) {
				e.printStackTrace();
				jedisThreadLocal.get().srem(this.key, str);
			}
		} while (ret == null);
		return ret;
	}

	public boolean moveTo(RedisSet<E> dstSet, E obj) {
		this.validateEnvironment(dstSet);

		try {
			String str = StringUtil.toString(obj);
			return jedisThreadLocal.get().smove(this.key, dstSet.key, str) != 0;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
