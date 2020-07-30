# Permission check failed

##  错误日志
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

### permissionCheck

#### API 29

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
添加 restrictedHandles 如下：
```java
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
#### API 28
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

private boolean isRestrictedCharUuid(final UUID charUuid) {
        return isHidUuid(charUuid);
    }

private boolean isHidUuid(final UUID uuid) {
        for (UUID hidUuid : HID_UUIDS) {
            if (hidUuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }
private boolean isRestrictedSrvcUuid(final UUID srvcUuid) {
        return isFidoUUID(srvcUuid);
    }

private boolean isFidoUUID(final UUID uuid) {
        for (UUID fidoUuid : FIDO_UUIDS) {
            if (fidoUuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

private static final UUID[] FIDO_UUIDS = {
            UUID.fromString("0000FFFD-0000-1000-8000-00805F9B34FB") // U2F
    };

private static final UUID[] HID_UUIDS = {
            UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB"),
            UUID.fromString("00002A4B-0000-1000-8000-00805F9B34FB"),
            UUID.fromString("00002A4C-0000-1000-8000-00805F9B34FB"),
            UUID.fromString("00002A4D-0000-1000-8000-00805F9B34FB")
    };
```

### onGetGattDb
```c
void btgattc_get_gatt_db_cb(int conn_id, const btgatt_db_element_t* db,
                            int count) {
  CallbackEnv sCallbackEnv(__func__);
  if (!sCallbackEnv.valid()) return;

  jclass arrayListclazz = sCallbackEnv->FindClass("java/util/ArrayList");
  ScopedLocalRef<jobject> array(
      sCallbackEnv.get(),
      sCallbackEnv->NewObject(
          arrayListclazz,
          sCallbackEnv->GetMethodID(arrayListclazz, "<init>", "()V")));

  jobject arrayPtr = array.get();
  fillGattDbElementArray(sCallbackEnv.get(), &arrayPtr, db, count);

  sCallbackEnv->CallVoidMethod(mCallbacksObj, method_onGetGattDb, conn_id,
                               array.get());
}

void fillGattDbElementArray(JNIEnv* env, jobject* array,
                            const btgatt_db_element_t* db, int count) {
  // Because JNI uses a different class loader in the callback context, we
  // cannot simply get the class.
  // As a workaround, we have to make sure we obtain an object of the class
  // first, as this will cause
  // class loader to load it.
  ScopedLocalRef<jobject> objectForClass(
      env, env->CallObjectMethod(mCallbacksObj, method_getSampleGattDbElement));
  ScopedLocalRef<jclass> gattDbElementClazz(
      env, env->GetObjectClass(objectForClass.get()));

  jmethodID gattDbElementConstructor =
      env->GetMethodID(gattDbElementClazz.get(), "<init>", "()V");

  ScopedLocalRef<jclass> arrayListclazz(env,
                                        env->FindClass("java/util/ArrayList"));
  jmethodID arrayAdd =
      env->GetMethodID(arrayListclazz.get(), "add", "(Ljava/lang/Object;)Z");

  ScopedLocalRef<jclass> uuidClazz(env, env->FindClass("java/util/UUID"));
  jmethodID uuidConstructor =
      env->GetMethodID(uuidClazz.get(), "<init>", "(JJ)V");

  for (int i = 0; i < count; i++) {
    const btgatt_db_element_t& curr = db[i];

    ScopedLocalRef<jobject> element(
        env,
        env->NewObject(gattDbElementClazz.get(), gattDbElementConstructor));

    jfieldID fid = env->GetFieldID(gattDbElementClazz.get(), "id", "I");
    env->SetIntField(element.get(), fid, curr.id);

    fid = env->GetFieldID(gattDbElementClazz.get(), "attributeHandle", "I");
    env->SetIntField(element.get(), fid, curr.attribute_handle);

    ScopedLocalRef<jobject> uuid(
        env, env->NewObject(uuidClazz.get(), uuidConstructor,
                            uuid_msb(curr.uuid), uuid_lsb(curr.uuid)));
    fid = env->GetFieldID(gattDbElementClazz.get(), "uuid", "Ljava/util/UUID;");
    env->SetObjectField(element.get(), fid, uuid.get());

    fid = env->GetFieldID(gattDbElementClazz.get(), "type", "I");
    env->SetIntField(element.get(), fid, curr.type);

    fid = env->GetFieldID(gattDbElementClazz.get(), "attributeHandle", "I");
    env->SetIntField(element.get(), fid, curr.attribute_handle);

    fid = env->GetFieldID(gattDbElementClazz.get(), "startHandle", "I");
    env->SetIntField(element.get(), fid, curr.start_handle);

    fid = env->GetFieldID(gattDbElementClazz.get(), "endHandle", "I");
    env->SetIntField(element.get(), fid, curr.end_handle);

    fid = env->GetFieldID(gattDbElementClazz.get(), "properties", "I");
    env->SetIntField(element.get(), fid, curr.properties);

    env->CallBooleanMethod(*array, arrayAdd, element.get());
  }
}

```
## 解决方案
Android 10  临时解决办法是注释掉 `gatt.close()` 方法的调用。
这个方法只在部分 `Android 10` 系统上有效，并不是最终的解决方案。
从蓝牙源码上看，出现这个permission failed,是因为缺少BLUETOOTH_PRIVILEGED权限，这个权限只有系统应用才有。正常情况下是不会执行到这里的，一般检查完handle后就会返回