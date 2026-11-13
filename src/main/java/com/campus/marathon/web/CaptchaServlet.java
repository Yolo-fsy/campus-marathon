package com.campus.marathon.web;

import com.campus.marathon.service.CaptchaService;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * /register/captcha ：生成注册用的四位数字图形验证码（PNG，5 分钟内有效）。
 */
public class CaptchaServlet extends HttpServlet {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Color[] INK = {
            new Color(255, 90, 31),
            new Color(23, 36, 58),
            new Color(37, 99, 235),
            new Color(15, 118, 110)
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        String code = CaptchaService.issue(session);

        BufferedImage image = draw(code);
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        ImageIO.write(image, "png", response.getOutputStream());
    }

    private BufferedImage draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(255, 244, 239),
                    WIDTH, HEIGHT, new Color(240, 246, 255)));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(150 + RANDOM.nextInt(80),
                        160 + RANDOM.nextInt(70),
                        190 + RANDOM.nextInt(50)));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
            // 噪点
            for (int i = 0; i < 36; i++) {
                g.setColor(new Color(120 + RANDOM.nextInt(110),
                        130 + RANDOM.nextInt(100),
                        150 + RANDOM.nextInt(90)));
                g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
            }

            // 数字逐个轻微旋转，增加机器识别难度
            int step = WIDTH / (code.length() + 1);
            for (int i = 0; i < code.length(); i++) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
                g.setColor(INK[RANDOM.nextInt(INK.length)]);
                int x = step + i * step - 8;
                int y = HEIGHT / 2 + 8 + RANDOM.nextInt(5);
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(-14 + RANDOM.nextInt(28)), x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(old);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
