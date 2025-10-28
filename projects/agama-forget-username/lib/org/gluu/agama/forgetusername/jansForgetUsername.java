package org.gluu.agama.forgetusernam;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.UserService;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.service.MailService;
import io.jans.model.SmtpConfiguration;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.gluu.agama.senduser.sendUsername;
import io.jans.agama.engine.script.LogUtils;
import java.io.IOException;
import io.jans.as.common.service.common.ConfigurationService;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.regex.Pattern;

// import org.gluu.agama.EmailTemplate;
// import org.gluu.agama.registration.Labels;
import org.gluu.agama.smtp.*;


public class JansUserRegistration {

    private static final Logger logger = LoggerFactory.getLogger(JansUserRegistration.class);
    private static final String MAIL = "mail";


    public Map<String, String> getUserEntityByMail(String email) {
    User user = getUser(MAIL, email);
    if (user == null) return null;

    Map<String, String> userMap = new HashMap<>();
    userMap.put("uid", getSingleValuedAttr(user, UID));
    userMap.put("inum", getSingleValuedAttr(user, INUM_ATTR));
    userMap.put("name", getSingleValuedAttr(user, GIVEN_NAME));
    userMap.put("email", email);
    userMap.put("lang", lang );

    return userMap;
}

public boolean sendUsernameEmail(String to, String username, String lang) {
    try {
        ConfigurationService configService = CdiUtil.bean(ConfigurationService.class);
        SmtpConfiguration smtpConfig = configService.getConfiguration().getSmtpConfiguration();

        if (smtpConfig == null) {
            logger.error("SMTP configuration missing.");
            return false;
        }

        // Normalize language
        String preferredLang = (lang != null && !lang.isEmpty()) ? lang.toLowerCase() : "en";

        // Select localized email template
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

        if (sent)
            logger.info("Username email sent successfully to {}", to);
        else
            logger.error("Failed to send username email to {}", to);

        return sent;

    } catch (Exception e) {
        logger.error("Error sending username email: {}", e.getMessage(), e);
        return false;
    }
}



        } catch (Exception e) {
            logger.error("Error sending username email: {}", e.getMessage(), e);
            return false;
        }
    }
}



