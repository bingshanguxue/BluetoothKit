# GATT Register Failed for client, MAX Interface reached!

## 日志

```java
2020-07-29 11:20:50.815 8139-8139/? D/BluetoothAdapter: STATE_ON
2020-07-29 11:20:50.815 8139-8139/? D/BluetoothLeScanner: Start Scan with callback
2020-07-29 11:20:50.816 2203-3553/? D/BtGatt.ContextMap: add() - appUid: 10334, appName: com.realsil.ota
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

### BluetoothLeScanner



```java
/**
     * Start Bluetooth LE scan. The scan results will be delivered through {@code callback}.
     * For unfiltered scans, scanning is stopped on screen off to save power. Scanning is
     * resumed when screen is turned on again. To avoid this, do filetered scanning by
     * using proper {@link ScanFilter}.
     * <p>
     * An app must hold
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission
     * in order to get results.
     *
     * @param filters {@link ScanFilter}s for finding exact BLE devices.
     * @param settings Settings for the scan.
     * @param callback Callback used to deliver scan results.
     * @throws IllegalArgumentException If {@code settings} or {@code callback} is null.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public void startScan(List<ScanFilter> filters, ScanSettings settings,
            final ScanCallback callback) {
        startScan(filters, settings, null, callback, /*callbackIntent=*/ null, null);
    }

