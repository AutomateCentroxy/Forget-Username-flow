package org.gluu.agama.usernameclass;

import java.util.Map;
import org.gluu.agama.forgetusername.jansForgetUsername;

public abstract class usernameResendclass {

    public abstract Map<String, String> getUserEntityByMail(String email);

    public abstract boolean sendUsernameEmail(String to, String usernam, String lang);

    public static usernameResendclass getInstance() {
        return new jansForgetUsername();
    }
}
