package net.cooliang.common.utils.mail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;

public class MailUtil {

	private JavaMailSenderImpl mailSender;

	public void setMailSender(JavaMailSenderImpl mailSender) {
		this.mailSender = mailSender;
	}

	/**
	 * 邮件发送
	 * @param sender 发送人
	 * @param receivers 收件人
	 * @param subject 标题
	 * @param text 邮件内容
	 * @param isHtml 配合text使用，true-按html格式发送text内容 false-按文本格式发送text内容
	 * @param files 附件
	 * @throws Exception
	 * @Author: cooliang
	 * @Date: 2016年4月19日 上午9:37:07
	 */
	public void send(String sender, String[] receivers, String subject, String text, boolean isHtml, File[] files) throws Exception {
		Map<String, InputStreamSource> sources = null;
		if (ArrayUtils.isNotEmpty(files)) {
			sources = new HashMap<String, InputStreamSource>();
			for (File file : files) {
				sources.put(file.getName(), new FileSystemResource(file));
			}
		}
		mailSender.send(createMimeMessage(sender, receivers, subject, text, isHtml, sources));
	}

	/**
	 * 邮件发送
	 * @param sender 发送人
	 * @param receivers 收件人
	 * @param subject 标题
	 * @param text 邮件内容
	 * @param isHtml 配合text使用，true-按html格式发送text内容 false-按文本格式发送text内容
	 * @param map 附件，key-附件名 value-附件流
	 * @throws Exception
	 * @Author: cooliang
	 * @Date: 2016年4月19日 下午7:41:07
	 */
	public void send(String sender, String[] receivers, String subject, String text, boolean isHtml, Map<String, byte[]> map) throws Exception {
		Map<String, InputStreamSource> sources = null;
		if (!CollectionUtils.isEmpty(map)) {
			sources = new HashMap<String, InputStreamSource>();
			for (Entry<String, byte[]> entry : map.entrySet()) {
				sources.put(entry.getKey(), new ByteArrayResource(entry.getValue()));
			}
		}
		mailSender.send(createMimeMessage(sender, receivers, subject, text, isHtml, sources));
	}

	private MimeMessage createMimeMessage(String sender, String[] receivers, String subject, String text, boolean isHtml, Map<String, InputStreamSource> sources) throws Exception {
		MimeMessage mailMessage = mailSender.createMimeMessage();
		MimeMessageHelper messageHelper = new MimeMessageHelper(mailMessage, true, "UTF-8");
		messageHelper.setFrom(sender);
		messageHelper.setTo(receivers);
		messageHelper.setSubject(subject);
		messageHelper.setText(text, isHtml);
		if (!CollectionUtils.isEmpty(sources)) {
			for (Entry<String, InputStreamSource> entry : sources.entrySet()) {
				messageHelper.addAttachment(entry.getKey(), entry.getValue());
			}
		}
		return mailMessage;
	}

}
