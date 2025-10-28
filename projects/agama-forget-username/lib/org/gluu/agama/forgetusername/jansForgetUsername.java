package org.gluu.agama.newregistration;

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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.gluu.agama.newuser.NewUserRegistration;
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

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class JansUserRegistration extends NewUserRegistration {

    private static final Logger logger = LoggerFactory.getLogger(JansUserRegistration.class);

    private static final String SN = "sn";
    private static final String CONFIRM_PASSWORD = "confirmPassword";
    private static final String LANG = "lang";
    private static final String REFERRAL_CODE = "referralCode";
    private static final String RESIDENCE_COUNTRY = "residenceCountry";
    private static final String PHONE_NUMBER = "mobile";
    private static final String MAIL = "mail";
    private static final String UID = "uid";
    private static final String DISPLAY_NAME = "displayName";
    private static final String GIVEN_NAME = "givenName";
    private static final String PASSWORD = "userPassword";
    private static final String INUM_ATTR = "inum";
    private static final String EXT_ATTR = "jansExtUid";
    private static final String USER_STATUS = "jansStatus";
    private static final String EXT_UID_PREFIX = "github:";
    private static final String EMAIL_VERIFIED = "emailVerified";
    private static final String PHONE_VERIFIED = "phoneNumberVerified";
    private static final int OTP_LENGTH = 6;
    public static final int OTP_CODE_LENGTH = 6;
    private static final String SUBJECT_TEMPLATE = "Here's your verification code: %s";
    private static final String MSG_TEMPLATE_TEXT = "%s is the code to complete your verification";
    private static final SecureRandom RAND = new SecureRandom();

    private static JansUserRegistration INSTANCE = null;
    private Map<String, String> flowConfig;
    private final Map<String, OTPEntry> emailOtpStore = new HashMap<>();
    private static final Map<String, OTPEntry> userCodes = new HashMap<>();

    // No-arg constructor
    public JansUserRegistration() {
        this.flowConfig = new HashMap<>();
        logger.info("Initialized JansUserRegistration using default constructor (no config).");
    }

    // Constructor used by config
    private JansUserRegistration(Map<String, String> config) {
        this.flowConfig = config;
        logger.info("Using Twilio account SID: {}", config.get("ACCOUNT_SID"));
    }

    // No-arg singleton accessor (required by engine)
    public static synchronized NewUserRegistration getInstance() {
        if (INSTANCE == null) {
            Map<String, String> config = loadTwilioConfig();
            INSTANCE = new JansUserRegistration(config);
        }
        return INSTANCE;
    }

    // Config-based singleton accessor
    public static synchronized NewUserRegistration getInstance(Map<String, String> config) {
        if (INSTANCE == null) {
            INSTANCE = new JansUserRegistration(config);
        }
        return INSTANCE;
    }



    public static class OTPEntry implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String code;
        private long timestamp; // in milliseconds

        // Required no-arg constructor for Agama serialization
        public OTPEntry() {
        }

        // Convenience constructor for easy instantiation
        public OTPEntry(String code) {
            this.code = code;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getCode() { 
            return code; 
        }

        public void setCode(String code) { 
            this.code = code; 
        }

        public long getTimestamp() { 
            return timestamp; 
        }

        public void setTimestamp(long timestamp) { 
            this.timestamp = timestamp; 
        }
    }


    public  Map<String, Object> validateInputs(Map<String, String> profile) {
        LogUtils.log("Validate inputs ");
        Map<String, Object> result = new HashMap<>();

        if (profile.get(UID)== null || !Pattern.matches('''^[A-Za-z][A-Za-z0-9]{5,19}$''', profile.get(UID))) {
            result.put("valid", false);
            result.put("message", "Invalid username. Must be 6-20 characters, start with a letter, and contain only letters, digits");
            return result;
        }
        if (profile.get(PASSWORD)==null || !Pattern.matches('''^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!"#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~])[!-~&&[^ ]]{12,24}$''', profile.get(PASSWORD))) {
            result.put("valid", false);
            result.put("message", "Invalid password. Must be at least 12 to 24 characters with uppercase, lowercase, digit, and special character.");
            return result;
        }

        if(profile.get(LANG)==null||!Pattern.matches('''^(ar|en|es|fr|pt|id)$''',profile.get(LANG)))

        {
            result.put("valid", false);
            result.put("message", "Invalid language code. Must be one of ar, en, es, fr, pt, or id.");
            return result;
        }

        if(profile.get(RESIDENCE_COUNTRY)==null||!Pattern.matches('''^[A-Z]{2}$''',profile.get(RESIDENCE_COUNTRY)))
        {
            result.put("valid", false);
            result.put("message", "Invalid residence country. Must be exactly two uppercase letters.");
            return result;
        }

        if(!profile.get(PASSWORD).equals(profile.get(CONFIRM_PASSWORD)))
        {
            result.put("valid", false);
            result.put("message", "Password and confirm password do not match");
            return result;
        }

        result.put("valid",true);
        result.put("message","All inputs are valid.");
        return result;
        }

    public Map<String, String> getUserEntityByMail(String email) {
        User user = getUser(MAIL, email);
        boolean local = user != null;
        LogUtils.log("There is % local account for %", local ? "a" : "no", email);
    
        if (local) {            
            String uid = getSingleValuedAttr(user, UID);
            String inum = getSingleValuedAttr(user, INUM_ATTR);
            String name = getSingleValuedAttr(user, GIVEN_NAME);
    
            if (name == null) {
                name = getSingleValuedAttr(user, DISPLAY_NAME);
                if (name == null && email != null && email.contains("@")) {
                    name = email.substring(0, email.indexOf("@"));
                }
            }
    
            // Creating a truly modifiable map
            Map<String, String> userMap = new HashMap<>();
            userMap.put(UID, uid);
            userMap.put(INUM_ATTR, inum);
            userMap.put("name", name);
            userMap.put("email", email);
    
            return userMap;
        }
    
        return new HashMap<>();
    }

    public Map<String, String> getUserEntityByUsername(String username) {
        User user = getUser(UID, username);
        boolean local = user != null;
        LogUtils.log("There is % local account for %", local ? "a" : "no", username);
    
        if (local) {
            String email = getSingleValuedAttr(user, MAIL);
            String inum = getSingleValuedAttr(user, INUM_ATTR);
            String name = getSingleValuedAttr(user, GIVEN_NAME);
            String uid = getSingleValuedAttr(user, UID); // Define uid properly
    
            if (name == null) {
                name = getSingleValuedAttr(user, DISPLAY_NAME);
                if (name == null && email != null && email.contains("@")) {
                    name = email.substring(0, email.indexOf("@"));
                }
            }    
            // Creating a modifiable HashMap directly
            Map<String, String> userMap = new HashMap<>();
            userMap.put(UID, uid);
            userMap.put(INUM_ATTR, inum);
            userMap.put("name", name);
            userMap.put("email", email);
    
            return userMap;
        }
    
        return new HashMap<>();
    }

    public String sendEmail(String to, String lang) {
        try {
            ConfigurationService configService = CdiUtil.bean(ConfigurationService.class);
            SmtpConfiguration smtpConfig = configService.getConfiguration().getSmtpConfiguration();

            if (smtpConfig == null) {
                LogUtils.log("SMTP configuration is missing.");
                return null;
            }

            // Preferred language or fallback to English
            String preferredLang = (lang != null && !lang.isEmpty())
                    ? lang.toLowerCase()
                    : "en";

            // Generate OTP
            String otp = IntStream.range(0, OTP_LENGTH)
                    .mapToObj(i -> String.valueOf(RAND.nextInt(10)))
                    .collect(Collectors.joining());

            // Pick localized email template
            Map<String, String> templateData;
            switch (preferredLang) {
                case "ar":
                    templateData = EmailRegistrationOtpAr.get(otp);
                    break;
                case "es":
                    templateData = EmailRegistrationOtpEs.get(otp);
                    break;
                case "fr":
                    templateData = EmailRegistrationOtpFr.get(otp);
                    break;
                case "id":
                    templateData = EmailRegistrationOtpId.get(otp);
                    break;
                case "pt":
                    templateData = EmailRegistrationOtpPt.get(otp);
                    break;
                default:
                    templateData = EmailRegistrationOtpEn.get(otp);
                    break;
            }

            String subject = templateData.get("subject");
            String htmlBody = templateData.get("body");
            String textBody = htmlBody.replaceAll("\\<.*?\\>", ""); // crude strip HTML

            // Send email
            MailService mailService = CdiUtil.bean(MailService.class);
            boolean sent = mailService.sendMailSigned(
                    smtpConfig.getFromEmailAddress(),
                    smtpConfig.getFromName(),
                    to,
                    null,
                    subject,
                    textBody,
                    htmlBody);

            if (sent) {
                LogUtils.log("Localized registration OTP email sent to %", to);

                // Store OTP with timestamp for expiration
                emailOtpStore.put(to, new OTPEntry(otp));

                return otp; // return OTP so you can validate later
            } else {
                LogUtils.log("Failed to send registration OTP email to %", to);
                return null;
            }

        } catch (Exception e) {
            LogUtils.log("Failed to send registration OTP email: %", e.getMessage());
            return null;
        }
    }

    private SmtpConfiguration getSmtpConfiguration() {
        ConfigurationService configurationService = CdiUtil.bean(ConfigurationService.class);
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        return smtpConfiguration;

    }

    public boolean validateEmailOtp(String email, String emailOtp) {
        OTPEntry entry = emailOtpStore.get(email);
        if (entry == null) {
            LogUtils.log("No OTP found for email %", email);
            return false;
        }

        // Check expiration: 10 minutes = 10 * 60 * 1000 ms
        long elapsed = System.currentTimeMillis() - entry.timestamp;
        if (elapsed > 10 * 60 * 1000) {
            LogUtils.log("OTP for email % has expired", email);
            emailOtpStore.remove(email);
            return false;
        }

        if (entry.code.equalsIgnoreCase(emailOtp)) {
            emailOtpStore.remove(email); // remove after successful validation
            return true;
        }
        return false;
    }

    public String sendOTPCode(String phone, String lang, boolean UniqueNumber) {
        try {
            if (!UniqueNumber ) { 
            logger.info("Phone number {} already exists. Skipping OTP send, but returning control to Agama flow.", phone);
            return phone;
            }

            logger.info("Sending OTP Code via SMS to phone: {}", phone);

            String otpCode = generateSMSOTpCode(OTP_CODE_LENGTH);
            logger.info("Generated OTP {} for phone {}", otpCode, phone);

            
            String preferredLang = (lang != null && !lang.isEmpty()) ? lang.toLowerCase() : "en";

            Map<String, String> messages = new HashMap<>();

            messages.put("ar", "رمز التحقق OTP الخاص بك من Phi Wallet هو " + otpCode + ". لا تشاركه مع أي شخص.");
            messages.put("en", "Your Phi Wallet OTP is " + otpCode + ". Do not share it with anyone.");
            messages.put("es", "Tu código de Phi Wallet es " + otpCode + ". No lo compartas con nadie.");
            messages.put("fr", "Votre code Phi Wallet est " + otpCode + ". Ne le partagez avec personne.");
            messages.put("id", "Kode Phi Wallet Anda adalah " + otpCode + ". Jangan bagikan kepada siapa pun.");
            messages.put("pt", "O seu código da Phi Wallet é " + otpCode + ". Não o partilhe com ninguém.");

            String message = messages.getOrDefault(preferredLang, messages.get("en"));

            associateGeneratedCodeToPhone(phone, otpCode);

            sendTwilioSms(phone, message);

            return phone;
        } catch (Exception ex) {
            logger.error("Failed to send OTP to phone: {}. Error: {}", phone, ex.getMessage(), ex);
            return null;
        }
    }

    private String generateSMSOTpCode(int codeLength) {
        String numbers = "0123456789";
        SecureRandom random = new SecureRandom();
        char[] otp = new char[codeLength];
        for (int i = 0; i < codeLength; i++) {
            otp[i] = numbers.charAt(random.nextInt(numbers.length()));
        }
        return new String(otp);
    }

    private boolean associateGeneratedCodeToPhone(String phone, String code) {
        try {
            logger.info("Associating code {} to phone {}", code, phone);
            userCodes.put(phone, new OTPEntry(code));
            logger.info("userCodes map now: {}", userCodes);
            return true;
        } catch (Exception e) {
            logger.error("Error associating OTP code to phone {}. Error: {}", phone, e.getMessage(), e);
            return false;
        }
    }

    private boolean sendTwilioSms(String phone, String message) {
        try {

            PhoneNumber FROM_NUMBER = new com.twilio.type.PhoneNumber(flowConfig.get("FROM_NUMBER"));

            logger.info("FROM_NUMBER", FROM_NUMBER);

            PhoneNumber TO_NUMBER = new com.twilio.type.PhoneNumber(phone);

            logger.info("TO_NUMBER", TO_NUMBER);

            Twilio.init(flowConfig.get("ACCOUNT_SID"), flowConfig.get("AUTH_TOKEN"));

            logger.info(null, flowConfig.get("ACCOUNT_SID"), flowConfig.get("AUTH_TOKEN"));

            Message.creator(TO_NUMBER, FROM_NUMBER, message).create();

            logger.info("OTP code has been successfully send to {} on phone number {} .", phone);

            return true;
        } catch (Exception exception) {
            logger.error("Error sending OTP code to user {} on pone number {} : error {} .", phone,
                    exception.getMessage(), exception);
            return false;
        }
    }

    public boolean validateOTPCode(String phone, String code) {
        try {
            logger.info("Validating OTP code {} for phone {}", code, phone);
            OTPEntry entry = userCodes.get(phone);

            if (entry == null) {
                logger.info("No OTP found for phone {}", phone);
                return false;
            }

            long elapsed = System.currentTimeMillis() - entry.timestamp;
            if (elapsed > 10 * 60 * 1000) { 
                logger.info("OTP for phone {} has expired", phone);
                userCodes.remove(phone);
                return false;
            }

            if (entry.code.equalsIgnoreCase(code)) {
                userCodes.remove(phone);
                return true;
            }

            return false;
        } catch (Exception ex) {
            logger.error("Error validating OTP code {} for phone {}. Error: {}", code, phone, ex.getMessage(), ex);
            return false;
        }
    }

    public String addNewUser(Map<String, String> profile) throws Exception {
        Set<String> attributes = Set.of("uid", "mail", "displayName","givenName", "sn", "userPassword", "lang", "residenceCountry", "referralCode");
        User user = new User();
    
        attributes.forEach(attr -> {
            String val = profile.get(attr);
            if (StringHelper.isNotEmpty(val)) {
                user.setAttribute(attr, val);      
            }
        });

        // defaults
        user.setAttribute("emailVerified", Boolean.TRUE);
        user.setAttribute("phoneNumberVerified", Boolean.FALSE);

        UserService userService = CdiUtil.bean(UserService.class);
        user = userService.addUser(user, true); // Set user status active
    
        if (user == null) {
            throw new EntryNotFoundException("Added user not found");
        }
    
        return getSingleValuedAttr(user, INUM_ATTR);
    }

    public String markPhoneAsVerified(String userName, String phone) {
        try {
            UserService userService = CdiUtil.bean(UserService.class);
            User user = getUser(UID, userName);
            if (user == null) {
                logger.error("User not found for username {}", userName);
                return "User not found.";
            }

            // Just set to true
            user.setAttribute(PHONE_NUMBER, phone);
            user.setAttribute(PHONE_VERIFIED, Boolean.TRUE);

            userService.updateUser(user);
            logger.info("Phone verification set to TRUE for UID {}", userName);
            return "Phone " + phone + " verified successfully for user " + userName;
        } catch (Exception e) {
            logger.error("Error setting phone verified TRUE for UID {}: {}", userName, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    public boolean isPhoneUnique(String username, String phone) {
        try {
            logger.info("=== isPhoneUnique() called for user: {}, phone: {} ===", username, phone);

            // Force CDI bean load here (defensive)
            UserService userService = CdiUtil.bean(UserService.class);
            if (userService == null) {
                logger.error("UserService is NULL in isPhoneUnique()");
                return false;
            }

            String normalizedPhone = phone.startsWith("+") ? phone : "+" + phone;
            logger.info("Normalized phone: {}", normalizedPhone);

            List<User> users = userService.getUsersByAttribute("mobile", normalizedPhone, true, 10);
            logger.info("LDAP search result: {}", users != null ? users.size() : "NULL");

            if (users != null && !users.isEmpty()) {
                for (User u : users) {
                    logger.info("Found user: {}", u.getUserId());
                    if (!u.getUserId().equalsIgnoreCase(username)) {
                        logger.info("Phone {} is NOT unique. Already used by {}", phone, u.getUserId());
                        return false;
                    }
                }
            }

            logger.info("Phone {} is unique", phone);
            return true;

        } catch (Exception e) {
            logger.error("Error checking phone uniqueness for {}: {}", phone, e.getMessage(), e);
            return false;
        }
    }

    private String getSingleValuedAttr(User user, String attribute) {
        Object value = null;
        if (attribute.equals(UID)) {
            // user.getAttribute("uid", true, false) always returns null :(
            value = user.getUserId();
        } else {
            value = user.getAttribute(attribute, true, false);
        }
        return value == null ? null : value.toString();

    }

    private static User getUser(String attributeName, String value) {
        UserService userService = CdiUtil.bean(UserService.class);
        return userService.getUserByAttribute(attributeName, value, true);
    }

    public static Map<String, Object> syncUserWithExternal(String inum, Map<String, String> conf) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Load config using CdiUtil or static ConfigService
            Map<String, String> config = new HashMap<>();
            if (conf == null) {
                result.put("status", "error");
                result.put("message", "Configuration is null");
                return result;
            }

            String publicKey = conf.get("PUBLIC_KEY");
            String privateKey = conf.get("PRIVATE_KEY");

            if (publicKey == null || privateKey == null) {
                result.put("status", "error");
                result.put("message", "PUBLIC_KEY or PRIVATE_KEY missing in config");
                return result;
            }

            // Generate HMAC-SHA256 signature (hex lowercase)
            String signature;
            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                        privateKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "HmacSHA256");
                mac.init(secretKey);
                byte[] hashBytes = mac.doFinal(inum.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (byte b : hashBytes) {
                    String h = Integer.toHexString(0xff & b);
                    if (h.length() == 1)
                        hex.append('0');
                    hex.append(h);
                }
                signature = hex.toString().toLowerCase();
            } catch (Exception ex) {
                result.put("status", "error");
                result.put("message", "Failed to generate signature: " + ex.getMessage());
                return result;
            }

            // Build webhook URL
            String url = String.format("https://api.phiwallet.dev/v1/webhooks/users/%s/sync", inum);

            // HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-AUTH-CLIENT", publicKey)
                    .header("X-HMAC-SIGNATURE", signature)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(String.format("Webhook sync response status: %d, body: %s",
                    response.statusCode(), response.body()));

            if (response.statusCode() == 200) {
                result.put("status", "success");
            } else {
                result.put("status", "error");
                result.put("message", response.body());
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }
}
