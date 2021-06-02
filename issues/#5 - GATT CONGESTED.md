# GATT CONGESTED

[TOC]

##  现象描述

使用 writeCommand 的方式发送数据时，出现收不到 onCharacteristicWrite callback 的现象，定位 logcat 发现底层发送阻塞了。

```java
bt_stack: [ERROR:gatt_cl.cc(224)] gatt_act_write() failed op_code=0x52 rt=143
```

## 源码分析



### gatt_att_write()

> /android/platform/system/bt/stack/gatt/gatt_cl.cc

```c++
/** GATT write operation */
void gatt_act_write(tGATT_CLCB* p_clcb, uint8_t sec_act) {
  tGATT_TCB& tcb = *p_clcb->p_tcb;

  CHECK(p_clcb->p_attr_buf);
  tGATT_VALUE& attr = *((tGATT_VALUE*)p_clcb->p_attr_buf);

  switch (p_clcb->op_subtype) {
    case GATT_WRITE_NO_RSP: {
      p_clcb->s_handle = attr.handle;
      uint8_t op_code = (sec_act == GATT_SEC_SIGN_DATA) ? GATT_SIGN_CMD_WRITE
                                                        : GATT_CMD_WRITE;
      uint8_t rt = gatt_send_write_msg(tcb, p_clcb, op_code, attr.handle,
                                       attr.len, 0, attr.value);
      if (rt != GATT_CMD_STARTED) {
        if (rt != GATT_SUCCESS) {
          LOG(ERROR) << StringPrintf(
              "gatt_act_write() failed op_code=0x%x rt=%d", op_code, rt);
        }
        gatt_end_operation(p_clcb, rt, NULL);
      }
      return;
    }

    case GATT_WRITE: {
      if (attr.len <= (tcb.payload_size - GATT_HDR_SIZE)) {
        p_clcb->s_handle = attr.handle;

        uint8_t rt = gatt_send_write_msg(tcb, p_clcb, GATT_REQ_WRITE,
                                         attr.handle, attr.len, 0, attr.value);
        if (rt != GATT_SUCCESS && rt != GATT_CMD_STARTED &&
            rt != GATT_CONGESTED) {
          if (rt != GATT_SUCCESS) {
            LOG(ERROR) << StringPrintf(
                "gatt_act_write() failed op_code=0x%x rt=%d", GATT_REQ_WRITE,
                rt);
          }
          gatt_end_operation(p_clcb, rt, NULL);
        }

      } else {
        /* prepare write for long attribute */
        gatt_send_prepare_write(tcb, p_clcb);
      }
      return;
    }

    case GATT_WRITE_PREPARE:
      gatt_send_prepare_write(tcb, p_clcb);
      return;

    default:
      CHECK(false) << "Unknown write type" << p_clcb->op_subtype;
      return;
  }
}
```



#### gatt_send_write_msg

> /android/platform/system/bt/stack/gatt/gatt_utils.cc

```c++
/** Send out the ATT message for write */
uint8_t gatt_send_write_msg(tGATT_TCB& tcb, tGATT_CLCB* p_clcb, uint8_t op_code,
                            uint16_t handle, uint16_t len, uint16_t offset,
                            uint8_t* p_data) {
  tGATT_CL_MSG msg;
  msg.attr_value.handle = handle;
  msg.attr_value.len = len;
  msg.attr_value.offset = offset;
  memcpy(msg.attr_value.value, p_data, len);

  /* write by handle */
  return attp_send_cl_msg(tcb, p_clcb, op_code, &msg);
}
```

#### attp_send_cl_msg

> /android/platform/system/bt/stack/gatt/att_protocol.cc

