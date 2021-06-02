package com.realsil.apps.bluetooth5.support.adv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseData;
import android.os.ParcelUuid;
import android.util.SparseArray;

import com.realsil.sdk.core.bluetooth.utils.BluetoothUuid;
import com.realsil.sdk.core.logger.ZLogger;
import com.realsil.sdk.core.utility.DataConverter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Utils {
    // Each fields need one byte for field length and another byte for field type.
    private static final int OVERHEAD_BYTES_PER_FIELD = 2;
    // Flags field will be set by system.
    private static final int FLAGS_FIELD_BYTES = 3;
    private static final int MANUFACTURER_SPECIFIC_DATA_LENGTH = 2;


    public static int totalBytes(BluetoothAdapter mBluetoothAdapter, AdvertiseData data, boolean isFlagsIncluded) {
        if (data == null) return 0;
        // Flags field is omitted if the advertising is not connectable.
        int size = (isFlagsIncluded) ? FLAGS_FIELD_BYTES : 0;
        if (data.getServiceUuids() != null) {
            int num16BitUuids = 0;
            int num32BitUuids = 0;
            int num128BitUuids = 0;
            for (ParcelUuid uuid : data.getServiceUuids()) {
                if (BluetoothUuid.is16BitUuid(uuid)) {
                    ++num16BitUuids;
                } else if (BluetoothUuid.is32BitUuid(uuid)) {
                    ++num32BitUuids;
                } else {
                    ++num128BitUuids;
                }
            }
            ZLogger.v(String.format(Locale.US, "num16BitUuids=%d, num32BitUuids=%d, num128BitUuids=%d",
                    num16BitUuids, num32BitUuids, num128BitUuids));

            // 16 bit service uuids are grouped into one field when doing advertising.
            if (num16BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num16BitUuids * BluetoothUuid.UUID_BYTES_16_BIT;
            }
            // 32 bit service uuids are grouped into one field when doing advertising.
            if (num32BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num32BitUuids * BluetoothUuid.UUID_BYTES_32_BIT;
            }
            // 128 bit service uuids are grouped into one field when doing advertising.
            if (num128BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD
                        + num128BitUuids * BluetoothUuid.UUID_BYTES_128_BIT;
            }
        }
        ZLogger.v("getServiceUuids=" + size);
        for (ParcelUuid uuid : data.getServiceData().keySet()) {
            ZLogger.v(String.format("uuid:%s: %s", DataConverter.bytes2Hex(uuidToBytes(uuid)), DataConverter.bytes2Hex(data.getServiceData().get(uuid))));
            int uuidLen = uuidToBytes(uuid).length;
            size += OVERHEAD_BYTES_PER_FIELD + uuidLen
                    + byteLength(data.getServiceData().get(uuid));
            ZLogger.v("getServiceData=" + size);
        }

        for (int i = 0; i < data.getManufacturerSpecificData().size(); ++i) {
            size += OVERHEAD_BYTES_PER_FIELD + MANUFACTURER_SPECIFIC_DATA_LENGTH
                    + byteLength(data.getManufacturerSpecificData().valueAt(i));
            ZLogger.v(String.format(Locale.US, "ManufacturerSpecificData:%d: %s", i, DataConverter.bytes2Hex(data.getManufacturerSpecificData().valueAt(i))));
            ZLogger.v("getManufacturerSpecificData=" + size);
        }
        if (data.getIncludeTxPowerLevel()) {
            size += OVERHEAD_BYTES_PER_FIELD + 1; // tx power level value is one byte.
            ZLogger.v("getIncludeTxPowerLevel=" + size);

        }
        if (data.getIncludeDeviceName() && mBluetoothAdapter.getName() != null) {
            size += OVERHEAD_BYTES_PER_FIELD + mBluetoothAdapter.getName().length();
            ZLogger.v(String.format(Locale.US, "getIncludeDeviceName=(%d)=%d+(%s)", size, OVERHEAD_BYTES_PER_FIELD, mBluetoothAdapter.getName()));
        }
        return size;
    }

    public static int getMaxServiceDataLength(BluetoothAdapter mBluetoothAdapter, boolean isFlagsIncluded, ParcelUuid uuid, boolean IncludeTxPowerLevel, boolean IncludeDeviceName) {
        // Flags field is omitted if the advertising is not connectable.
        int size = (isFlagsIncluded) ? FLAGS_FIELD_BYTES : 0;

        if (BluetoothUuid.is16BitUuid(uuid)) {
            // 16 bit service uuids are grouped into one field when doing advertising.
            size += OVERHEAD_BYTES_PER_FIELD +  BluetoothUuid.UUID_BYTES_16_BIT;
        } else if (BluetoothUuid.is32BitUuid(uuid)) {
            // 32 bit service uuids are grouped into one field when doing advertising.
            size += OVERHEAD_BYTES_PER_FIELD +  BluetoothUuid.UUID_BYTES_32_BIT;
        } else {
            // 128 bit service uuids are grouped into one field when doing advertising.

            size += OVERHEAD_BYTES_PER_FIELD
                    + BluetoothUuid.UUID_BYTES_128_BIT;
        }
//        ZLogger.v(String.format(Locale.US, "num16BitUuids=%d, num32BitUuids=%d, num128BitUuids=%d",
//                num16BitUuids, num32BitUuids, num128BitUuids));


//        ZLogger.v("getServiceUuids=" + size);
//        ZLogger.v(String.format("uuid:%s: %s", DataConverter.bytes2Hex(uuidToBytes(uuid)), DataConverter.bytes2Hex(data.getServiceData().get(uuid))));
        int uuidLen = uuidToBytes(uuid).length;
        size += OVERHEAD_BYTES_PER_FIELD + uuidLen;
//        ZLogger.v("getServiceData=" + size);

//        for (int i = 0; i < data.getManufacturerSpecificData().size(); ++i) {
//            size += OVERHEAD_BYTES_PER_FIELD + MANUFACTURER_SPECIFIC_DATA_LENGTH
//                    + byteLength(data.getManufacturerSpecificData().valueAt(i));
//            ZLogger.v(String.format(Locale.US, "ManufacturerSpecificData:%d: %s", i, DataConverter.bytes2Hex(data.getManufacturerSpecificData().valueAt(i))));
//            ZLogger.v("getManufacturerSpecificData=" + size);
//        }
        if (IncludeTxPowerLevel) {
            size += OVERHEAD_BYTES_PER_FIELD + 1; // tx power level value is one byte.
//            ZLogger.v("getIncludeTxPowerLevel=" + size);

        }
        if (IncludeDeviceName && mBluetoothAdapter.getName() != null) {
            size += OVERHEAD_BYTES_PER_FIELD + mBluetoothAdapter.getName().length();
//            ZLogger.v(String.format(Locale.US, "getIncludeDeviceName=(%d)=%d+(%s)", size, OVERHEAD_BYTES_PER_FIELD, mBluetoothAdapter.getName()));
        }
        return size;
    }

    private static int byteLength(byte[] array) {
        return array == null ? 0 : array.length;
    }

    /**
     * Parse UUID to bytes. The returned value is shortest representation, a 16-bit, 32-bit or
     * 128-bit UUID, Note returned value is little endian (Bluetooth).
     *
     * @param uuid uuid to parse.
     * @return shortest representation of {@code uuid} as bytes.
     * @throws IllegalArgumentException If the {@code uuid} is null.
     *
     * @hide
     */
    public static byte[] uuidToBytes(ParcelUuid uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }

        if (is16BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_16_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            return uuidBytes;
        }

        if (is32BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_32_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            uuidBytes[2] = (byte) ((uuidVal & 0xFF0000) >> 16);
            uuidBytes[3] = (byte) ((uuidVal & 0xFF000000) >> 24);
            return uuidBytes;
        }

        // Construct a 128 bit UUID.
        long msb = uuid.getUuid().getMostSignificantBits();
        long lsb = uuid.getUuid().getLeastSignificantBits();

        byte[] uuidBytes = new byte[UUID_BYTES_128_BIT];
        ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(8, msb);
        buf.putLong(0, lsb);
        return uuidBytes;
    }

    /** @hide */
    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /**
     * Length of bytes for 16 bit UUID
     *
     * @hide
     */
    public static final int UUID_BYTES_16_BIT = 2;
    /**
     * Length of bytes for 32 bit UUID
     *
     * @hide
     */
    public static final int UUID_BYTES_32_BIT = 4;
    /**
     * Length of bytes for 128 bit UUID
     *
     * @hide
     */
    public static final int UUID_BYTES_128_BIT = 16;

    /**
     * Extract the Service Identifier or the actual uuid from the Parcel Uuid.
     * For example, if 0000110B-0000-1000-8000-00805F9B34FB is the parcel Uuid,
     * this function will return 110B
     *
     * @param parcelUuid
     * @return the service identifier.
     */
    private static int getServiceIdentifierFromParcelUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        long value = (uuid.getMostSignificantBits() & 0xFFFFFFFF00000000L) >>> 32;
        return (int) value;
    }

    /**
     * Check whether the given parcelUuid can be converted to 16 bit bluetooth uuid.
     *
     * @param parcelUuid
     * @return true if the parcelUuid can be converted to 16 bit uuid, false otherwise.
     *
     * @hide
     */
    public static boolean is16BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getUuid().getLeastSignificantBits()) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFF0000FFFFFFFFL) == 0x1000L);
    }

    /**
     * Check whether the given parcelUuid can be converted to 32 bit bluetooth uuid.
     *
     * @param parcelUuid
     * @return true if the parcelUuid can be converted to 32 bit uuid, false otherwise.
     *
     * @hide
     */
    public static boolean is32BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getUuid().getLeastSignificantBits()) {
            return false;
        }
        if (is16BitUuid(parcelUuid)) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFFFFFFL) == 0x1000L);
    }

    /**
     * Returns a string composed from a {@link SparseArray}.
     */
    public static String toString(SparseArray<byte[]> array) {
        if (array == null) {
            return "null";
        }
        if (array.size() == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        for (int i = 0; i < array.size(); ++i) {
            byte[] value = array.valueAt(i);
            int len = 0;
            if (value != null) {
                len = value.length;
            }
            buffer.append(array.keyAt(i)).append("=(").append(len).append(")").append(DataConverter.bytes2HexWithSeparate(value));
//            buffer.append(array.keyAt(i)).append("=").append(Arrays.toString(array.valueAt(i)));
        }
        buffer.append('}');
        return buffer.toString();
    }

    /**
     * Returns a string composed from a {@link Map}.
     */
    public static <T> String toString(Map<T, byte[]> map) {
        if (map == null) {
            return "null";
        }
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        Iterator<Map.Entry<T, byte[]>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T, byte[]> entry = it.next();
            Object key = entry.getKey();
            byte[] value = map.get(key);
            int len = 0;
            if (value != null) {
                len = value.length;
            }
            buffer.append(key).append("=(").append(len).append(")").append(DataConverter.bytes2HexWithSeparate(value));
//            buffer.append(key).append("=").append(Arrays.toString(map.get(key)));
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    private TransmitThread transmitThread;
    private class TransmitThread extends  Thread {
        /**
         * If this thread was constructed using a separate
         * <code>Runnable</code> run object, then that
         * <code>Runnable</code> object's <code>run</code> method is called;
         * otherwise, this method does nothing and returns.
         * <p>
         * Subclasses of <code>Thread</code> should override this method.
         *
         * @see #start()
         * @see #stop()
         * @see #Thread(ThreadGroup, Runnable, String)
         */
        @Override
        public void run() {
            super.run();

        }
    }
}
