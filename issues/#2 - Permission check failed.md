# Permission check failed

##  现象描述

**Android 10** 手机连接`GATT`后，读写`characteristic`结果返回`true`,但是确没有`callback`返回，`logcat`打印如下:

```java
02-20 04:26:15.599  3815  3834 W BtGatt.GattService: writeCharacteristic() - permission check failed!
```

## 源码分析
### BluetoothGatt#writeCharacteristic

> android.bluetooth.BluetoothGatt.java

```java
/**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * <p>Once the write operation has been completed, the
     * {@link BluetoothGattCallback#onCharacteristicWrite} callback is invoked,
     * reporting the result of the operation.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            return false;
        }

        if (VDBG) Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        if (mService == null || mClientIf == 0 || characteristic.getValue() == null) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) return false;
            mDeviceBusy = true;
        }

        try {
            mService.writeCharacteristic(mClientIf, device.getAddress(),
                    characteristic.getInstanceId(), characteristic.getWriteType(),
                    AUTHENTICATION_NONE, characteristic.getValue());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            mDeviceBusy = false;
            return false;
        }

        return true;
    }
```

### GattService#writeCharacteristic

> /android/platform/packages/apps/Bluetooth/src/com/android/bluetooth/gatt/GattService.java

```java
void writeCharacteristic(int clientIf, String address, int handle, int writeType, int authReq,
            byte[] value) {
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");

        if (VDBG) {
            Log.d(TAG, "writeCharacteristic() - address=" + address);
        }

        if (mReliableQueue.contains(address)) {
            writeType = 3; // Prepared write
        }

        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (connId == null) {
            Log.e(TAG, "writeCharacteristic() - No connection for " + address + "...");
            return;
        }

        if (!permissionCheck(connId, handle)) {
            Log.w(TAG, "writeCharacteristic() - permission check failed!");
            return;
        }

        gattClientWriteCharacteristicNative(connId, handle, writeType, authReq, value);
    }

```

从上面的代码可以看到，`writeCharacteristic() - permission check failed!` 是在`permissionCheck(connId, handle)`返回失败的情况下才打印的。

### permissionCheck

> /android/platform/packages/apps/Bluetooth/src/com/android/bluetooth/gatt/GattService.java

```java
private boolean permissionCheck(int connId, int handle) {
        Set<Integer> restrictedHandles = mRestrictedHandles.get(connId);
        if (restrictedHandles == null || !restrictedHandles.contains(handle)) {
            return true;
        }

        return (checkCallingOrSelfPermission(BLUETOOTH_PRIVILEGED)
                == PERMISSION_GRANTED);
    }

```
从上面的代码可以看出检查流程如下：

* 首先检查 `restrictedHandles` 是否包含当前操作的 handle，由于handle对应的UUID在`restrictedHandles` 中，所以开始检查`BLUETOOTH_PRIVILEGED`权限

* 但是只有系统应用才可以申请获取`BLUETOOTH_PRIVILEGED`权限，所以最后依然返回`false`.

  

第三方应用是无法获取`BLUETOOTH_PRIVILEGED`权限的，所以问题应该是出在`restrictedHandles`上。

### restrictedHandles

