package com.eouna.scmusicgenerator.utils;

import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Predicate;

/**
 * to see {@linkplain com.carrotsearch.sizeof.RamUsageEstimator}
 *
 * @author CCL
 */
public class MemUsageUtils {
  public static final boolean JRE_IS_64BIT;

  /** The value of <tt>System.getProperty("java.version")<tt>. * */
  public static final String JAVA_VERSION = System.getProperty("java.version");

  public static final String JAVA_VENDOR = System.getProperty("java.vendor");
  public static final String JVM_VENDOR = System.getProperty("java.vm.vendor");
  public static final String JVM_VERSION = System.getProperty("java.vm.version");
  public static final String JVM_NAME = System.getProperty("java.vm.name");
  public static final String OS_ARCH = System.getProperty("os.arch");
  public static final String OS_VERSION = System.getProperty("os.version");

  /** JVM diagnostic features. */
  public static enum JvmFeature {
    //
    OBJECT_REFERENCE_SIZE("Object reference size estimated using array index scale."),
    ARRAY_HEADER_SIZE("Array header size estimated using array based offset."),
    FIELD_OFFSETS("Shallow instance size based on field offsets."),
    OBJECT_ALIGNMENT("Object alignment retrieved from HotSpotDiagnostic MX gentemppath.bean.");

    public final String description;

    private JvmFeature(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return super.name() + " (" + description + ")";
    }
  }

  /** JVM info string for debugging and reports. */
  public static final String JVM_INFO_STRING;

  /** One kilobyte bytes. */
  public static final long ONE_KB = 1024;

  /** One megabyte bytes. */
  public static final long ONE_MB = ONE_KB * ONE_KB;

  /** One gigabyte bytes. */
  public static final long ONE_GB = ONE_KB * ONE_MB;

  /** No instantiation. */
  private MemUsageUtils() {}

  public static final int NUM_BYTES_BOOLEAN = 1;
  public static final int NUM_BYTES_BYTE = 1;
  public static final int NUM_BYTES_CHAR = 2;
  public static final int NUM_BYTES_SHORT = 2;
  public static final int NUM_BYTES_INT = 4;
  public static final int NUM_BYTES_FLOAT = 4;
  public static final int NUM_BYTES_LONG = 8;
  public static final int NUM_BYTES_DOUBLE = 8;

  /** Number of bytes this jvm uses to represent an object reference. */
  public static final int NUM_BYTES_OBJECT_REF;

  /** Number of bytes to represent an object header (no fields, no alignments). */
  public static final int NUM_BYTES_OBJECT_HEADER;

  /** Number of bytes to represent an array header (no content, but with alignments). */
  public static final int NUM_BYTES_ARRAY_HEADER;

  /**
   * A constant specifying the object alignment boundary inside the JVM. Objects will always take a
   * full multiple of this constant, possibly wasting some space.
   */
  public static final int NUM_BYTES_OBJECT_ALIGNMENT;

  /** Sizes of primitive classes. */
  private static final Map<Class<?>, Integer> primitiveSizes;

  static {
    primitiveSizes = new IdentityHashMap<Class<?>, Integer>();
    primitiveSizes.put(boolean.class, NUM_BYTES_BOOLEAN);
    primitiveSizes.put(byte.class, NUM_BYTES_BYTE);
    primitiveSizes.put(char.class, NUM_BYTES_CHAR);
    primitiveSizes.put(short.class, NUM_BYTES_SHORT);
    primitiveSizes.put(int.class, NUM_BYTES_INT);
    primitiveSizes.put(float.class, NUM_BYTES_FLOAT);
    primitiveSizes.put(double.class, NUM_BYTES_DOUBLE);
    primitiveSizes.put(long.class, NUM_BYTES_LONG);
    final String OS_ARCH = System.getProperty("os.arch");
    boolean is64Bit = false;
    try {
      final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      final Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      final Object unsafe = unsafeField.get(null);
      final int addressSize =
          ((Number) unsafeClass.getMethod("addressSize").invoke(unsafe)).intValue();
      is64Bit = addressSize >= 8;
    } catch (Exception e) {
      final String x = System.getProperty("sun.arch.data.model");
      if (x != null) {
        is64Bit = x.indexOf("64") != -1;
      } else {
        if (OS_ARCH != null && OS_ARCH.indexOf("64") != -1) {
          is64Bit = true;
        } else {
          is64Bit = false;
        }
      }
    }
    JRE_IS_64BIT = is64Bit;
  }

