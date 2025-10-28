package org.gluu.agama.senduser;

import java.util.Map;
import org.gluu.agama.forgetusernam.JansUserRegistration;

public abstract class forgetusernam {

    public abstract Map<String, String> getUserEntityByMail(String mail);

    public abstract boolean sendUsernameEmail(String to, String userName, String lang);

    public static JansUserRegistration getInstance() {
        return new JansUserRegistration();
    }
}
