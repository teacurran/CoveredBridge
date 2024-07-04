package app.coveredbridge.services;

import app.coveredbridge.constants.ApplicationConstants;
import org.hibernate.HibernateException;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class SnowflakeIdGenerator {

  private final ServerInstance serverInstance;

  SnowflakeIdGenerator(ServerInstance serverInstance) {
    this.serverInstance = serverInstance;
  }

  HashMap<String, Snowflake> snowflakes = new HashMap<>();

  public Long generate(String idName) throws HibernateException {
    Snowflake snowflake;
    if (snowflakes.containsKey(idName)) {
      snowflake = snowflakes.get(idName);
    } else {
      snowflake = new Snowflake(serverInstance.getServer().instanceNumber);
      snowflakes.put(idName, snowflake);
    }
    return snowflake.nextId();
  }

  static class Snowflake {
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_INSTANCE_ID = (1L << INSTANCE_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private final ReentrantLock lock = new ReentrantLock();
    private final long instanceNumber;
    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private final AtomicLong sequence = new AtomicLong(0L);

    public Snowflake(int instanceNumber) {
      if(instanceNumber < 0 || instanceNumber > MAX_INSTANCE_ID) {
        throw new IllegalArgumentException(String.format("Instance %s out of range %d - %d", instanceNumber, 0, MAX_INSTANCE_ID));
      }
      this.instanceNumber = instanceNumber;
    }

    public long nextId() {
      lock.lock();
      try {
        var currentTimestamp = timestamp();

        if(currentTimestamp < lastTimestamp.get()) {
          throw new IllegalStateException("Invalid System Clock!");
        }

        if (currentTimestamp == lastTimestamp.get()) {
          sequence.set((sequence.get() + 1) & MAX_SEQUENCE);
          if(sequence.get() == 0) {
            currentTimestamp = waitForTimeToChange(currentTimestamp);
          }
        } else {
          sequence.set(0);
        }

        lastTimestamp.set(currentTimestamp);

        return currentTimestamp << (INSTANCE_BITS + SEQUENCE_BITS)
          | (instanceNumber << SEQUENCE_BITS)
          | sequence.get();
      } finally {
        lock.unlock();
      }
    }

    private long timestamp() {
      return Instant.now().toEpochMilli() - ApplicationConstants.APP_EPOCH;
    }

    private long waitForTimeToChange(long currentTimestamp) {
      while (currentTimestamp == lastTimestamp.get()) {
        currentTimestamp = timestamp();
      }
      return currentTimestamp;
    }
  }
}
