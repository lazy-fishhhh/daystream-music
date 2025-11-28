package com.future.login.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.future.login.service.EmailService;
import constants.RegexPatterns;
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
        if (!RegexPatterns.matches(toEmail, RegexPatterns.EMAIL_REGEX))
        {
            return Result.fail("邮箱格式不正确，请输入正确的邮箱");
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
        ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
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
        return  "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<style>\n" +
                "/* 模拟邮件客户端环境 */\n" +
                ".email-client-mockup {\n" +
                "  max-width: 700px;\n" +
                "  margin: 40px auto;\n" +
                "  border: 1px solid #333;\n" +
                "  border-radius: 12px;\n" +
                "  overflow: hidden;\n" +
                "  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);\n" +
                "  background-color: black;\n" +
                "}\n" +
                "\n" +
                ".email-container {\n" +
                "  max-width: 600px;\n" +
                "  margin: 0 auto;\n" +
                "  background: #020000;\n" +
                "  padding: 30px;\n" +
                "  border-radius: 0;\n" +
                "  color: #f0f0f0;\n" +
                "}\n" +
                "\n" +
                ".header {\n" +
                "  text-align: center;\n" +
                "  margin-bottom: 25px;\n" +
                "}\n" +
                "\n" +
                ".logo-title-group {\n" +
                "  display: flex;\n" +
                "  align-items: center;\n" +
                "  justify-content: center;\n" +
                "  gap: 12px;\n" +
                "}\n" +
                "\n" +
                ".logo-img {\n" +
                "  width: 60px;\n" +
                "  height: 60px;\n" +
                "  object-fit: cover;\n" +
                "  border-radius: 50%;\n" +
                "  border: 2px solid #ff2e93;\n" +
                "  padding: 2px;\n" +
                "}\n" +
                "\n" +
                ".title {\n" +
                "  font-size: 26px;\n" +
                "  font-weight: bold;\n" +
                "  color: #ffffff;\n" +
                "  text-shadow: 0 0 8px rgba(255, 46, 147, 0.3);\n" +
                "  margin: 0;\n" +
                "}\n" +
                "\n" +
                ".content {\n" +
                "  font-size: 16px;\n" +
                "  line-height: 1.8;\n" +
                "  color: #bbbbbb;\n" +
                "  text-align: center;\n" +
                "  padding: 0 20px;\n" +
                "}\n" +
                "\n" +
                ".content strong {\n" +
                "  color: #ffffff;\n" +
                "}\n" +
                "\n" +
                ".code-box {\n" +
                "  background: #1e1e1e;\n" +
                "  border: 2px dashed #ff2e9333;\n" +
                "  border-radius: 8px;\n" +
                "  padding: 20px 10px;\n" +
                "  margin: 25px 0;\n" +
                "  text-align: center;\n" +
                "  position: relative;\n" +
                "  overflow: hidden;\n" +
                "}\n" +
                "\n" +
                ".code-box::before {\n" +
                "  content: '';\n" +
                "  position: absolute;\n" +
                "  top: -50%;\n" +
                "  left: -50%;\n" +
                "  width: 200%;\n" +
                "  height: 200%;\n" +
                "  background: linear-gradient(to right, rgba(255, 46, 147, 0) 0%, rgba(255, 46, 147, 0.1) 50%, rgba(255, 46, 147, 0) 100%);\n" +
                "  transform: rotate(30deg);\n" +
                "  animation: shine 3s infinite;\n" +
                "}\n" +
                "\n" +
                "@keyframes shine {\n" +
                "  0% { transform: rotate(30deg) translateX(-100%); }\n" +
                "  100% { transform: rotate(30deg) translateX(100%); }\n" +
                "}\n" +
                "\n" +
                ".code {\n" +
                "  font-size: 32px;\n" +
                "  font-weight: bold;\n" +
                "  color: #ff2e93;\n" +
                "  font-family: 'Courier New', monospace;\n" +
                "  letter-spacing: 6px;\n" +
                "  position: relative;\n" +
                "  text-shadow: 0 0 10px rgba(255, 46, 147, 0.5);\n" +
                "}\n" +
                "\n" +
                ".footer {\n" +
                "  text-align: center;\n" +
                "  font-size: 14px;\n" +
                "  color: #666666;\n" +
                "  margin-top: 30px;\n" +
                "  padding-top: 20px;\n" +
                "  border-top: 1px solid #2d2d2d;\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"email-client-mockup\">\n" +
                "  <div class=\"email-container\">\n" +
                "    <div class=\"header\">\n" +
                "      <div class=\"logo-title-group\">\n" +
                "        <img src=\"cid:logo\" alt=\"白昼音流 Logo\" class=\"logo-img\" />\n" +
                "        <div class=\"title\">白昼音流</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <p>您好！您正在注册 <strong>白昼音流</strong>，请查收您的验证码：</p>\n" +
                "      <div class=\"code-box\">\n" +
                "        <div class=\"code\">" + captcha + "</div>\n" +
                "      </div>\n" +
                "      <p><strong>该验证码 5 分钟内有效，请勿泄露给他人。</strong></p>\n" +
                "      <p>如非本人操作，请忽略此邮件。</p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">© 2025 白昼音流团队. 保留所有权利。</div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";
    }
}
