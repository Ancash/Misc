package de.ancash.misc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@SuppressWarnings("nls")
public class CustomReentrantReadWriteLock extends ReentrantReadWriteLock {

	private static final long serialVersionUID = 6029935387954940648L;

	public CustomReentrantReadWriteLock() {
		super();
	}

	public CustomReentrantReadWriteLock(boolean fair) {
		super(fair);
	}

	@Deprecated
	@Override
	public ReadLock readLock() {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public WriteLock writeLock() {
		throw new UnsupportedOperationException();
	}

	public <T> T readLock(Supplier<T> r) {
		super.readLock().lock();
		try {
			return r.get();
		} finally {
			super.readLock().unlock();
		}
	}

	public <T> T writeLock(Supplier<T> r) {
		try {
			if (!super.writeLock().tryLock(10, TimeUnit.SECONDS))
				throw new IllegalStateException(
						"could not acquire write lock within timeout limit (probably also holds read lock)");

			try {
				return r.get();
			} finally {
				super.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("interrupted locking");
		}
	}

	public void readLock(Runnable r) {
		super.readLock().lock();
		try {
			r.run();
		} finally {
			super.readLock().unlock();
		}
	}

	public void writeLock(Runnable r) {
		try {
			if (!super.writeLock().tryLock(10, TimeUnit.SECONDS))
				throw new IllegalStateException(
						"could not acquire write lock within timeout limit (probably also holds read lock)");

			try {
				r.run();
			} finally {
				super.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("interrupted locking");
		}
	}

	public <T> T conditionalReadWriteLock(Supplier<Boolean> condition, Supplier<T> success) {
		return conditionalReadWriteLock(condition, null, success);
	}

	public <T> T conditionalReadWriteLock(Supplier<Boolean> condition, Supplier<T> failed, Supplier<T> success) {
		super.readLock().lock();
		try {
			if (!condition.get()) {
				if (failed != null)
					return failed.get();
				return null;
			}
		} finally {
			super.readLock().unlock();
		}
		return conditionalWriteLock(condition, failed, success);
	}

	public <T> T conditionalReadLock(Supplier<Boolean> condition, Supplier<T> success) {
		return conditionalReadLock(condition, null, success);
	}

	public <T> T conditionalReadLock(Supplier<Boolean> condition, Supplier<T> failed, Supplier<T> success) {
		super.readLock().lock();
		try {
			if (!condition.get()) {
				if (failed != null)
					return failed.get();
				return null;
			}
			return success.get();
		} finally {
			super.readLock().unlock();
		}
	}

	public <T> T conditionalWriteLock(Supplier<Boolean> condition, Supplier<T> success) {
		return conditionalWriteLock(condition, null, success);
	}

	public <T> T conditionalWriteLock(Supplier<Boolean> condition, Supplier<T> failed, Supplier<T> success) {
		try {
			if (!super.writeLock().tryLock(10, TimeUnit.SECONDS))
				throw new IllegalStateException(
						"could not acquire write lock within timeout limit (probably also holds read lock)");

			try {
				if (!condition.get()) {
					if (failed != null)
						return failed.get();
					return null;
				}
				return success.get();
			} finally {
				super.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("interrupted locking");
		}

	}

	public boolean conditionalReadWriteLock(Supplier<Boolean> condition, Runnable success) {
		return conditionalReadWriteLock(condition, null, success);
	}

	public boolean conditionalReadWriteLock(Supplier<Boolean> condition, Runnable failed, Runnable success) {
		if (!conditionalReadLock(condition, failed, () -> getClass()))
			return false;
		return conditionalWriteLock(condition, failed, success);
	}

	/**
	 * Equal to conditionalReadLock(Supplier, null, Runnable) (See
	 * {@link CustomReentrantReadWriteLock#conditionalReadLock(Supplier, Runnable, Runnable)})
	 * 
	 * @param condition
	 * @param success
	 * @return
	 */
	public boolean conditionalReadLock(Supplier<Boolean> condition, Runnable success) {
		return conditionalReadLock(condition, null, success);
	}

	/**
	 * Acquires the {@link ReentrantReadWriteLock#readLock} and checks if the
	 * condition is true. If it is the 2nd runnable passed as an arg will be
	 * executed, else, if not null, the 1st will be executed.
	 * 
	 * @param condition
	 * @param failed
	 * @param success
	 * @return
	 */
	public boolean conditionalReadLock(Supplier<Boolean> condition, Runnable failed, Runnable success) {
		return conditionalLock(super.readLock(), condition, failed, success);
	}

	public boolean conditionalWriteLock(Supplier<Boolean> condition, Runnable success) {
		return conditionalWriteLock(condition, null, success);
	}

	public boolean conditionalWriteLock(Supplier<Boolean> condition, Runnable failed, Runnable success) {
		return conditionalLock(super.writeLock(), condition, failed, success);
	}

	private boolean conditionalLock(Lock l, Supplier<Boolean> condition, Runnable failed, Runnable success) {
		try {
			if (!l.tryLock(10, TimeUnit.SECONDS))
				throw new IllegalStateException("could not acquire " + l + " lock within timeout limit");
			try {
				if (!condition.get()) {
					if (failed != null)
						failed.run();
					return false;
				}
				success.run();
				return true;
			} finally {
				l.unlock();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("interrupted locking");
		}
	}
}
