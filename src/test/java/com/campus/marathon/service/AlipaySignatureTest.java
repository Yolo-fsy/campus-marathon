package com.campus.marathon.service;

import com.alipay.api.internal.util.AlipaySignature;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付宝回调验签链路验证：使用真实 RSA2（SHA256withRSA）密钥对，
 * 模拟支付宝服务器对异步通知参数签名，再由 AlipayService.verifyCallback 验签。
 * 密钥算法、签名方式（RSA2）、字符集（UTF-8）与支付宝沙箱完全一致。
 */
class AlipaySignatureTest {

    @Test
    void validSignaturePassesAndTamperedParamsFail() throws Exception {
        // 生成真实 RSA 密钥对（应用私钥签名，"支付宝公钥"位置放配对公钥用于验签）
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // 构造模拟的支付宝异步通知参数（不含 sign_type，与 rsaCheckV1 的验签内容一致）
        Map<String, String> signed = new HashMap<>();
        signed.put("out_trade_no", "MR1234567890");
        signed.put("trade_status", "TRADE_SUCCESS");
        signed.put("total_amount", "30.00");
        signed.put("app_id", "2021000122000000");
        signed.put("charset", "UTF-8");
        signed.put("notify_time", "2026-10-30 21:30:00");

        // 按 rsaCheckV1 的规则拼接待签名串：参数按 key 字典序 k=v 以 & 连接，排除 sign/sign_type
        String content = "app_id=2021000122000000"
                + "&charset=UTF-8"
                + "&notify_time=2026-10-30 21:30:00"
                + "&out_trade_no=MR1234567890"
                + "&total_amount=30.00"
                + "&trade_status=TRADE_SUCCESS";

        // 用私钥签名（与支付宝沙箱同一算法 RSA2/UTF-8）
        String sign = AlipaySignature.rsaSign(content, privateKey, "UTF-8", "RSA2");

        Properties props = new Properties();
        props.setProperty("alipay.alipay_public_key", publicKey);
        AlipayService service = new AlipayService(props);

        // 签名正确的通知通过验签
        Map<String, String> callback = new HashMap<>(signed);
        callback.put("sign", sign);
        assertTrue(service.verifyCallback(callback), "正确签名应通过验签");

        // 篡改金额后验签必须失败（防伪造通知）
        Map<String, String> tampered = new HashMap<>(signed);
        tampered.put("total_amount", "0.01");
        tampered.put("sign", sign);
        assertFalse(service.verifyCallback(tampered), "篡改参数后验签必须失败");

        // 签名格式非法（非 256 字节签名）也必须返回 false，不能抛出异常导致回调接口 500
        Map<String, String> garbage = new HashMap<>(signed);
        garbage.put("sign", "ZmFrZXNpZ24=");
        assertFalse(service.verifyCallback(garbage), "非法签名格式应视为验签不通过");
    }

    @Test
    void chineseSubjectSurvivesUtf8PropertiesLoading() throws Exception {
        // 复现收银台中文乱码：properties 若按 ISO-8859-1 解码，subject 会变成 æ ¡å›­...
        String raw = "alipay.subject=校园马拉松报名费\n";
        Properties iso = new Properties();
        iso.load(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        assertEquals("校园马拉松报名费".equals(iso.getProperty("alipay.subject")), false,
                "按 ISO-8859-1 加载应产生乱码（回归保护）");

        // 修复方式：显式以 UTF-8 Reader 加载，与 AlipayService.loadDefault 保持一致
        Properties utf8 = new Properties();
        utf8.load(new InputStreamReader(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8));
        AlipayService service = new AlipayService(utf8);
        assertEquals("校园马拉松报名费", service.getSubject());
    }
}