```java
/**
* Set of restricted (which require a BLUETOOTH_PRIVILEGED permission) handles per connectionId.
*/
private final Map<Integer, Set<Integer>> mRestrictedHandles = new HashMap<>();

void onGetGattDb(int connId, ArrayList<GattDbElement> db) throws RemoteException {
        String address = mClientMap.addressByConnId(connId);

        if (DBG) {
            Log.d(TAG, "onGetGattDb() - address=" + address);
        }

        ClientMap.App app = mClientMap.getByConnId(connId);
        if (app == null || app.callback == null) {
            Log.e(TAG, "app or callback is null");
            return;
        }

        List<BluetoothGattService> dbOut = new ArrayList<BluetoothGattService>();
        Set<Integer> restrictedIds = new HashSet<>();

        BluetoothGattService currSrvc = null;
        BluetoothGattCharacteristic currChar = null;
        boolean isRestrictedSrvc = false;
        boolean isHidSrvc = false;
        boolean isRestrictedChar = false;

        for (GattDbElement el : db) {
            switch (el.type) {
                case GattDbElement.TYPE_PRIMARY_SERVICE:
                case GattDbElement.TYPE_SECONDARY_SERVICE:
                    if (DBG) {
                        Log.d(TAG, "got service with UUID=" + el.uuid + " id: " + el.id);
                    }

                    currSrvc = new BluetoothGattService(el.uuid, el.id, el.type);
                    dbOut.add(currSrvc);
                    isRestrictedSrvc =
                            isFidoSrvcUuid(el.uuid) || isAndroidTvRemoteSrvcUuid(el.uuid);
                    isHidSrvc = isHidSrvcUuid(el.uuid);
                    if (isRestrictedSrvc) {
                        restrictedIds.add(el.id);
                    }
                    break;

                case GattDbElement.TYPE_CHARACTERISTIC:
                    if (DBG) {
                        Log.d(TAG, "got characteristic with UUID=" + el.uuid + " id: " + el.id);
                    }

                    currChar = new BluetoothGattCharacteristic(el.uuid, el.id, el.properties, 0);
                    currSrvc.addCharacteristic(currChar);
                    isRestrictedChar = isRestrictedSrvc || (isHidSrvc && isHidCharUuid(el.uuid));
                    if (isRestrictedChar) {
                        restrictedIds.add(el.id);
                    }
                    break;

                case GattDbElement.TYPE_DESCRIPTOR:
                    if (DBG) {
                        Log.d(TAG, "got descriptor with UUID=" + el.uuid + " id: " + el.id);
                    }

                    currChar.addDescriptor(new BluetoothGattDescriptor(el.uuid, el.id, 0));
                    if (isRestrictedChar) {
                        restrictedIds.add(el.id);
                    }
                    break;

                case GattDbElement.TYPE_INCLUDED_SERVICE:
                    if (DBG) {
                        Log.d(TAG, "got included service with UUID=" + el.uuid + " id: " + el.id
                                + " startHandle: " + el.startHandle);
                    }

                    currSrvc.addIncludedService(
                            new BluetoothGattService(el.uuid, el.startHandle, el.type));
                    break;

                default:
                    Log.e(TAG, "got unknown element with type=" + el.type + " and UUID=" + el.uuid
                            + " id: " + el.id);
            }
        }

        if (!restrictedIds.isEmpty()) {
            mRestrictedHandles.put(connId, restrictedIds);
        }
        // Search is complete when there was error, or nothing more to process
        app.callback.onSearchComplete(address, dbOut, 0 /* status */);
    }
```

从上面的代码看到`restrictedHandles`里面包含了需要被过滤的UUID，从前面的[permissionCheck](#permissionCheck)已经知道，这些被限制的UUID只有系统应用才可以访问。



> 我们测试设备的服务里面确实没有需要被限制的UUID为什么也会被过滤？

继续跟踪定位发现，`restrictedHandles`只有`put`操作，没有`remove`或者`clear`操作。于是怀疑是`restrictedHandles`缓存导致的，模拟场景如下：

* 首先先连接一个HID设备，查询到的服务里面包含被限制的Service（这里以HID为例），连接成功后，connId=0x09.

* 断开HID设备，connId=0x09被释放。

* 连接一个新的LE设备，服务里面没有需要被限制的Service.连接成功后，connId也是0x09。

* 从前面的`onGetGattDb`可以看出，虽然新的设备没有需要被限制的Service，但是由于`restrictedHandles`没有被清空，两次的connId也是一样的，导致后面判断的时候依然会被过滤。

  ```java
  if (!restrictedIds.isEmpty()) {
     mRestrictedHandles.put(connId, restrictedIds);
  }
  ```

  

> 为什么其他Android设备正常，只有 Android 10有问题?

前面我们分析的代码就是Android 10 的。`mRestrictedHandles`也是在Android 10才加上的。

Android 10 以前版本的 `permissionCheck`是直接检查UUID，不会受前一次连接的缓存影响。

```java
boolean permissionCheck(int connId, int handle) {
        List<BluetoothGattService> db = mGattClientDatabases.get(connId);
        if (db == null) {
            return true;
        }

        for (BluetoothGattService service : db) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if (handle == characteristic.getInstanceId()) {
                    return !((isRestrictedCharUuid(characteristic.getUuid())
                            || isRestrictedSrvcUuid(service.getUuid()))
                            && (0 != checkCallingOrSelfPermission(BLUETOOTH_PRIVILEGED)));
                }

                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    if (handle == descriptor.getInstanceId()) {
                        return !((isRestrictedCharUuid(characteristic.getUuid())
                                || isRestrictedSrvcUuid(service.getUuid())) && (0
                                != checkCallingOrSelfPermission(BLUETOOTH_PRIVILEGED)));
                    }
                }
            }
        }

        return true;
    }
```

## 结论
从上面的分析可以知道，这个问题是Android 10 引入的 BUG，Android 10 以前的系统没有这个问题。第三方APP连接LE设备后，如果前一次连接了一个HID设备，且当前连接的connId和上一次连接的connId相同，就会触发这个BUG。

## 解决方案

* 重新开关蓝牙，这样BluetoothManagerService就会重新bind，缓存会被清除。

* 确保App 或者第三方应用不会去连接HID设备，减小触发BUG的机率。

上面两个方法都不能从根本上解决问题，最终我们还是要等到`Google`更新patch来修复这个BUG。