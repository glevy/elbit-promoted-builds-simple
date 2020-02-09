package hudson.plugins.promoted_builds_simple;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Date;


import org.acegisecurity.AccessDeniedException;

import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.tasks.Mailer;

import jenkins.model.Jenkins;

/**
 * Configured promotion email notification.
 * 
 * @author Gilad Levy
 */
public class PromoteNotification {

    private String emailList;
    private String subject;
    private String body;
    private String level;

    public PromoteNotification(String emailList, String subject, String body, String level) {
        this.emailList = emailList;
        this.subject = subject;
        this.body = body;
    }

    public PromoteNotification() {
    }

    public String getEmailList() {
        return emailList;
    }

    public void setEmailList(String emailList) {
        this.emailList = emailList;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

  
    public void notify(String buildURL, String buildName, Integer buildNum, String level) {

        setLevel(level);
 
        try {

           
            String emailList = String.valueOf(Jenkins.getInstance().getItemByFullName(buildName, Job.class).getBuildByNumber(buildNum)
                    .getAction(ParametersAction.class).getParameter("Promotion notification list").getValue());
            
            System.out.println(emailList);
            setEmailList(emailList);
            setSubject(
                    "Build Promotion Notification: " + buildName + ":" + buildNum + " was promoted to " + this.level);
            setBody("You are getting this message because you were added to this Jenkins build promotion notification list by the job owner.\n\n "
                    + buildName + " build number " + buildNum + " was promoted to level: " + level + "\n\n Build URL: "
                    + buildURL);

            System.out.println(subject);

            
            if (!this.emailList.isEmpty()){
                sendMail();
            } else{System.out.println("Promotion notification list is empty.\nPlease make sure a job string parameter by this name exists and is not empty.");}
        } catch (AccessDeniedException e) {
          e.printStackTrace();
      }
          
    }
      
    public void sendMail() {

        if(!this.emailList.isEmpty() && !this.subject.isEmpty() && !this.body.isEmpty()){
            try
            {
                InternetAddress sender = new InternetAddress(Mailer.descriptor().getAdminAddress());
                String rt = String.valueOf(sender);
                MimeMessage msg = new MimeMessage(Mailer.descriptor().createSession());
                msg.setSubject(this.subject);
                msg.setText(this.body, "utf-8");
                msg.setFrom(sender);
                msg.setReplyTo(InternetAddress.parse(rt, false));
                msg.setSentDate(new Date());
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(this.emailList, false));
                Transport.send(msg);     

            System.out.println("EMail Sent Successfully!!");
            }
            catch (Exception e) {
            e.printStackTrace();
            }
        }

    }

}