  /** A handle to <code>sun.misc.Unsafe</code>. */
  private static final Object theUnsafe;

  /** A handle to <code>sun.misc.Unsafe#fieldOffset(Field)</code>. */
  private static final Method objectFieldOffsetMethod;

  /** All the supported "internal" JVM features detected at clinit. */
  private static final EnumSet<MemUsageUtils.JvmFeature> supportedFeatures;

  /** Initialize constants and try to collect information about the JVM internals. */
  static {
    // Initialize empirically measured defaults. We'll modify them to the current
    // JVM settings later on if possible.
    int referenceSize = JRE_IS_64BIT ? 8 : 4;
    int objectHeader = JRE_IS_64BIT ? 16 : 8;
    // The following is objectHeader + NUM_BYTES_INT, but aligned (object alignment)
    // so on 64 bit JVMs it'll be align(16 + 4, @8) = 24.
    int arrayHeader = JRE_IS_64BIT ? 24 : 12;

    supportedFeatures = EnumSet.noneOf(MemUsageUtils.JvmFeature.class);

    Class<?> unsafeClass = null;
    Object tempTheUnsafe = null;
    try {
      unsafeClass = Class.forName("sun.misc.Unsafe");
      final Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      tempTheUnsafe = unsafeField.get(null);
    } catch (Exception e) {
      // Ignore.
    }
    theUnsafe = tempTheUnsafe;

    // get object reference size by getting scale factor of Object[] arrays:
    try {
      final Method arrayIndexScaleM = unsafeClass.getMethod("arrayIndexScale", Class.class);
      referenceSize = ((Number) arrayIndexScaleM.invoke(theUnsafe, Object[].class)).intValue();
      supportedFeatures.add(MemUsageUtils.JvmFeature.OBJECT_REFERENCE_SIZE);
    } catch (Exception e) {
      // ignore.
    }

    // "best guess" based on reference size. We will attempt to modify
    // these to exact values if there is supported infrastructure.
    objectHeader = JRE_IS_64BIT ? (8 + referenceSize) : 8;
    arrayHeader = JRE_IS_64BIT ? (8 + 2 * referenceSize) : 12;

    // get the object header size:
    // - first try out if the field offsets are not scaled (see warning in Unsafe docs)
    // - get the object header size by getting the field offset of the first field of a dummy object
    // If the scaling is byte-wise and unsafe is available, enable dynamic size measurement for
    // estimateRamUsage().
    Method tempObjectFieldOffsetMethod = null;
    try {
      final Method objectFieldOffsetM = unsafeClass.getMethod("objectFieldOffset", Field.class);
      final Field dummy1Field = MemUsageUtils.DummyTwoLongObject.class.getDeclaredField("dummy1");
      final int ofs1 = ((Number) objectFieldOffsetM.invoke(theUnsafe, dummy1Field)).intValue();
      final Field dummy2Field = MemUsageUtils.DummyTwoLongObject.class.getDeclaredField("dummy2");
      final int ofs2 = ((Number) objectFieldOffsetM.invoke(theUnsafe, dummy2Field)).intValue();
      if (Math.abs(ofs2 - ofs1) == NUM_BYTES_LONG) {
        final Field baseField = MemUsageUtils.DummyOneFieldObject.class.getDeclaredField("base");
        objectHeader = ((Number) objectFieldOffsetM.invoke(theUnsafe, baseField)).intValue();
        supportedFeatures.add(MemUsageUtils.JvmFeature.FIELD_OFFSETS);
        tempObjectFieldOffsetMethod = objectFieldOffsetM;
      }
    } catch (Exception e) {
      // Ignore.
    }
    objectFieldOffsetMethod = tempObjectFieldOffsetMethod;

    // Get the array header size by retrieving the array base offset
    // (offset of the first element of an array).
    try {
      final Method arrayBaseOffsetM = unsafeClass.getMethod("arrayBaseOffset", Class.class);
      // we calculate that only for byte[] arrays, it's actually the same for all types:
      arrayHeader = ((Number) arrayBaseOffsetM.invoke(theUnsafe, byte[].class)).intValue();
      supportedFeatures.add(MemUsageUtils.JvmFeature.ARRAY_HEADER_SIZE);
    } catch (Exception e) {
      // Ignore.
    }

    NUM_BYTES_OBJECT_REF = referenceSize;
    NUM_BYTES_OBJECT_HEADER = objectHeader;
    NUM_BYTES_ARRAY_HEADER = arrayHeader;

    // Try to get the object alignment (the default seems to be 8 on Hotspot,
    // regardless of the architecture).
    int objectAlignment = 8;
    try {
      final Class<?> beanClazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
      // Try to get the diagnostic mxbean without calling {@link
      // ManagementFactory#getPlatformMBeanServer()}
      // which starts AWT thread (and shows junk in the dock) on a Mac:
      Object hotSpotBean;
      // Java 7+, HotSpot
      try {
        hotSpotBean =
            ManagementFactory.class
                .getMethod("getPlatformMXBean", Class.class)
                .invoke(null, beanClazz);
      } catch (Exception e1) {
        // Java 6, HotSpot
        try {
          Class<?> sunMF = Class.forName("sun.management.ManagementFactory");
          hotSpotBean = sunMF.getMethod("getDiagnosticMXBean").invoke(null);
        } catch (Exception e2) {
          // Last resort option is an attempt to get it from ManagementFactory's server anyway (may
          // start AWT).
          hotSpotBean =
              ManagementFactory.newPlatformMXBeanProxy(
                  ManagementFactory.getPlatformMBeanServer(),
                  "com.sun.management:type=HotSpotDiagnostic",
                  beanClazz);
        }
      }
      if (hotSpotBean != null) {
        final Method getVMOptionMethod = beanClazz.getMethod("getVMOption", String.class);
        final Object vmOption = getVMOptionMethod.invoke(hotSpotBean, "ObjectAlignmentInBytes");
        objectAlignment =
            Integer.parseInt(vmOption.getClass().getMethod("getValue").invoke(vmOption).toString());
        supportedFeatures.add(MemUsageUtils.JvmFeature.OBJECT_ALIGNMENT);
      }
    } catch (Exception e) {
      // Ignore.
    }

    NUM_BYTES_OBJECT_ALIGNMENT = objectAlignment;

    JVM_INFO_STRING =
        "[JVM: "
            + JVM_NAME
            + ", "
            + JVM_VERSION
            + ", "
            + JVM_VENDOR
            + ", "
            + JAVA_VENDOR
            + ", "
            + JAVA_VERSION
            + "]";
  }

