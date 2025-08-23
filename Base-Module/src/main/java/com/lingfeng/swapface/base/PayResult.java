package com.lingfeng.swapface.base;

import java.io.Serializable;

/**
 * 功能描述：
 * <p>
 * Author:lining
 * Date:2025/8/23
 */
public class PayResult implements Serializable {
    int resultCode;
    String data;

    String errorMsg;

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
