package org.protobj.mock.module.mail;

import com.guangyu.cd003.projects.mock.net.MockConnect;

public class MailController {


    public static void handleMail(MockConnect connect) {
        connect.MAIL_DATA.mailState.execute(connect);
    }
}
