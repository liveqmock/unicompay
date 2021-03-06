package com.chinaunicom.unipay.ws.controllers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.chinaunicom.unipay.ws.persistence.ChargePoint;
import com.chinaunicom.unipay.ws.persistence.Order;
import com.chinaunicom.unipay.ws.persistence.UserInfo;
import com.chinaunicom.unipay.ws.services.ICPService;
import com.chinaunicom.unipay.ws.services.IMessageService;
import com.chinaunicom.unipay.ws.services.ISMSService;
import com.chinaunicom.unipay.ws.utils.MD5;
import com.chinaunicom.unipay.ws.utils.RedisUtil;
import com.chinaunicom.unipay.ws.utils.Tools;
import com.chinaunicom.unipay.ws.utils.VerifyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.tools.Tool;
import java.util.Map;
import java.util.TreeMap;

import static com.chinaunicom.unipay.ws.utils.RedisUtil.MINUTE;

/**
 * Created by jackj_000 on 2015/2/27 0027.
 */
public class SMSController extends WSController{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String mid = "";
    private final String appid = "";
    @Resource RedisUtil ru;
    @Resource
    ISMSService iss;
    @Resource ICPService cps;
    @Resource IMessageService ms;
    class SmsRequest extends SDKRequest{

    }
    class SmsRes extends Response{
        private String smscontent;
        private String smsaddress;

        SmsRes(String smscontent,String smsaddress){
            this.smscontent = smscontent;
            this.smsaddress = smsaddress;
        }
    }
    public void pay() throws Exception {

        final HttpServletRequest r = getRequest();

        SmsRequest sr  = getJSONObject(SmsRequest.class);

        Order order = createOrder(sr);
        String orderid = order.getOrderid();

        ISMSService.SmsRequest isr = new ISMSService.SmsRequest();

        isr.setMerchantid(mid);
        isr.setAppid(appid);
        isr.setLinkid(orderid);
        isr.setPaypoint("");
        isr.setPrice("");
        isr.setIp(StringUtils.isEmpty(r.getHeader("X-Real-IP")) ? r.getRemoteAddr() : r.getHeader("X-Real-IP"));
        isr.setImsi(sr.getImsi());
        isr.setMac(sr.getTerminalid());
        isr.setOrdertime(Tools.getCurrentTime());
        isr.setNotifyurl("");

        Map<String,String> map = JSON.parseObject(JSON.toJSONString(isr),new TypeReference<TreeMap>(){});
        String verify = VerifyUtil.getVerify(map);

        isr.setMd5(MD5.md5Digest(verify));

        ISMSService.SmsResponse res = iss.charge(isr);
        Response sres= null;
        if(res.isSuccess()){
            sres = new SmsRes(res.getSmscontent(),res.getSmsaddress());
        }else {
            sres = new Response(399,res.getErrormsg());
        }
        renderJson(sres);

    }
    class CallbackResponse{
        private String payid;
        private String linkid;
        private int status;
        private String chargemsg;
        private String price;
        private String md5;

        public String getPayid() {
            return payid;
        }

        public void setPayid(String payid) {
            this.payid = payid;
        }

        public String getLinkid() {
            return linkid;
        }

