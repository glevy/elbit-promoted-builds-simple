package hudson.plugins.promoted_builds_simple;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Date;
import java.util.List;

import org.acegisecurity.AccessDeniedException;

import hudson.model.Job;
import hudson.model.ParameterValue;
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
        if (emailList != null)
            return emailList;
            else return "";
    }

    public void setEmailList(String emailList) {
        this.emailList = emailList;
    }

    public String getSubject() {
        if (subject != null)
           return subject;
            else return "";
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        if (body != null)
            return body;
            else return "";
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLevel() {
        if (level != null)
            return level;
            else return "";
    }

    public void setLevel(String level) {
        this.level = level;
    }

  
    public void notify(String buildURL, String buildName, Integer buildNum, String level) {

        setLevel(level);
            try {
            ParametersAction pa = Jenkins.getInstance().getItemByFullName(buildName, Job.class).getBuildByNumber(buildNum).getAction(ParametersAction.class);
            if (pa != null){
                List<ParameterValue> params = pa.getParameters();
                if(params.size() > 0){
                    for (ParameterValue p : params) {
                        if (p == null) continue;
                        System.out.println(p.getName());
                        if (p.getName().equals("Promotion notification list")) {
                            setEmailList(String.valueOf(p.getValue()));
                            System.out.println(getEmailList());
                        }
                        if (p.getName().equals("Promotion notification Subject")) {
                            setSubject(String.valueOf(p.getValue()));
                            System.out.println(getSubject());
                        }
                        if (p.getName().equals("Promotion notification Body")) {
                            setBody(String.valueOf(p.getValue()));
                            System.out.println(getBody());
                        }
                    } 
                }
            }
            
            if (!getEmailList().isEmpty()){
                if (getSubject().isEmpty()){
                    setSubject(
                            "Build Promotion Notification: " + buildName + ":" + buildNum + " was promoted to " + this.level);
                }
                if (getBody().isEmpty()){
                    setBody("You are getting this message because you were added to this Jenkins build promotion notification list by the job owner.\n\n "
                            + buildName + " build number " + buildNum + " was promoted to level: " + level + "\n\n Build URL: "
                            + buildURL);
                }

                System.out.println(subject);

                sendMail();
            } else{ System.out.println("Promotion notification list is empty.\nPlease make sure a job string parameter by this name exists and is not empty.");}
        } catch (AccessDeniedException e) {
          e.printStackTrace();
      }
          
    }
      
    public void sendMail() {

        if(!getEmailList().isEmpty() && !getSubject().isEmpty() && !getBody().isEmpty()){
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
