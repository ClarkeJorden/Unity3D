package ZoneExtension;

import java.sql.SQLException;

import javax.mail.MessagingException;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Email;
import com.smartfoxserver.v2.entities.SFSEmail;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.managers.IMailerService;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class ResetPasswordHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT password FROM user WHERE email=\"" + params.getUtfString("email") + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if(res.size() == 0) {
				params.putBool("success", false);
			}
			else {
				params.putBool("success", true);
				SendMail(params.getUtfString("email"), "Forgot Password", "Your password is : " + res.getSFSObject(0).getUtfString("password"));
			}
			send("reset_password", params, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
	
	public void SendMail(String email, String title, String msg)
	{
		IMailerService mailService = SmartFoxServer.getInstance().getMailService();
		Email em = new SFSEmail("instagame@service.com", email, title, msg);
		try {
		   mailService.sendMail(em);
		   trace(em.getFromAddress()+ " is sending Mail to "+em.getToAddress());
		} catch(MessagingException e)
		{
		   trace("mail send exception - "+e.getMessage());
		}
	}

}
