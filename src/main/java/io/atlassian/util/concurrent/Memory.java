package io.atlassian.util.concurrent;

import java.util.Comparator;

/**
 * Value representing an amount of Memory, as measured in {@link Unit memory
 * units}.
 *
 * @since 2.5
 */
public final class Memory {
  //
  // static factory methods
  //

  /**
   * Construct a Memory represented in the supplied unit type.
   *
   * @param number a long.
   * @param unit a {@link io.atlassian.util.concurrent.Memory.Unit}.
   * @return a {@link io.atlassian.util.concurrent.Memory}.
   */
  public static Memory of(long number, Unit unit) {
    return new Memory(number, unit);
  }

  /**
   * Construct a Memory represented in bytes.
   *
   * @param number a long.
   * @return a {@link io.atlassian.util.concurrent.Memory}.
   */
  public static Memory bytes(long number) {
    return of(number, Unit.Bytes);
  }

  /**
   * Construct a Memory represented in kilobytes.
   *
   * @param number a long.
   * @return a {@link io.atlassian.util.concurrent.Memory}.
   */
  public static Memory kilobytes(long number) {
    return of(number, Unit.KB);
  }

  /**
   * Construct a Memory represented in megabytes.
   *
   * @param number a long.
   * @return a {@link io.atlassian.util.concurrent.Memory}.
   */
  public static Memory megabytes(long number) {
    return new Memory(number, Unit.MB);
  }

  /**
   * Construct a Memory represented in gigabytes.
   *
   * @param number a long.
   * @return a {@link io.atlassian.util.concurrent.Memory}.
   */
  public static Memory gigabytes(long number) {
    return new Memory(number, Unit.GB);
  }

  /**
   * Construct a Memory represented in terabytes.
   *
   * @param number a long.
   * @return a {@link io.atlassian.util.concurrent.Memory}.
   */
  public static Memory terabytes(long number) {
    return new Memory(number, Unit.TB);
  }

  //
  // members
  //

  private final long number;
  private final Unit unit;

  //
  // ctors
  //

  /**
   * Package private constructor, use a static factory method instead.
   */
  Memory(long number, Unit unit) {
    this.number = number;
    this.unit = unit;
  }

  //
  // methods
  //

  /**
   * The number of {@link Unit units} this represents.
   *
   * @return a long.
   */
  public long number() {
    return number;
  }

  /**
   * The memory {@link io.atlassian.util.concurrent.Memory.Unit} this is
   * represented in.
   *
   * @return a {@link io.atlassian.util.concurrent.Memory.Unit} object.
   */
  public Unit unit() {
    return unit;
  }

  /**
   * The number of bytes represented by this instance.
   *
   * @return a long.
   */
  public long bytes() {
    return number * unit.bytes;
  }

  /**
   * Convert to the supplied unit representation.
   * <p>
   * This may involve a loss of precision if the Unit is greater than the
   * current representation. This will always round down to the nearest complete
   * Unit. 2043B will be 1KB for instance.
   *
   * @param unit a {@link io.atlassian.util.concurrent.Memory.Unit} object.
   * @return a {@link io.atlassian.util.concurrent.Memory} object.
   */
  public Memory to(Memory.Unit unit) {
    return of(bytes() / unit.bytes, unit);
  }

  //
  // value object overrides
  //

  /** {@inheritDoc} */
  @Override public String toString() {
    return number + " " + unit;
  }

  /** {@inheritDoc} */
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (number ^ (number >>> 32));
    result = prime * result + ((unit == null) ? 0 : unit.hashCode());
    return result;
  }

  /** {@inheritDoc} */
  @Override public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if ((obj == null) || (getClass() != obj.getClass()))
      return false;
    Memory other = (Memory) obj;
    if (number != other.number)
      return false;
    return (unit == other.unit);
  }

  /**
   * Units in which memory is expressed.
   */
  public enum Unit {
    Bytes(0), KB(1), MB(2), GB(3), TB(4);

    final long bytes;

    Unit(int pow) {
      bytes = pow(1, pow);
    }

    private static final long pow(long amt, int power) {
      if (power < 1)
        return amt;
      // tail recursive, do not leak impl
      return pow(amt * 1024, power - 1);
    }
  }

  /**
   * Comparator for {@link Unit units}.
   */
  public enum MemoryComparator implements Comparator<Memory> {
    INSTANCE;

    @Override public int compare(Memory o1, Memory o2) {
      return cmp(o1.bytes() - o2.bytes());
    }
  }

  /**
   * Comparator for {@link Unit units}.
   */
  public enum UnitComparator implements Comparator<Memory.Unit> {
    INSTANCE;

    @Override public int compare(Unit o1, Unit o2) {
      return cmp(o1.bytes - o2.bytes);
    }
  }

  private static int cmp(long diff) {
    return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
  }
}
