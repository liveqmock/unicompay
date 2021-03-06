package com.chinaunicom.unipay.ws.services;

/**
 * User: Frank
 * Date: 2014/11/20
 * Time: 16:50
 */
public class UnipayException extends RuntimeException {
    //支付类型为空或不支持
    public static final String UN_SUPPORT_PAYTYPE = "00301";
    //用户或绑卡ID不存在
    public static final String UN_ISEMPTY_USERACCOUNT ="00302";
    //订单校验失败
    public static final String UN_CPORDER_VALIDATE = "00303";
    //银行卡限额不存在
    public static final String UN_CARDQUOTA_EXISTS = "00304";

    private String orderid;
    protected String code;

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UnipayException(String orderid) {
        super();
        this.code = "00399";
        this.orderid = orderid;
    }

    public UnipayException(String message, String orderid) {
        super(message);
        this.code = "00399";
        this.orderid = orderid;
    }

    public UnipayException(String code, String message, String orderid) {
        super(message);
        this.code = code;
        this.orderid = orderid;
    }
}