```c++
/*******************************************************************************
 *
 * Function         attp_send_cl_msg
 *
 * Description      This function sends the client request or confirmation
 *                  message to server.
 *
 * Parameter        p_tcb: pointer to the connectino control block.
 *                  p_clcb: clcb
 *                  op_code: message op code.
 *                  p_msg: pointer to message parameters structure.
 *
 * Returns          GATT_SUCCESS if sucessfully sent; otherwise error code.
 *
 *
 ******************************************************************************/
tGATT_STATUS attp_send_cl_msg(tGATT_TCB& tcb, tGATT_CLCB* p_clcb,
                              uint8_t op_code, tGATT_CL_MSG* p_msg) {
  BT_HDR* p_cmd = NULL;
  uint16_t offset = 0, handle;
  switch (op_code) {
    case GATT_REQ_MTU:
      if (p_msg->mtu > GATT_MAX_MTU_SIZE) return GATT_ILLEGAL_PARAMETER;

      tcb.payload_size = p_msg->mtu;
      p_cmd = attp_build_mtu_cmd(GATT_REQ_MTU, p_msg->mtu);
      break;

    case GATT_REQ_FIND_INFO:
    case GATT_REQ_READ_BY_TYPE:
    case GATT_REQ_READ_BY_GRP_TYPE:
      if (!GATT_HANDLE_IS_VALID(p_msg->browse.s_handle) ||
          !GATT_HANDLE_IS_VALID(p_msg->browse.e_handle) ||
          p_msg->browse.s_handle > p_msg->browse.e_handle)
        return GATT_ILLEGAL_PARAMETER;

      p_cmd = attp_build_browse_cmd(op_code, p_msg->browse.s_handle,
                                    p_msg->browse.e_handle, p_msg->browse.uuid);
      break;

    case GATT_REQ_READ_BLOB:
      offset = p_msg->read_blob.offset;
      FALLTHROUGH_INTENDED; /* FALLTHROUGH */
    case GATT_REQ_READ:
      handle =
          (op_code == GATT_REQ_READ) ? p_msg->handle : p_msg->read_blob.handle;
      /*  handle checking */
      if (!GATT_HANDLE_IS_VALID(handle)) return GATT_ILLEGAL_PARAMETER;

      p_cmd = attp_build_handle_cmd(op_code, handle, offset);
      break;

    case GATT_HANDLE_VALUE_CONF:
      p_cmd = attp_build_opcode_cmd(op_code);
      break;

    case GATT_REQ_PREPARE_WRITE:
      offset = p_msg->attr_value.offset;
      FALLTHROUGH_INTENDED; /* FALLTHROUGH */
    case GATT_REQ_WRITE:
    case GATT_CMD_WRITE:
    case GATT_SIGN_CMD_WRITE:
      if (!GATT_HANDLE_IS_VALID(p_msg->attr_value.handle))
        return GATT_ILLEGAL_PARAMETER;

      p_cmd = attp_build_value_cmd(
          tcb.payload_size, op_code, p_msg->attr_value.handle, offset,
          p_msg->attr_value.len, p_msg->attr_value.value);
      break;

    case GATT_REQ_EXEC_WRITE:
      p_cmd = attp_build_exec_write_cmd(op_code, p_msg->exec_write);
      break;

    case GATT_REQ_FIND_TYPE_VALUE:
      p_cmd = attp_build_read_by_type_value_cmd(tcb.payload_size,
                                                &p_msg->find_type_value);
      break;

    case GATT_REQ_READ_MULTI:
      p_cmd = attp_build_read_multi_cmd(tcb.payload_size,
                                        p_msg->read_multi.num_handles,
                                        p_msg->read_multi.handles);
      break;

    default:
      break;
  }

  if (p_cmd == NULL) return GATT_NO_RESOURCES;

  return attp_cl_send_cmd(tcb, p_clcb, op_code, p_cmd);
}

```



#### attp_cl_send_cmd

> /android/platform/system/bt/stack/gatt/att_protocol.cc

```c++
/*******************************************************************************
 *
 * Function         attp_cl_send_cmd
 *
 * Description      Send a ATT command or enqueue it.
 *
 * Returns          GATT_SUCCESS if command sent
 *                  GATT_CONGESTED if command sent but channel congested
 *                  GATT_CMD_STARTED if command queue up in GATT
 *                  GATT_ERROR if command sending failure
 *
 ******************************************************************************/
tGATT_STATUS attp_cl_send_cmd(tGATT_TCB& tcb, tGATT_CLCB* p_clcb,
                              uint8_t cmd_code, BT_HDR* p_cmd) {
  cmd_code &= ~GATT_AUTH_SIGN_MASK;

  if (!tcb.cl_cmd_q.empty() && cmd_code != GATT_HANDLE_VALUE_CONF) {
    gatt_cmd_enq(tcb, p_clcb, true, cmd_code, p_cmd);
    return GATT_CMD_STARTED;
  }

  /* no pending request or value confirmation */
  tGATT_STATUS att_ret = attp_send_msg_to_l2cap(tcb, p_cmd);
  if (att_ret != GATT_CONGESTED && att_ret != GATT_SUCCESS) {
    return GATT_INTERNAL_ERROR;
  }

  /* do not enq cmd if handle value confirmation or set request */
  if (cmd_code == GATT_HANDLE_VALUE_CONF || cmd_code == GATT_CMD_WRITE) {
    return att_ret;
  }

  gatt_start_rsp_timer(p_clcb);
  gatt_cmd_enq(tcb, p_clcb, false, cmd_code, NULL);
  return att_ret;
}

```



#### attp_send_msg_to_l2cap

