# java.io.IOException: Connect refused

[TOC]

##  现象描述

与蓝牙设备建立 SPP 连接，提示连接失败。

```java
java.io.IOException: Connect refused
```

## 源码分析
### BluetoothSocket#connect()

> android/bluetooth/BluetoothSocket.java

```java
/**
     * Attempt to connect to a remote device.
     * <p>This method will block until a connection is made or the connection
     * fails. If this method returns without an exception then this socket
     * is now connected.
     * <p>Creating new connections to
     * remote Bluetooth devices should not be attempted while device discovery
     * is in progress. Device discovery is a heavyweight procedure on the
     * Bluetooth adapter and will significantly slow a device connection.
     * Use {@link BluetoothAdapter#cancelDiscovery()} to cancel an ongoing
     * discovery. Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * {@link BluetoothAdapter#cancelDiscovery()} even if it
     * did not directly request a discovery, just to be sure.
     * <p>{@link #close} can be used to abort this call from another thread.
     *
     * @throws IOException on error, for example connection failure
     */
    public void connect() throws IOException {
        if (mDevice == null) throw new IOException("Connect is called on null device");

        try {
            if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
            IBluetooth bluetoothProxy =
                    BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
            if (bluetoothProxy == null) throw new IOException("Bluetooth is off");
            mPfd = bluetoothProxy.getSocketManager().connectSocket(mDevice, mType,
                    mUuid, mPort, getSecurityFlags());
            synchronized (this) {
                if (DBG) Log.d(TAG, "connect(), SocketState: " + mSocketState + ", mPfd: " + mPfd);
                if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
                if (mPfd == null) throw new IOException("bt socket connect failed");
                FileDescriptor fd = mPfd.getFileDescriptor();
                mSocket = LocalSocket.createConnectedLocalSocket(fd);
                mSocketIS = mSocket.getInputStream();
                mSocketOS = mSocket.getOutputStream();
            }
            int channel = readInt(mSocketIS);
            if (channel <= 0) {
                throw new IOException("bt socket connect failed");
            }
            mPort = channel;
            waitSocketSignal(mSocketIS);
            synchronized (this) {
                if (mSocketState == SocketState.CLOSED) {
                    throw new IOException("bt socket closed");
                }
                mSocketState = SocketState.CONNECTED;
            }
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            throw new IOException("unable to send RPC: " + e.getMessage());
        }
    }
```



## 结论



## 解决方案

* 

