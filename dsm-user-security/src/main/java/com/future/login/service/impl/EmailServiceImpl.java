package com.future.login.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.future.login.service.EmailService;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static constants.RedisCacheConstants.USER_EMAILCAPTCHA;


@Service
public class EmailServiceImpl implements EmailService {
   @Resource
   private JavaMailSender javaMailSender;
   @Resource
   private MailProperties mailProperties;
   @Autowired
   private StringRedisTemplate stringRedisTemplate;
   private static final long  TIME_EXCEPTION=300L;
    @Override
    public Result sendCaptcha(String toEmail) throws MessagingException, IOException {
        if(toEmail==null||toEmail.trim()==null)
        {
            return Result.fail("收件邮箱不能为空");
        }
        //生成6位验证码
        String captcha = RandomUtil.randomNumbers(6);
        String key=USER_EMAILCAPTCHA+toEmail;
        //2.判断redis中的验证码是否过期
        String emailCaptca = stringRedisTemplate.opsForValue().get(key);
        if(!Objects.isNull(emailCaptca))
        {
            Long expire = stringRedisTemplate.getExpire(key);
            return Result.fail("验证码还未过期"+",请"+expire+"秒后尝试");
        }
        // 3. 创建 MIME 邮件
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        // 4. 设置基本信息
        helper.setFrom(mailProperties.getUsername()); // 发件人
        helper.setTo(toEmail);                     // 收件人
        helper.setSubject("【白昼音流】您的邮箱验证码"); // 邮件标题
        // 4. 加载 Logo 图片并嵌入邮件（CID 方式）
        ClassPathResource logoResource = new ClassPathResource("static/images/logo.jpeg");
        helper.addInline("logo", logoResource);
        String htmlContent = buildHtmlEmailContentWithLogo(captcha);
        helper.setText(htmlContent, true); // true 表示是 HTML
        //发送
        try
        {
            javaMailSender.send(mimeMessage);
        }catch (Exception e)
        {
            return Result.fail("收件人邮箱地址无效");
        }
        //把验证码存入riedis
        stringRedisTemplate.opsForValue().set(key,captcha);
        stringRedisTemplate.expire(key,TIME_EXCEPTION, TimeUnit.SECONDS);
        return Result.ok();
    }
    private String buildHtmlEmailContentWithLogo(String captcha) {
        return "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 0; }" +
                ".email-container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                ".header { text-align: center; margin-bottom: 25px; }" +
                ".logo-img { max-width: 100px; height: auto; }" +
                ".title { font-size: 24px; font-weight: bold; color: #333; margin: 10px 0; }" +
                ".content { font-size: 16px; line-height: 1.6; color: #555; text-align: center; }" +
                ".code-box { background: #f8f9fa; border: 2px dashed #dee2e6; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: center; }" +
                ".code { font-size: 28px; font-weight: bold; color: #007bff; font-family: 'Courier New', monospace; letter-spacing: 4px; }" +
                ".footer { text-align: center; font-size: 14px; color: #888; margin-top: 30px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='email-container'>" +
                "<div class='header'>" +
                "<img src='cid:logo' alt='白昼音流 Logo' class='logo-img' />" +
                "<div class='title'>🎵 白昼音流</div>" +
                "</div>" +
                "<div class='content'>" +
                "<p>您好！您正在注册 <strong>白昼音流</strong>，请查收您的验证码：</p>" +
                "<div class='code-box'>" +
                "<div class='code'>" + captcha + "</div>" +
                "</div>" +
                "<p><strong>该验证码 5 分钟内有效，请勿泄露给他人。</strong></p>" +
                "<p>如非本人操作，请忽略此邮件。</p>" +
                "</div>" +
                "<div class='footer'>" +
                "© 2025 白昼音流团队. 保留所有权利。" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
