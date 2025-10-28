package org.gluu.agama.senduser;

import java.util.Map;
import org.gluu.agama.forgetusername.jansForgetUsername;

public abstract class SendUsername {

    public abstract Map<String, String> getUserEntityByMail(String email);

    public abstract boolean sendUsernameEmail(String to, String usernam, String lang);

    public static SendUsername getInstance() {
        return jansForgetUsername.getInstance(); // use the singleton
    }
}
