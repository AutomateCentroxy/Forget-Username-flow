package org.gluu.agama.forgetusername;

import io.jans.as.common.service.common.UserService;
import io.jans.as.common.model.common.User;
import io.jans.model.SmtpConfiguration;
import io.jans.service.MailService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.common.service.common.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

import org.gluu.agama.usernameclass.UsernameResendclass;

public class JansForgetUsername extends UsernameResendclass {

    private static final Logger logger = LoggerFactory.getLogger(JansForgetUsername.class);

    private static final String UID = "uid";
    private static final String INUM_ATTR = "inum";
    private static final String LANG = "lang";
    private static final String MAIL = "mail";

    @Override
    public Map<String, String> getUserEntityByMail(String email) {
        UserService userService = CdiUtil.bean(UserService.class);
        User user = null;

        try {
            user = userService.getUserByAttribute(MAIL, email, true);
        } catch (Exception e) {
            logger.error("Error fetching user by email {}: {}", email, e.getMessage(), e);
        }

        if (user == null) {
            logger.warn("No user found for email: {}", email);
            return null;
        }

        Map<String, String> userMap = new HashMap<>();

        try {
            String uid = user.getAttribute(UID);
            String inum = user.getAttribute(INUM_ATTR);
            String lang = user.getAttribute(LANG);

            userMap.put("uid", uid != null ? uid : "");
            userMap.put("inum", inum != null ? inum : "");
            userMap.put("email", email);
            userMap.put("lang", lang != null ? lang : "en");

        } catch (Exception e) {
            logger.error("Error reading attributes for user {}: {}", email, e.getMessage(), e);
        }

        return userMap;
    }

    @Override
    public boolean sendUsernameEmail(String to, String username, String lang) {
        try {
            ConfigurationService configService = CdiUtil.bean(ConfigurationService.class);
            SmtpConfiguration smtpConfig = configService.getConfiguration().getSmtpConfiguration();

            if (smtpConfig == null) {
                logger.error("SMTP configuration missing.");
                return false;
            }

            String preferredLang = (lang != null && !lang.isEmpty()) ? lang.toLowerCase() : "en";
            Map<String, String> templateData = null;

            switch (preferredLang) {
                case "ar": templateData = EmailUsernameAr.get(username); break;
                case "es": templateData = EmailUsernameEs.get(username); break;
                case "fr": templateData = EmailUsernameFr.get(username); break;
                case "id": templateData = EmailUsernameId.get(username); break;
                case "pt": templateData = EmailUsernamePt.get(username); break;
                default:   templateData = EmailUsernameEn.get(username); break;
            }

            if (templateData == null || !templateData.containsKey("body")) {
                logger.error("No email template found for language: {}", preferredLang);
                return false;
            }

            String subject = templateData.getOrDefault("subject", "Your Username Information");
            String htmlBody = templateData.get("body");

            if (htmlBody == null || htmlBody.isEmpty()) {
                logger.error("Email HTML body is empty for language: {}", preferredLang);
                return false;
            }

            // Plain text version
            String textBody = htmlBody.replaceAll("\\<.*?\\>", "");

            MailService mailService = CdiUtil.bean(MailService.class);

            // FIX — Groovy parser issue solved
            boolean sent = mailService.sendMailSigned(
                smtpConfig.getFromEmailAddress(),
                smtpConfig.getFromName(),
                to,
                null,
                subject,
                textBody,
                htmlBody
            );

            if (sent) {
                logger.info("Username email sent successfully to {}", to);
            } else {
                logger.error("Failed to send username email to {}", to);
            }

            return sent;

        } catch (Exception e) {
            logger.error("Error sending username email: {}", e.getMessage(), e);
            return false;
        }
    }
}
