package org.gluu.agama.forgetusername;

import io.jans.agama.engine.service.FlowService;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.model.SmtpConfiguration;
import io.jans.service.MailService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.common.service.common.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.agama.smtp.*;
import org.gluu.agama.usernameclass.UsernameResendclass;

import java.util.HashMap;
import java.util.Map;

public class JansForgetUsername extends UsernameResendclass {   // ✅ FIX HERE

    private static final Logger logger = LoggerFactory.getLogger(FlowService.class);

    private static final String UID = "uid";
    private static final String INUM_ATTR = "inum";
    private static final String LANG = "lang";
    private static final String MAIL = "mail";

    public JansForgetUsername() {
    }

    @Override
    public Map<String, String> getUserEntityByMail(String email) {
        UserService userService = CdiUtil.bean(UserService.class);
        User user = null;

        try {
            user = userService.getUserByAttribute(MAIL, email, true);
        } catch (Exception e) {
            logger.error("Error fetching user by email {}: {}", email, e.getMessage());
        }

        if (user == null) {
            logger.warn("No user found for email: {}", email);
            return null;
        }

        Map<String, String> userMap = new HashMap<>();
        
        userMap.put("uid", user.getAttribute(UID));
        userMap.put("inum", user.getAttribute(INUM_ATTR));
        userMap.put("email", email);
        userMap.put("lang", user.getAttribute(LANG));

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

            Map<String, String> templateData;
            switch (preferredLang) {
                case "ar": templateData = EmailUsernameAr.get(username); break;
                case "es": templateData = EmailUsernameEs.get(username); break;
                case "fr": templateData = EmailUsernameFr.get(username); break;
                case "id": templateData = EmailUsernameId.get(username); break;
                case "pt": templateData = EmailUsernamePt.get(username); break;
                default:   templateData = EmailUsernameEn.get(username); break;
            }

            String subject = templateData.get("subject");
            String htmlBody = templateData.get("body");
            String textBody = htmlBody.replaceAll("\\<.*?\\>", "");

            MailService mailService = CdiUtil.bean(MailService.class);
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
                logger.info("✅ Username email sent successfully to {}", to);
            } else {
                logger.error("❌ Failed to send username email to {}", to);
            }

            return sent;

        } catch (Exception e) {
            logger.error("Error sending username email: {}", e.getMessage(), e);
            return false;
        }
    }

}
