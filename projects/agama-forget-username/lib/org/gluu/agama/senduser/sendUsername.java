package org.gluu.agama.senduser;

import java.util.Map;
import org.gluu.agama.forgetusername.JansForgetUsername;

public abstract class SendUsername {

    public abstract Map<String, String> getUserEntityByMail(String mail);

    public abstract boolean sendUsernameEmail(String to, String userName, String lang);

    public static SendUsername getInstance() {
        return JansForgetUsername.getInstance(); // use the singleton
    }
}
