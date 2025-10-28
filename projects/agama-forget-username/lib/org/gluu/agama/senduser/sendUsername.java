package org.gluu.agama.senduser;

import java.util.Map;
import org.gluu.agama.forgetusername.jansForgetUsername;

public abstract class forgetusername {

  
    public abstract Map<String, String> getUserEntityByMail(String mail);

    
    public abstract boolean sendUsernameEmail(String to, String userName, String lang);

    /**
     * Factory method to return an instance of jansForgetUsername.
     */
    public static jansForgetUsername getInstance() {
        return new jansForgetUsername();
    }
}
