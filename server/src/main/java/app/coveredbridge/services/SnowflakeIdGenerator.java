package app.coveredbridge.services;

import app.coveredbridge.constants.ApplicationConstants;
import app.coveredbridge.data.models.Server;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import org.hibernate.HibernateException;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class SnowflakeIdGenerator {

  @Inject
  Server server;

  HashMap<String, Snowflake> snowflakes = new HashMap<>();

  /**
   * Generates a unique ID based on the provided identifier name.
   *
   * @param idName The identifier name to generate the ID for
   * @return A unique long ID
   * @throws HibernateException if there is an error generating the ID
   */
  @WithSpan
  public String generate(String idName) throws HibernateException {
    Snowflake snowflake;
    if (snowflakes.containsKey(idName)) {
      snowflake = snowflakes.get(idName);
    } else {
      snowflake = new Snowflake(server.instanceNumber);
      snowflakes.put(idName, snowflake);
    }
    long newId = snowflake.nextId();
    return Long.toString(newId, Character.MAX_RADIX);
  }

  /**
   * Snowflake class that generates unique IDs.
   * <p>
   * This generator can handle up to 1024 instances, each generating up to 4096 IDs per millisecond.
   * <ul>
   *     <li>INSTANCE_BITS (10 bits): Can handle up to 2^10 = 1024 instances (0-1023 instance IDs).</li>
   *     <li>SEQUENCE_BITS (12 bits): Each instance can generate up to 2^12 = 4096 IDs per millisecond.</li>
   * </ul>
   * </p>
   */
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
