import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * @version 1.0
 * @date 2022/1/29 21:27
 */
public class JavaMailTest {


    public static void main(String[] args)
            throws MessagingException, GeneralSecurityException, GeneralSecurityException {
        Properties props = new Properties();

        // 开启debug调试
        props.setProperty("mail.debug", "true");
        // 发送服务器需要身份验证
        props.setProperty("mail.smtp.auth", "true");
        // 设置邮件服务器主机名
        props.setProperty("mail.host", "smtp.exmail.qq.com");
        // 发送邮件协议名称
        props.setProperty("mail.transport.protocol", "smtp");

        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.socketFactory", sf);

        Session session = Session.getInstance(props);

        Message msg = new MimeMessage(session);
        msg.setSubject("seenews 错误");
        StringBuilder builder = new StringBuilder();

        builder.append("\n时间 " + System.currentTimeMillis());
        msg.setText(builder.toString());
        msg.setFrom(new InternetAddress("chenqi@xhintech.com"));

        Transport transport = session.getTransport();
        transport.connect("smtp.exmail.qq.com", "chenqi@xhintech.com", "oRX8Mj6FyzX6kY6X");

        transport.sendMessage(msg, new Address[]{new InternetAddress("chenqi@xhintech.com")});
        transport.close();
    }
}

