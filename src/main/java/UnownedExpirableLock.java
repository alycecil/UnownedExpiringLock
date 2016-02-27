import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class UnownedExpirableLock implements Lock {

	private AtomicBoolean lockState = new AtomicBoolean(false);
	private Long lockExpiresAt = null;

	@Override
	public void lock() {
			try {
				lockInterruptibly();
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		for (;;) {
			if (tryLock())
				return;
			wait();
		}
	}

	public boolean tryLockExpireable(long leaseTime, TimeUnit unit)
			throws InterruptedException {

		boolean gotLock = false;
		synchronized (lockState) {
			if (!isLocked()) {
				lockState.set(true);
				gotLock = true;

				lockExpiresAt = System.currentTimeMillis()
						+ unit.toMillis(leaseTime);
			}
		}

		return gotLock;
	}
	
	@Override
	public boolean tryLock() {
		try {
			return tryLockExpireable(365l, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		long maxTime = System.currentTimeMillis() + unit.toMillis(time);

		do {
			if (tryLock()) {
				return true;
			}
		} while (maxTime > System.currentTimeMillis());

		return false;
	}

	@Override
	public void unlock() {
		synchronized (lockState) {
			lockState.set(false);
		}
	}

	@Override
	public Condition newCondition() {
		throw new IllegalStateException("This method is not supported");
	}

	public boolean isLocked() {
		return lockState.get()
				&& (lockExpiresAt != null && lockExpiresAt > System
						.currentTimeMillis());
	}

}