  /** Cached information about a given class. */
  private static final class ClassCache {
    public final long alignedShallowInstanceSize;
    public final Field[] referenceFields;

    public ClassCache(long alignedShallowInstanceSize, Field[] referenceFields) {
      this.alignedShallowInstanceSize = alignedShallowInstanceSize;
      this.referenceFields = referenceFields;
    }
  }

  // Object with just one field to determine the object header size by getting the offset of the
  // dummy field:
  @SuppressWarnings("unused")
  private static final class DummyOneFieldObject {
    public byte base;
  }

  // Another test object for checking, if the difference in offsets of dummy1 and dummy2 is 8 bytes.
  // Only then we can be sure that those are real, unscaled offsets:
  @SuppressWarnings("unused")
  private static final class DummyTwoLongObject {
    public long dummy1, dummy2;
  }

  /**
   * Returns true, if the current JVM is fully supported by {@code MemUsageHelper}. If this method
   * returns {@code false} you are maybe using a 3rd party Java VM that is not supporting Oracle/Sun
   * private APIs. The memory estimates can be imprecise then (no way of detecting compressed
   * references, alignments, etc.). Lucene still tries to use sensible defaults.
   */
  public static boolean isSupportedJVM() {
    return supportedFeatures.size() == MemUsageUtils.JvmFeature.values().length;
  }

  /** Aligns an object size to be the next multiple of {@link #NUM_BYTES_OBJECT_ALIGNMENT}. */
  public static long alignObjectSize(long size) {
    size += (long) NUM_BYTES_OBJECT_ALIGNMENT - 1L;
    return size - (size % NUM_BYTES_OBJECT_ALIGNMENT);
  }