private int startScan(List<ScanFilter> filters, ScanSettings settings,
            final WorkSource workSource, final ScanCallback callback,
            final PendingIntent callbackIntent,
            List<List<ResultStorageDescriptor>> resultStorages) {
        BluetoothLeUtils.checkAdapterStateOn(mBluetoothAdapter);
        if (callback == null && callbackIntent == null) {
            throw new IllegalArgumentException("callback is null");
        }
        if (settings == null) {
            throw new IllegalArgumentException("settings is null");
        }
        synchronized (mLeScanClients) {
            if (callback != null && mLeScanClients.containsKey(callback)) {
                return postCallbackErrorOrReturn(callback,
                            ScanCallback.SCAN_FAILED_ALREADY_STARTED);
            }
            IBluetoothGatt gatt;
            try {
                gatt = mBluetoothManager.getBluetoothGatt();
            } catch (RemoteException e) {
                gatt = null;
            }
            if (gatt == null) {
                return postCallbackErrorOrReturn(callback, ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            }
            if (!isSettingsConfigAllowedForScan(settings)) {
                return postCallbackErrorOrReturn(callback,
                        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED);
            }
            if (!isHardwareResourcesAvailableForScan(settings)) {
                return postCallbackErrorOrReturn(callback,
                        ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES);
            }
            if (!isSettingsAndFilterComboAllowed(settings, filters)) {
                return postCallbackErrorOrReturn(callback,
                        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED);
            }
            if (callback != null) {
                BleScanCallbackWrapper wrapper = new BleScanCallbackWrapper(gatt, filters,
                        settings, workSource, callback, resultStorages);
                wrapper.startRegistration();
            } else {
                try {
                    gatt.startScanForIntent(callbackIntent, settings, filters,
                            ActivityThread.currentOpPackageName());
                } catch (RemoteException e) {
                    return ScanCallback.SCAN_FAILED_INTERNAL_ERROR;
                }
            }
        }
        return ScanCallback.NO_ERROR;
    }
```

### enable

œandroid/platform/system/bt/btif/src/bluetooth.cc

```c
static int enable() {
  if (!interface_ready()) return BT_STATUS_NOT_READY;

  stack_manager_get_interface()->start_up_stack_async();
  return BT_STATUS_SUCCESS;
}
```

### start_up_stack_async

> android/platform/system/bt/btif/src/stack_manager.cc

```c
static void start_up_stack_async() {
  management_thread.DoInThread(FROM_HERE,
                               base::Bind(event_start_up_stack, nullptr));
}
```

### event_start_up_stack

> android/platform/system/bt/btif/src/stack_manager.cc

```c
// Synchronous function to start up the stack
static void event_start_up_stack(UNUSED_ATTR void* context) {
  if (stack_is_running) {
    LOG_INFO("%s stack already brought up", __func__);
    return;
  }

  ensure_stack_is_initialized();

  LOG_INFO("%s is bringing up the stack", __func__);
  future_t* local_hack_future = future_new();
  hack_future = local_hack_future;

  // Include this for now to put btif config into a shutdown-able state
  bte_main_enable();

  if (future_await(local_hack_future) != FUTURE_SUCCESS) {
    LOG_ERROR("%s failed to start up the stack", __func__);
    stack_is_running = true;  // So stack shutdown actually happens
    event_shut_down_stack(nullptr);
    return;
  }

  stack_is_running = true;
  LOG_INFO("%s finished", __func__);
  do_in_jni_thread(FROM_HERE, base::Bind(event_signal_stack_up, nullptr));
}
```

### btif_enable_bluetooth

> android/platform/external/bluetooth/bluedroid/btif/src/btif_core.c

```c
/*******************************************************************************
**
** Function         btif_enable_bluetooth
**
** Description      Performs chip power on and kickstarts OS scheduler
**
** Returns          bt_status_t
**
*******************************************************************************/

bt_status_t btif_enable_bluetooth(void)
{
    BTIF_TRACE_DEBUG("BTIF ENABLE BLUETOOTH");

    if (btif_core_state != BTIF_CORE_STATE_DISABLED)
    {
        ALOGD("not disabled\n");
        return BT_STATUS_DONE;
    }

    btif_core_state = BTIF_CORE_STATE_ENABLING;

    /* Create the GKI tasks and run them */
    bte_main_enable();

    return BT_STATUS_SUCCESS;
}
```

### bte_main_enable

> android/platform/system/bt/main/bte_main.cc

```c
/******************************************************************************
 *
 * Function         bte_main_enable
 *
 * Description      BTE MAIN API - Creates all the BTE tasks. Should be called
 *                  part of the Bluetooth stack enable sequence
 *
 * Returns          None
 *
 *****************************************************************************/
void bte_main_enable() {
  APPL_TRACE_DEBUG("%s", __func__);

  if (bluetooth::shim::is_gd_shim_enabled()) {
    LOG_INFO("%s Gd shim module enabled", __func__);
    module_shut_down(get_module(GD_IDLE_MODULE));
    module_start_up(get_module(GD_SHIM_MODULE));
    module_start_up(get_module(BTIF_CONFIG_MODULE));
  } else {
    module_start_up(get_module(BTIF_CONFIG_MODULE));
    module_start_up(get_module(BTSNOOP_MODULE));
    module_start_up(get_module(HCI_MODULE));
  }

  BTU_StartUp();
}
```

### BTU_StartUp

> android/platform/system/bt/stack/btu/btu_init.cc

```c
/*****************************************************************************
 *
 * Function         BTU_StartUp
 *
 * Description      Initializes the BTU control block.
 *
 *                  NOTE: Must be called before creating any tasks
 *                      (RPC, BTU, HCIT, APPL, etc.)
 *
 * Returns          void
 *
 *****************************************************************************/
void BTU_StartUp() {
  btu_trace_level = HCI_INITIAL_TRACE_LEVEL;
  bt_startup_thread.StartUp();
  if (!bt_startup_thread.EnableRealTimeScheduling()) {
    LOG(ERROR) << __func__ << ": Unable to set real time scheduling policy for "
               << bt_startup_thread;
    BTU_ShutDown();
    return;
  }
  if (!bt_startup_thread.DoInThread(FROM_HERE,
                                    base::Bind(btu_task_start_up, nullptr))) {
    LOG(ERROR) << __func__ << ": Unable to continue start-up on "
               << bt_startup_thread;
    BTU_ShutDown();
    return;
  }
}
```

### btu_task_start_up

> android/platform/system/bt/stack/btu/btu_task.cc

```c
void btu_task_start_up(UNUSED_ATTR void* context) {
  LOG(INFO) << "Bluetooth chip preload is complete";

  /* Initialize the mandatory core stack control blocks
     (BTU, BTM, L2CAP, and SDP)
   */
  btu_init_core();

  /* Initialize any optional stack components */
  BTE_InitStack();

  bta_sys_init();

  /* Initialise platform trace levels at this point as BTE_InitStack() and
   * bta_sys_init()
   * reset the control blocks and preset the trace level with
   * XXX_INITIAL_TRACE_LEVEL
   */
  module_init(get_module(BTE_LOGMSG_MODULE));

  main_thread.StartUp();
  if (!main_thread.IsRunning()) {
    LOG(FATAL) << __func__ << ": unable to start btu message loop thread.";
  }
  if (!main_thread.EnableRealTimeScheduling()) {
    LOG(FATAL) << __func__ << ": unable to enable real time scheduling";
  }
  if (do_in_jni_thread(FROM_HERE, base::Bind(btif_init_ok, 0, nullptr)) !=
      BT_STATUS_SUCCESS) {
    LOG(FATAL) << __func__ << ": unable to continue starting Bluetooth";
  }
}
```

### BTE_InitStack

> android/platform/system/bt/main/bte_init.cc



```c
/*****************************************************************************
 *
 * Function         BTE_InitStack
 *
 * Description      Initialize control block memory for each component.
 *
 *                  Note: The core stack components must be called
 *                      before creating the BTU Task.  The rest of the
 *                      components can be initialized at a later time if desired
 *                      as long as the component's init function is called
 *                      before accessing any of its functions.
 *
 * Returns          void
 *
 *****************************************************************************/
void BTE_InitStack(void) {
  /* Initialize the optional stack components */
  RFCOMM_Init();

/**************************
 * BNEP and its profiles **
 **************************/
#if (BNEP_INCLUDED == TRUE)
  BNEP_Init();

#if (PAN_INCLUDED == TRUE)
  PAN_Init();
#endif /* PAN */
#endif /* BNEP Included */

/**************************
 * AVDT and its profiles **
 **************************/
  A2DP_Init();

  AVRC_Init();

  /***********
   * Others **
   ***********/
  GAP_Init();

#if (HID_HOST_INCLUDED == TRUE)
  HID_HostInit();
#endif
}
```

### GAP_Init

> android/platform/system/bt/stack/gatt/gatt_api.cc



```c
/*******************************************************************************
**
** Function         GAP_Init
**
** Description      Initializes the control blocks used by GAP.
**
**                  This routine should not be called except once per
**                      stack invocation.
**
** Returns          Nothing
**
*******************************************************************************/
void GAP_Init(void)
{
    memset (&gap_cb, 0, sizeof (tGAP_CB));

    /*** Initialize the callbacks for BTM; Needs to be one per GAP_MAX_BLOCKS ***/
    gap_cb.btm_cback[0] = gap_btm_cback0;
#if GAP_MAX_BLOCKS > 1
    gap_cb.btm_cback[1] = gap_btm_cback1;
#endif
#if GAP_MAX_BLOCKS > 2
    gap_cb.btm_cback[2] = gap_btm_cback2;
#endif

#if defined(GAP_INITIAL_TRACE_LEVEL)
    gap_cb.trace_level = GAP_INITIAL_TRACE_LEVEL;
#else
    gap_cb.trace_level = BT_TRACE_LEVEL_NONE;    /* No traces */
#endif

    /* Initialize the connection control block if included in build */
#if GAP_CONN_INCLUDED == TRUE
    gap_conn_init();
#endif  /* GAP_CONN_INCLUDED */

#if BLE_INCLUDED == TRUE
    gap_attr_db_init();
#endif
}
```



### gap_attr_db_init

> android/platform/system/bt/stack/gap/gap_ble.cc

```c
/*******************************************************************************
 *
 * Function         btm_ble_att_db_init
 *
 * Description      GAP ATT database initalization.
 *
 * Returns          void.
 *
 ******************************************************************************/
void gap_attr_db_init(void) {
  uint16_t service_handle;

  /* Fill our internal UUID with a fixed pattern 0x82 */
  std::array<uint8_t, Uuid::kNumBytes128> tmp;
  tmp.fill(0x82);
  Uuid app_uuid = Uuid::From128BitBE(tmp);
  gatt_attr.fill({});

  gatt_if = GATT_Register(app_uuid, &gap_cback);

  GATT_StartIf(gatt_if);

  Uuid svc_uuid = Uuid::From16Bit(UUID_SERVCLASS_GAP_SERVER);
  Uuid name_uuid = Uuid::From16Bit(GATT_UUID_GAP_DEVICE_NAME);
  Uuid icon_uuid = Uuid::From16Bit(GATT_UUID_GAP_ICON);
  Uuid addr_res_uuid = Uuid::From16Bit(GATT_UUID_GAP_CENTRAL_ADDR_RESOL);

  btgatt_db_element_t service[] = {
    {
        .uuid = svc_uuid,
        .type = BTGATT_DB_PRIMARY_SERVICE,
    },
    {.uuid = name_uuid,
     .type = BTGATT_DB_CHARACTERISTIC,
     .properties = GATT_CHAR_PROP_BIT_READ,
     .permissions = GATT_PERM_READ},
    {.uuid = icon_uuid,
     .type = BTGATT_DB_CHARACTERISTIC,
     .properties = GATT_CHAR_PROP_BIT_READ,
     .permissions = GATT_PERM_READ},
    {.uuid = addr_res_uuid,
     .type = BTGATT_DB_CHARACTERISTIC,
     .properties = GATT_CHAR_PROP_BIT_READ,
     .permissions = GATT_PERM_READ}
#if (BTM_PERIPHERAL_ENABLED == TRUE) /* Only needed for peripheral testing */
    ,
    {.uuid = Uuid::From16Bit(GATT_UUID_GAP_PREF_CONN_PARAM),
     .type = BTGATT_DB_CHARACTERISTIC,
     .properties = GATT_CHAR_PROP_BIT_READ,
     .permissions = GATT_PERM_READ}
#endif
  };

  /* Add a GAP service */
  GATTS_AddService(gatt_if, service,
                   sizeof(service) / sizeof(btgatt_db_element_t));
  service_handle = service[0].attribute_handle;

  DVLOG(1) << __func__ << ": service_handle = " << +service_handle;

  gatt_attr[0].uuid = GATT_UUID_GAP_DEVICE_NAME;
  gatt_attr[0].handle = service[1].attribute_handle;

  gatt_attr[1].uuid = GATT_UUID_GAP_ICON;
  gatt_attr[1].handle = service[2].attribute_handle;

  gatt_attr[2].uuid = GATT_UUID_GAP_CENTRAL_ADDR_RESOL;
  gatt_attr[2].handle = service[3].attribute_handle;
  gatt_attr[2].attr_value.addr_resolution = 0;

#if (BTM_PERIPHERAL_ENABLED == TRUE) /*  Only needed for peripheral testing */

  gatt_attr[3].uuid = GATT_UUID_GAP_PREF_CONN_PARAM;
  gatt_attr[3].attr_value.conn_param.int_max = GAP_PREFER_CONN_INT_MAX; /* 6 */
  gatt_attr[3].attr_value.conn_param.int_min = GAP_PREFER_CONN_INT_MIN; /* 0 */
  gatt_attr[3].attr_value.conn_param.latency = GAP_PREFER_CONN_LATENCY; /* 0 */
  gatt_attr[3].attr_value.conn_param.sp_tout =
      GAP_PREFER_CONN_SP_TOUT; /* 2000 */
  gatt_attr[3].handle = service[4].attribute_handle;
#endif
}
```



### GATT_Register

> android/platform/system/bt/stack/gatt/gatt_api.cc



```c
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

### 



从现象看是30秒内多次搜索出现的问题。