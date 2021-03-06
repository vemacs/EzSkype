package in.kyle.ezskypeezlife.api.captcha;

/**
 * Created by Kyle on 11/5/2015.
 */
public interface SkypeErrorHandler {
    
    /**
     * Returns a result to the given captcha
     *
     * @param skypeCaptcha - The captcha to sole
     * @return - The solution to the captcha
     */
    String solve(SkypeCaptcha skypeCaptcha);
    
    /**
     * Sets a new password for the user, this is called when Skype requires a user to change their password
     * If you do not want to change your password, return an empty string or null
     *
     * @return - The new Skype password
     */
    String setNewPassword();
}