  /** Returns the size in bytes of the byte[] object. */
  public static long sizeOf(byte[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + arr.length);
  }

  /** Returns the size in bytes of the boolean[] object. */
  public static long sizeOf(boolean[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + arr.length);
  }

  /** Returns the size in bytes of the char[] object. */
  public static long sizeOf(char[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_CHAR * arr.length);
  }

  /** Returns the size in bytes of the short[] object. */
  public static long sizeOf(short[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_SHORT * arr.length);
  }

  /** Returns the size in bytes of the int[] object. */
  public static long sizeOf(int[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_INT * arr.length);
  }

  /** Returns the size in bytes of the float[] object. */
  public static long sizeOf(float[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_FLOAT * arr.length);
  }

  /** Returns the size in bytes of the long[] object. */
  public static long sizeOf(long[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_LONG * arr.length);
  }

  /** Returns the size in bytes of the double[] object. */
  public static long sizeOf(double[] arr) {
    return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_DOUBLE * arr.length);
  }

  /**
   * Estimates the RAM usage by the given object. It will walk the object tree and sum up all
   * referenced objects.
   *
   * <p><b>Resource Usage:</b> This method internally uses a set of every object seen during
   * traversals so it does allocate memory (it isn't side-effect free). After the method exits, this
   * memory should be GCed.
   */
  public static long sizeOf(Object obj) {
    ArrayList<Object> stack = new ArrayList<Object>();
    stack.add(obj);
    return measureSizeOf(stack, null);
  }

  public static long sizeOf(Object obj, Predicate<Field> objectFilter) {
    ArrayList<Object> stack = new ArrayList<Object>();
    stack.add(obj);
    return measureSizeOf(stack, objectFilter);
  }

  /**
   * Same as {@link #sizeOf(Object)} but sums up all the arguments. Not an overload to prevent
   * accidental parameter conflicts with {@link #sizeOf(Object)}.
   */
  public static long sizeOfAll(Object... objects) {
    return sizeOfAll(Arrays.asList(objects));
  }

  /**
   * Same as {@link #sizeOf(Object)} but sums up all the arguments. Not an overload to prevent
   * accidental parameter conflicts with {@link #sizeOf(Object)}.
   */
  public static long sizeOfAll(Iterable<Object> objects) {
    final ArrayList<Object> stack;
    if (objects instanceof Collection<?>) {
      stack = new ArrayList<Object>(((Collection<?>) objects).size());
    } else {
      stack = new ArrayList<Object>();
    }

    for (Object o : objects) {
      stack.add(o);
    }

    return measureSizeOf(stack, null);
  }

  /**
   * Estimates a "shallow" memory usage of the given object. For arrays, this will be the memory
   * taken by array storage (no subreferences will be followed). For objects, this will be the
   * memory taken by the fields.
   *
   * <p>JVM object alignments are also applied.
   */
  public static long shallowSizeOf(Object obj) {
    if (obj != null) {
      final Class<?> clz = obj.getClass();
      if (clz.isArray()) {
        return shallowSizeOfArray(obj);
      } else {
        return shallowSizeOfInstance(clz);
      }
    } else {
      return 0;
    }
  }

  /**
   * Same as {@link #shallowSizeOf(Object)} but sums up all the arguments. Not an overload to
   * prevent accidental parameter conflicts with {@link #shallowSizeOf(Object)}.
   */
  public static long shallowSizeOfAll(Object... objects) {
    return shallowSizeOfAll(Arrays.asList(objects));
  }

  /**
   * Same as {@link #shallowSizeOf(Object)} but sums up all the arguments. Duplicate objects are not
   * resolved and will be counted twice. Not an overload to prevent accidental parameter conflicts
   * with {@link #shallowSizeOf(Object)}.
   */
  public static long shallowSizeOfAll(Iterable<Object> objects) {
    long sum = 0;
    for (Object o : objects) {
      sum += shallowSizeOf(o);
    }
    return sum;
  }

  /**
   * Returns the shallow instance size in bytes an instance of the given class would occupy. This
   * works with all conventional classes and primitive types, but not with arrays (the size then
   * depends on the number of elements and varies from object to object).
   *
   * @see #shallowSizeOf(Object)
   * @throws IllegalArgumentException if {@code clazz} is an array class.
   */
  public static long shallowSizeOfInstance(Class<?> clazz) {
    if (!clazz.isArray()) {
      if (clazz.isPrimitive()) {
        return primitiveSizes.get(clazz);
      }

      long size = NUM_BYTES_OBJECT_HEADER;

      // Walk type hierarchy
      for (; clazz != null; clazz = clazz.getSuperclass()) {
        final Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
          if (!Modifier.isStatic(f.getModifiers())) {
            size = adjustForField(size, f);
          }
        }
      }
      return alignObjectSize(size);
    } else {
      throw new IllegalArgumentException("This method does not work with array classes.");
    }
  }

  /** Return the set of unsupported JVM features that improve the estimation. */
  public static EnumSet<MemUsageUtils.JvmFeature> getUnsupportedFeatures() {
    EnumSet<MemUsageUtils.JvmFeature> unsupported = EnumSet.allOf(MemUsageUtils.JvmFeature.class);
    unsupported.removeAll(supportedFeatures);
    return unsupported;
  }

  /** Return the set of supported JVM features that improve the estimation. */
  public static EnumSet<MemUsageUtils.JvmFeature> getSupportedFeatures() {
    return EnumSet.copyOf(supportedFeatures);
  }

  /** Return shallow size of any <code>array</code>. */
  private static long shallowSizeOfArray(Object array) {
    long size = NUM_BYTES_ARRAY_HEADER;
    final int len = Array.getLength(array);
    if (len > 0) {
      Class<?> arrayElementClazz = array.getClass().getComponentType();
      if (arrayElementClazz.isPrimitive()) {
        size += (long) len * primitiveSizes.get(arrayElementClazz);
      } else {
        size += (long) NUM_BYTES_OBJECT_REF * len;
      }
    }
    return alignObjectSize(size);
  }

  /**
   * Non-recursive version of object descend. This consumes more memory than recursive in-depth
   * traversal but prevents stack overflows on long chains of objects or complex graphs (a max.
   * recursion depth on my machine was ~5000 objects linked in a chain so not too much).
   *
   * @param stack Root objects.
   */
  private static long measureSizeOf(ArrayList<Object> stack, Predicate<Field> objectFilter) {
    final IdentityHashSet<Object> seen = new IdentityHashSet<Object>();
    final IdentityHashMap<Class<?>, MemUsageUtils.ClassCache> classCache =
        new IdentityHashMap<Class<?>, MemUsageUtils.ClassCache>();

    long totalSize = 0;
    while (!stack.isEmpty()) {
      final Object ob = stack.remove(stack.size() - 1);

      if (ob == null || seen.contains(ob)) {
        continue;
      }
      seen.add(ob);

      final Class<?> obClazz = ob.getClass();
      if (obClazz.isArray()) {
        /*
         * Consider an array, possibly of primitive types. Push any of its references to
         * the processing stack and accumulate this array's shallow size.
         */
        long size = NUM_BYTES_ARRAY_HEADER;
        final int len = Array.getLength(ob);
        if (len > 0) {
          Class<?> componentClazz = obClazz.getComponentType();
          if (componentClazz.isPrimitive()) {
            size += (long) len * primitiveSizes.get(componentClazz);
          } else {
            size += (long) NUM_BYTES_OBJECT_REF * len;

            for (int i = len; --i >= 0; ) {
              final Object o = Array.get(ob, i);
              if (o != null && !seen.contains(o)) {
                stack.add(o);
              }
            }
          }
        }
        totalSize += alignObjectSize(size);
      } else {
        /*
         * Consider an object. Push any references it has to the processing stack
         * and accumulate this object's shallow size.
         */
        try {
          MemUsageUtils.ClassCache cachedInfo = classCache.get(obClazz);
          if (cachedInfo == null) {
            classCache.put(obClazz, cachedInfo = createCacheEntry(obClazz, objectFilter));
          }

          for (Field f : cachedInfo.referenceFields) {
            // Fast path to eliminate redundancies.
            final Object o = f.get(ob);
            if (o != null && !seen.contains(o)) {
              stack.add(o);
            }
          }

          totalSize += cachedInfo.alignedShallowInstanceSize;
        } catch (IllegalAccessException e) {
          // this should never happen as we enabled setAccessible().
          throw new RuntimeException("Reflective field access failed?", e);
        }
      }
    }

    // Help the GC.
    seen.clear();
    stack.clear();
    classCache.clear();

    return totalSize;
  }

  /** Create a cached information about shallow size and reference fields for a given class. */
  private static MemUsageUtils.ClassCache createCacheEntry(
      final Class<?> clazz, Predicate<Field> objectFilter) {
    MemUsageUtils.ClassCache cachedInfo;
    long shallowInstanceSize = NUM_BYTES_OBJECT_HEADER;
    final ArrayList<Field> referenceFields = new ArrayList<Field>(32);
    for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
      if (c == Class.class) {
        // Prevent inspection of Class' fields, throws SecurityException in Java 9
        continue;
      }
      final Field[] fields = c.getDeclaredFields();
      for (final Field f : fields) {
        if (!Modifier.isStatic(f.getModifiers())) {
          shallowInstanceSize = adjustForField(shallowInstanceSize, f);
          boolean needFilter = objectFilter == null || objectFilter.test(f);
          if (!f.getType().isPrimitive() && needFilter) {
            f.setAccessible(true);
            referenceFields.add(f);
          }
        }
      }
    }

    cachedInfo =
        new MemUsageUtils.ClassCache(
            alignObjectSize(shallowInstanceSize),
            referenceFields.toArray(new Field[referenceFields.size()]));
    return cachedInfo;
  }

  /**
   * This method returns the maximum representation size of an object. <code>sizeSoFar</code> is the
   * object's size measured so far. <code>f</code> is the field being probed.
   *
   * <p>The returned offset will be the maximum of whatever was measured so far and <code>f</code>
   * field's offset and representation size (unaligned).
   */
  private static long adjustForField(long sizeSoFar, final Field f) {
    final Class<?> type = f.getType();
    final int fsize = type.isPrimitive() ? primitiveSizes.get(type) : NUM_BYTES_OBJECT_REF;
    if (objectFieldOffsetMethod != null) {
      try {
        final long offsetPlusSize =
            ((Number) objectFieldOffsetMethod.invoke(theUnsafe, f)).longValue() + fsize;
        return Math.max(sizeSoFar, offsetPlusSize);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException("Access problem with sun.misc.Unsafe", ex);
      } catch (InvocationTargetException ite) {
        final Throwable cause = ite.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
          throw (Error) cause;
        }
        // this should never happen (Unsafe does not declare
        // checked Exceptions for this method), but who knows?
        throw new RuntimeException(
            "Call to Unsafe's objectFieldOffset() throwed "
                + "checked Exception when accessing field "
                + f.getDeclaringClass().getName()
                + "#"
                + f.getName(),
            cause);
      }
    } else {
      // TODO: No alignments based on field type/ subclass fields alignments?
      return sizeSoFar + fsize;
    }
  }

  /** Returns <code>size</code> in human-readable units (GB, MB, KB or bytes). */
  public static String humanReadableUnits(long bytes) {
    return humanReadableUnits(
        bytes, new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH)));
  }

  /** Returns <code>size</code> in human-readable units (GB, MB, KB or bytes). */
  public static String humanReadableUnits(long bytes, DecimalFormat df) {
    if (bytes / ONE_GB > 0) {
      return df.format((float) bytes / ONE_GB) + " GB";
    } else if (bytes / ONE_MB > 0) {
      return df.format((float) bytes / ONE_MB) + " MB";
    } else if (bytes / ONE_KB > 0) {
      return df.format((float) bytes / ONE_KB) + " KB";
    } else {
      return bytes + " bytes";
    }
  }

  /**
   * Return a human-readable size of a given object.
   *
   * @see #sizeOf(Object)
   * @see #humanReadableUnits(long)
   */
  public static String humanSizeOf(Object object) {
    return humanReadableUnits(sizeOf(object));
  }

  /**
   * Return a human-readable size of a given object.
   *
   * @see #sizeOf(Object)
   * @see #humanReadableUnits(long)
   */
  public static String humanSizeOf(Object object, Predicate<Field> objectFilter) {
    return humanReadableUnits(sizeOf(object, objectFilter));
  }
}