> /android/platform/system/bt/stack/gatt/att_protocol.cc

```c++
/*******************************************************************************
 *
 * Function         attp_send_msg_to_l2cap
 *
 * Description      Send message to L2CAP.
 *
 ******************************************************************************/
tGATT_STATUS attp_send_msg_to_l2cap(tGATT_TCB& tcb, BT_HDR* p_toL2CAP) {
  uint16_t l2cap_ret;

  if (tcb.att_lcid == L2CAP_ATT_CID)
    l2cap_ret = L2CA_SendFixedChnlData(L2CAP_ATT_CID, tcb.peer_bda, p_toL2CAP);
  else
    l2cap_ret = (uint16_t)L2CA_DataWrite(tcb.att_lcid, p_toL2CAP);

  if (l2cap_ret == L2CAP_DW_FAILED) {
    LOG(ERROR) << __func__ << ": failed to write data to L2CAP";
    return GATT_INTERNAL_ERROR;
  } else if (l2cap_ret == L2CAP_DW_CONGESTED) {
    VLOG(1) << StringPrintf("ATT congested, message accepted");
    return GATT_CONGESTED;
  }
  return GATT_SUCCESS;
}
```



### gatt_end_operation

> /android/platform/system/bt/stack/gatt/gatt_utils.cc

```c++
/*******************************************************************************
 *
 * Function         gatt_end_operation
 *
 * Description      This function ends a discovery, send callback and finalize
 *                  some control value.
 *
 * Returns          16 bits uuid.
 *
 ******************************************************************************/
void gatt_end_operation(tGATT_CLCB* p_clcb, tGATT_STATUS status, void* p_data) {
  tGATT_CL_COMPLETE cb_data;
  tGATT_CMPL_CBACK* p_cmpl_cb =
      (p_clcb->p_reg) ? p_clcb->p_reg->app_cb.p_cmpl_cb : NULL;
  uint8_t op = p_clcb->operation, disc_type = GATT_DISC_MAX;
  tGATT_DISC_CMPL_CB* p_disc_cmpl_cb =
      (p_clcb->p_reg) ? p_clcb->p_reg->app_cb.p_disc_cmpl_cb : NULL;
  uint16_t conn_id;
  uint8_t operation;

  VLOG(1) << __func__
          << StringPrintf(" status=%d op=%d subtype=%d", status,
                          p_clcb->operation, p_clcb->op_subtype);
  memset(&cb_data.att_value, 0, sizeof(tGATT_VALUE));

  if (p_cmpl_cb != NULL && p_clcb->operation != 0) {
    if (p_clcb->operation == GATTC_OPTYPE_READ) {
      cb_data.att_value.handle = p_clcb->s_handle;
      cb_data.att_value.len = p_clcb->counter;

      if (p_data && p_clcb->counter)
        memcpy(cb_data.att_value.value, p_data, cb_data.att_value.len);
    }

    if (p_clcb->operation == GATTC_OPTYPE_WRITE) {
      memset(&cb_data.att_value, 0, sizeof(tGATT_VALUE));
      cb_data.handle = cb_data.att_value.handle = p_clcb->s_handle;
      if (p_clcb->op_subtype == GATT_WRITE_PREPARE) {
        if (p_data) {
          cb_data.att_value = *((tGATT_VALUE*)p_data);
        } else {
          VLOG(1) << "Rcv Prepare write rsp but no data";
        }
      }
    }

    if (p_clcb->operation == GATTC_OPTYPE_CONFIG)
      cb_data.mtu = p_clcb->p_tcb->payload_size;

    if (p_clcb->operation == GATTC_OPTYPE_DISCOVERY) {
      disc_type = p_clcb->op_subtype;
    }
  }

  osi_free_and_reset((void**)&p_clcb->p_attr_buf);

  operation = p_clcb->operation;
  conn_id = p_clcb->conn_id;
  alarm_cancel(p_clcb->gatt_rsp_timer_ent);

  gatt_clcb_dealloc(p_clcb);

  if (p_disc_cmpl_cb && (op == GATTC_OPTYPE_DISCOVERY))
    (*p_disc_cmpl_cb)(conn_id, disc_type, status);
  else if (p_cmpl_cb && op)
    (*p_cmpl_cb)(conn_id, op, status, &cb_data);
  else
    LOG(WARNING) << __func__
                 << StringPrintf(
                        ": not sent out op=%d p_disc_cmpl_cb:%p p_cmpl_cb:%p",
                        operation, p_disc_cmpl_cb, p_cmpl_cb);
}
```



## 结论

原因是SPP Port 已经被其他应用占用并打开。



## 解决方案

* 

