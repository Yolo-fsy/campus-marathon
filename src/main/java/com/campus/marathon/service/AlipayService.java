package com.campus.marathon.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * 支付宝沙箱支付封装：电脑网站支付下单、回调验签、退款。
 * alipay.mock=true 时不发起真实请求，由上层直接模拟支付成功，便于本地演示。
 */
public class AlipayService {

    private static final String SIGN_TYPE = "RSA2";
    private static final String CHARSET = "UTF-8";

    private final boolean mock;
    private final String appId;
    private final String privateKey;
    private final String alipayPublicKey;
    private final String gateway;
    private final String notifyUrl;
    private final String returnUrl;
    private final String amount;
    private final String subject;

    public AlipayService(Properties props) {
        this.mock = Boolean.parseBoolean(props.getProperty("alipay.mock", "true"));
        this.appId = props.getProperty("alipay.app_id", "");
        this.privateKey = props.getProperty("alipay.private_key", "");
        this.alipayPublicKey = props.getProperty("alipay.alipay_public_key", "");
        this.gateway = props.getProperty("alipay.gateway",
                "https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        this.notifyUrl = props.getProperty("alipay.notify_url", "");
        this.returnUrl = props.getProperty("alipay.return_url", "");
        this.amount = props.getProperty("alipay.amount", "30.00");
        this.subject = props.getProperty("alipay.subject", "校园马拉松报名费");
    }

    /** 从 classpath 的 alipay.properties 加载配置，文件缺失时退化为 mock 模式。 */
    public static AlipayService loadDefault() {
        Properties props = new Properties();
        try (InputStream in = AlipayService.class.getClassLoader().getResourceAsStream("alipay.properties")) {
            if (in != null) {
                // properties 默认按 ISO-8859-1 解码，中文 subject 会乱码；显式按 UTF-8 读取
                props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // 读取失败时保持默认 mock 配置
        }
        return new AlipayService(props);
    }

    public boolean isMock() {
        return mock;
    }

    public String getAmount() {
        return amount;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * 生成电脑网站支付的跳转表单（自动提交的 HTML），调用方直接写回响应即可跳转收银台。
     */
    public String buildPayForm(String outTradeNo) throws AlipayApiException {
        AlipayClient client = newClient();
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(returnUrl);
        request.setNotifyUrl(notifyUrl);
        request.setBizContent("{"
                + "\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
                + "}");
        return client.pageExecute(request).getBody();
    }

    /** 校验支付宝同步/异步回调签名。签名缺失、格式非法或公钥异常一律视为验签不通过。 */
    public boolean verifyCallback(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(params, alipayPublicKey, CHARSET, SIGN_TYPE);
        } catch (AlipayApiException e) {
            return false;
        }
    }

    /**
     * 取消报名退款：向支付宝发起 alipay.trade.refund。返回是否退款成功。
     */
    public boolean refund(String outTradeNo, String refundAmount) throws AlipayApiException {
        if (mock) {
            return true;
        }
        AlipayClient client = newClient();
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizContent("{"
                + "\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"refund_amount\":\"" + refundAmount + "\","
                + "\"out_request_no\":\"RF" + outTradeNo + "\""
                + "}");
        AlipayTradeRefundResponse response = client.execute(request);
        return response.isSuccess() && "Y".equals(response.getFundChange());
    }

    private AlipayClient newClient() {
        return new DefaultAlipayClient(gateway, appId, privateKey, "json", CHARSET, alipayPublicKey, SIGN_TYPE);
    }
}