        public void setLinkid(String linkid) {
            this.linkid = linkid;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getChargemsg() {
            return chargemsg;
        }

        public void setChargemsg(String chargemsg) {
            this.chargemsg = chargemsg;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public boolean isSuccess(){return status == 0;}
    }
    class SmsResponse{
        private String status;
        private String errormsg;
        public SmsResponse(){}
        public SmsResponse(String status,String errormsg){
            this.status = status;
            this.errormsg = errormsg;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrormsg() {
            return errormsg;
        }

        public void setErrormsg(String errormsg) {
            this.errormsg = errormsg;
        }
    }

    public void callback() throws Exception {
        CallbackResponse cr = getJSONObject(CallbackResponse.class);
        Map<String,String> map = JSON.parseObject(JSON.toJSONString(cr),new TypeReference<TreeMap>(){});
        String verify = VerifyUtil.getVerify(map);
        if(!verify.equals(cr.getMd5()))
            return;
        String orderid = cr.getLinkid();
        if(ru.getSet(RedisUtil.Table.CALLBACK.getKey(orderid), "y", 30 * MINUTE) != null) {
            logger.debug("订单[" + orderid + "]重复发送支付结果通知");
            return;
        }

        Order order = Order.dao.findById(orderid);
        if(order != null) {
            order.setPayflowid(cr.getPayid())
                    .setStatus(cr.isSuccess() ? 0 : 1)
                    .setPayresult(cr.isSuccess() ? "00000" : "1")
                    .setErrorcode(cr.isSuccess() ? "00000" : "1")
                    .update();
        } else {
            logger.error("[" + orderid + "]无法在订单库中找到");
        }

        if(cr.getStatus() != 0) {
            renderJson(new SmsResponse("ERROR", "失败"));
            return;
        }
         renderJson(new SmsResponse("OK","成功"));

        //通知CP
        ICPService.Notification n = new ICPService.Notification();
        n.setAppid(order.getProductid());
        n.setConsumecode(order.getPointid());
        n.setCpid(order.getCpid());
        n.setCporderid(order.getOrderid_3rd());
        n.setOrderid(order.getOrderid());
        n.setFid(order.getChannelid());
        n.setOrdertime(order.getPaytime());
        n.setPayfee(String.valueOf(order.getPayfee()));
        n.setStatus(order.getStatus());
        n.setPaytype(7);
        try {
            cps.sendNotification(n);
        } catch (Exception e) {
            logger.error("CP通知失败", e);
        }

        //通知消息系统
        IMessageService.Message msg = new IMessageService.Message();
        msg.setCpid(order.getCpid());
        msg.setPayresult(order.getPayresult());
        msg.setOrderid(order.getOrderid());
        msg.setCporderid(order.getOrderid_3rd());
        msg.setPayfee(order.getPayfee());
        msg.setPaytime(order.getPaytime());
        msg.setPaytype(IMessageService.Message.PayType.SMS.getValue());
        msg.setServiceid(order.getServicekey());
        msg.setStatus(order.getStatus());
        try {
            ms.notify(msg);
        } catch (Exception e) {
            logger.error("消息通知失败", e);
        }

    }
    private Order createOrder(SmsRequest pay) throws Exception {

        String consumecode = pay.getConsumecode();
        String cpid = pay.getCpid();
        String channelid = pay.getChannelid();

        if(!cps.checkAuth(consumecode, cpid, channelid)){
            logger.debug("[" + pay.getCporderid() + "]鉴权失败");
            throw new Exception("[" + pay.getCporderid() + "]鉴权失败");
        }

        UserInfo userInfo = UserInfo.dao.getByCpid(cpid);
        ChargePoint point = ChargePoint.dao.getByConsumecode(consumecode);

        Order o = new Order();
        o.setOrderid(Tools.getUUID());
        o.setEncryptparam("11");
        o.setOrderid_3rd(pay.getCporderid());
        o.setOrdertime(pay.getOrdertime());
        o.setServicekey(pay.getServiceid());
        o.setImsi(pay.getImsi());
        o.setUseraccount(pay.getIdentityid());
        o.setPaytime(Tools.getCurrentTime());
        o.setSdkversion(pay.getSdkversion());
        o.setChannelid(channelid);
        o.setEmpno(pay.getAssistantid());
        o.setCpid(cpid);
        o.setPointid(consumecode);

        o.setUserindex(userInfo.getUserindex());
        o.setUserid(userInfo.getUserid());
        o.setProductindex(point.getCntindex());
        o.setProductid(point.getProduct().getCntid());
        o.setProductname(point.getProduct().getCntname());
        o.setPointindex(point.getPointindex());
        o.setPointname(point.getPointname());
        o.setPayfee(point.getPointvalue());

        o.save();

        return o;
    }
}
