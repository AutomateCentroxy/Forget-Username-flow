package org.gluu.agama.senduse.senduser;

import java.util.HashMap;
import java.util.Map;

import org.gluu.agama.forgetusernam;



public abstract class sendUsernam {
    public abstract Map<String, Object> validateInputs(Map<String, String> profile);

    public abstract String addNewUser(Map<String, String> profile) throws Exception;

    // public abstract boolean usernamePolicyMatch(String userName);

    // public abstract boolean passwordPolicyMatch(String userPassword);

    public abstract String sendEmail(String to, String lang);

    public abstract String sendOTPCode(String phone, String lang, boolean UniqueNumber);

    public abstract boolean validateOTPCode(String phone, String code);

    public abstract String markPhoneAsVerified(String userName, String phone);

    public abstract boolean isPhoneUnique(String userName, String phone);

    public abstract boolean validateEmailOtp(String email, String emailOtp);

    public abstract boolean sendRegSuccessEmail(String to, String userName, String lang);



    
    public static NewUserRegistration getInstance(HashMap config) {
        return new JansUserRegistration(config);
    }
}
