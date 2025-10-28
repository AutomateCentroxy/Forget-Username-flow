package org.gluu.agama.senduser;

import java.util.Map;
import org.gluu.agama.forgetusername.jansForgetUsername;

public abstract class sendUsername {

    public abstract Map<String, String> getUserEntityByMail(String email);

    public abstract boolean sendUsernameEmail(String to, String usernam, String lang);

    public static sendUsername getInstance() {
        return new jansForgetUsername();
    }
}
