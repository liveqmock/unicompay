package com.chinaunicom.unipay.ws;

import com.chinaunicom.unipay.ws.controllers.*;
import com.chinaunicom.unipay.ws.persistence.BindCard;
import com.chinaunicom.unipay.ws.persistence.ChargePoint;
import com.chinaunicom.unipay.ws.persistence.CheckPoint;
import com.chinaunicom.unipay.ws.persistence.Notify;
import com.chinaunicom.unipay.ws.persistence.Order;
import com.chinaunicom.unipay.ws.persistence.Quota;
import com.chinaunicom.unipay.ws.persistence.UserInfo;
import com.chinaunicom.unipay.ws.plugins.ioc.IocPlugin;
import com.jfinal.config.Constants;
import com.jfinal.config.Handlers;
import com.jfinal.config.Interceptors;
import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.CaseInsensitiveContainerFactory;
import com.jfinal.plugin.activerecord.dialect.OracleDialect;
import com.jfinal.plugin.c3p0.C3p0Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Frank
 * Date: 14-3-17
 * Time: 下午5:44
 */
public class Config extends JFinalConfig {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void configConstant(Constants constants) {
        constants.setEncoding("UTF-8");
    }

    @Override
    public void configRoute(Routes routes) {
        routes.add("/", PointController.class);
        routes.add("/bank", BankController.class);
        routes.add("/internal", InternalController.class);
        routes.add("/alipay", AliPayController.class);
        routes.add("/19pay", ChargeController.class);
        routes.add("/aliqrcode", AliQrcodeController.class);
        routes.add("/weixin", WeiXinController.class);
        routes.add("/telecom", TelecomController.class);
    }

    @Override
    public void configPlugin(Plugins plugins) {

        String active = System.getProperty("spring.profiles.active");
        if(active == null) {
            loadPropertyFile("env.properties");
            logger.debug("spring.profiles.active:" + getProperty("spring.profiles.active"));
            System.setProperty("spring.profiles.active", getProperty("spring.profiles.active"));
        }

        plugins.add(new IocPlugin("classpath:ws_ctx.xml"));

        loadPropertyFile("db.properties");
        logger.debug("oracle.url:" + getProperty("oracle.url") + ",oracle.username:" + getProperty("oracle.username") + ",oracle.password:" + getProperty("oracle.password") + ",oracle.driver:" + getProperty("oracle.driver"));
        C3p0Plugin cp = new C3p0Plugin(getProperty("oracle.url"), getProperty("oracle.username"), getProperty("oracle.password"), getProperty("oracle.driver"));
        plugins.add(cp);
        ActiveRecordPlugin rp = new ActiveRecordPlugin(cp);
        plugins.add(rp);
        rp.setShowSql(true);
        rp.setDialect(new OracleDialect());
        rp.setContainerFactory(new CaseInsensitiveContainerFactory());

        rp.addMapping(Order.TABLE, Order.ID, Order.class);
        rp.addMapping(BindCard.TABLE, BindCard.ID, BindCard.class);
        rp.addMapping(ChargePoint.TABLE, ChargePoint.ID, ChargePoint.class);
        rp.addMapping(ChargePoint.Product.TABLE, ChargePoint.Product.ID, ChargePoint.Product.class);
        rp.addMapping(UserInfo.TABLE, UserInfo.ID, UserInfo.class);
        rp.addMapping(UserInfo.Signup.TABLE, UserInfo.Signup.ID, UserInfo.Signup.class);
        rp.addMapping(Notify.TABLE, Notify.ID, Notify.class);
        rp.addMapping(Quota.TABLE, Quota.ID, Quota.class);
        rp.addMapping(CheckPoint.TABLE, CheckPoint.ID, CheckPoint.class);

    }

    @Override
    public void configInterceptor(Interceptors interceptors) {
//        interceptors.add(new Restful());
    }

    @Override
    public void configHandler(Handlers handlers) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
