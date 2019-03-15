package cqt.goai.run.notice;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 邮箱通知
 *
 * @author goai
 */
public class EmailNotice extends BaseNotice {

    private String mailSmtpHost = "smtp.exmail.qq.com";
    private String mailSmtpPort = "465";
    private String mailSmtpSocketFactoryPort = "465";
    private String username;
    private String password;
    private String sender = "goai";
    private String[] to;

    public EmailNotice(Logger log, String strategyName, JSONObject config) {
        super(log, strategyName);
        mailSmtpHost = config.containsKey("mailSmtpHost") ? config.getString("mailSmtpHost") : mailSmtpHost;
        mailSmtpPort = config.containsKey("mailSmtpPort") ? config.getString("mailSmtpPort") : mailSmtpPort;
        mailSmtpSocketFactoryPort = config.containsKey("mailSmtpSocketFactoryPort") ? config.getString("mailSmtpSocketFactoryPort") : mailSmtpSocketFactoryPort;
        username = config.getString("username");
        password = config.getString("password");
        sender = config.containsKey("sender") ? config.getString("sender") : sender;
        String to = config.getString("to");
        if (to.startsWith("[")) {
            this.to = config.getJSONArray("to").stream()
                    .map(Object::toString)
                    .collect(Collectors.toList())
                    .toArray(new String[]{});
        } else {
            this.to = new String[]{to};
        }
    }

    private void send(String content) {
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", mailSmtpHost);
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.port", mailSmtpPort);
        props.setProperty("mail.smtp.timeout", "5000");
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.socketFactory.port", mailSmtpSocketFactoryPort);
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.starttls.required", "true");
        props.setProperty("mail.smtp.ssl.enable", "true");
        Session session = Session.getInstance(props);
        Transport transport = null;
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, sender, "UTF-8"));
            InternetAddress[] internetAddressTo = new InternetAddress[to.length];
            for (int i = 0; i < internetAddressTo.length; i++) {
                internetAddressTo[i] = new InternetAddress(to[i]);
            }
            message.setRecipients(MimeMessage.RecipientType.TO, internetAddressTo);
            message.setSubject(strategyName, "UTF-8");
            message.setContent(content, "text/plain;charset=UTF-8");
            message.setSentDate(new Date());
            message.saveChanges();
            transport = session.getTransport();
            transport.connect(username, password);
            transport.sendMessage(message, message.getAllRecipients());
        } catch (UnsupportedEncodingException | MessagingException e) {
            e.printStackTrace();
        } finally {
            if (null != transport) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void notice(String message) {
        this.send(message);
    }
}
