# GATT Register Failed for client, MAX Interface reached!

## 日志

```
2020-07-29 11:20:50.814 8139-8139/? D/BluetoothAdapter: STATE_ON
2020-07-29 11:20:50.815 8139-8139/? D/BluetoothAdapter: STATE_ON
2020-07-29 11:20:50.815 8139-8139/? D/BluetoothLeScanner: Start Scan with callback
2020-07-29 11:20:50.816 2203-3553/? D/BtGatt.ContextMap: add() - appUid: 10334, appName: com.realsil.ota
2020-07-29 11:20:50.816 2203-3166/? I/bt_stack: [INFO:gatt_api.cc(953)] GATT_Register fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.816 2203-3166/? E/bt_stack: [ERROR:gatt_api.cc(995)] can't Register GATT client, MAX client reached: 32
2020-07-29 11:20:50.816 2203-3166/? E/bt_btif: GATT Register Failed for client, MAX Interface reached!
2020-07-29 11:20:50.816 2203-3166/? I/bt_stack: [INFO:gatt_api.cc(953)] GATT_Register fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.816 2203-3166/? E/bt_stack: [ERROR:gatt_api.cc(995)] can't Register GATT client, MAX client reached: 32
2020-07-29 11:20:50.816 2203-3166/? E/bt_btif: GATT Register Failed for client, MAX Interface reached!
2020-07-29 11:20:50.816 2203-3166/? I/bt_stack: [INFO:gatt_api.cc(953)] GATT_Register fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.816 2203-3166/? E/bt_stack: [ERROR:gatt_api.cc(995)] can't Register GATT client, MAX client reached: 32
2020-07-29 11:20:50.816 2203-3166/? E/bt_btif: GATT Register Failed for client, MAX Interface reached!
2020-07-29 11:20:50.816 2203-3166/? I/bt_stack: [INFO:gatt_api.cc(953)] GATT_Register fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.816 2203-3166/? E/bt_stack: [ERROR:gatt_api.cc(995)] can't Register GATT client, MAX client reached: 32
2020-07-29 11:20:50.816 2203-3166/? E/bt_btif: GATT Register Failed for client, MAX Interface reached!
2020-07-29 11:20:50.817 2203-3166/? I/bt_stack: [INFO:gatt_api.cc(953)] GATT_Register fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.817 2203-3166/? E/bt_stack: [ERROR:gatt_api.cc(995)] can't Register GATT client, MAX client reached: 32
2020-07-29 11:20:50.817 2203-3166/? E/bt_btif: GATT Register Failed for client, MAX Interface reached!
2020-07-29 11:20:50.817 2203-2382/? D/BtGatt.ContextMap: remove() - uuid: fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.817 2203-2382/? E/BtGatt.ContextMap: remove() - removed: fd364433-59da-4176-8618-9d31596efd14
2020-07-29 11:20:50.817 8139-13397/? D/BluetoothLeScanner: onScannerRegistered() - status=133 scannerId=0 mScannerId=0
2020-07-29 11:20:50.817 8139-13397/? D/BluetoothLeScanner: Registration failed, unregister scannerId = 0
2020-07-29 11:20:50.817 8139-8139/? D/BluetoothLeScanner: Scan failed, reason: app registration failed
```



## 源码分析

```java
/*******************************************************************************
 *
 * Function         GATT_Register
 *
 * Description      This function is called to register an  application
 *                  with GATT
 *
 * Parameter        p_app_uuid128: Application UUID
 *                  p_cb_info: callback functions.
 *
 * Returns          0 for error, otherwise the index of the client registered
 *                  with GATT
 *
 ******************************************************************************/
tGATT_IF GATT_Register(const Uuid& app_uuid128, tGATT_CBACK* p_cb_info) {
  tGATT_REG* p_reg;
  uint8_t i_gatt_if = 0;
  tGATT_IF gatt_if = 0;

  LOG(INFO) << __func__ << " " << app_uuid128;

  for (i_gatt_if = 0, p_reg = gatt_cb.cl_rcb; i_gatt_if < GATT_MAX_APPS;
       i_gatt_if++, p_reg++) {
    if (p_reg->in_use && p_reg->app_uuid128 == app_uuid128) {
      LOG(ERROR) << "application already registered.";
      return 0;
    }
  }

  for (i_gatt_if = 0, p_reg = gatt_cb.cl_rcb; i_gatt_if < GATT_MAX_APPS;
       i_gatt_if++, p_reg++) {
    if (!p_reg->in_use) {
      memset(p_reg, 0, sizeof(tGATT_REG));
      i_gatt_if++; /* one based number */
      p_reg->app_uuid128 = app_uuid128;
      gatt_if = p_reg->gatt_if = (tGATT_IF)i_gatt_if;
      p_reg->app_cb = *p_cb_info;
      p_reg->in_use = true;

      LOG(INFO) << "allocated gatt_if=" << +gatt_if;
      return gatt_if;
    }
  }

  LOG(ERROR) << "can't Register GATT client, MAX client reached: "
             << GATT_MAX_APPS;
  return 0;
}
```

## 结论

从现象看是30秒内多次搜索出现的问题。