package hudson.plugins.promoted_builds_simple;

import javax.mail.*;
import javax.mail.internet.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        setBody(body);
    }

    public PromoteNotification() {
    }

    public String getEmailList() {
        if (this.emailList != null)
            return this.emailList;
        else
            return "";
    }

    public void setEmailList(String emailList) {
        this.emailList = emailList;
    }

    public String getSubject() {
        if (this.subject != null)
            return this.subject;
        else
            return "";
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        if (this.body != null)
            return this.body;
        else
            return "";
    }

    public void setBody(String body) {
         if (body != null) {
             this.body = body;
        }
    }

    public String getLevel() {
        if (level != null)
            return level;
        else
            return "";
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void notify(String buildURL, String buildName, Integer buildNum, String level) {

        setLevel(level);
        try {

            Map<String, String> env = Jenkins.getInstance().getItemByFullName(buildName, Job.class)
                    .getBuildByNumber(buildNum).getEnvironment( );
            System.out.println(env);
            // static EnvActionImpl env
            // forRun(Jenkins.getInstance().getItemByFullName(buildName,
            // Job.class).getBuildByNumber(buildNum));
            ParametersAction pa = Jenkins.getInstance().getItemByFullName(buildName, Job.class)
                    .getBuildByNumber(buildNum).getAction(ParametersAction.class);
            if (pa != null) {
                List<ParameterValue> params = pa.getParameters();
                if (params.size() > 0) {
                    for (ParameterValue p : params) {
                        if (p == null)
                            continue;
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
                            String emailBody = String.valueOf(p.getValue());
                            if (emailBody != null) {
                                emailBody = emailBody.replaceAll(Pattern.quote("!!BUILD_NUMBER"), String.valueOf(buildNum));
                                emailBody = emailBody.replaceAll(Pattern.quote("!!JOB_NAME"), buildName);
                                emailBody = emailBody.replaceAll(Pattern.quote("!!BUILD_URL"), buildURL);
                                emailBody = emailBody.replaceAll(Pattern.quote("!!PROMOTION_LVL"), this.level);
                                setBody(emailBody);
                                System.out.println(getBody());
                            }
                        }
                    }
                }
            }

            if (!getEmailList().isEmpty()) {
                if (getSubject().isEmpty()) {
                    setSubject("Build Promotion Notification: " + buildName + ":" + buildNum + " was promoted to "
                            + this.level);
                }
                if (getBody().isEmpty()) {
                    setBody("<div><p>You are getting this message because you were added to this Jenkins build promotion notification list by the job owner.</p><p><strong>"
                            + buildName + "</strong> build number <strong>" + buildNum
                            + "</strong> was promoted to level: <strong>" + level + "</strong></p><p>Build URL: " + buildURL
                            + "</p></div>");

                    // setBody("You are getting this message because you were added to this Jenkins
                    // build promotion notification list by the job owner.\n\n "
                    // + buildName + " build number " + buildNum + " was promoted to level: " +
                    // level + "\n\n Build URL: "
                    // + buildURL);
                }

                System.out.println(subject);

                sendMail();
            } else {
                System.out.println(
                        "Promotion notification list is empty.\nPlease make sure a job string parameter by this name exists and is not empty.");
            }
        } catch (AccessDeniedException | IOException | InterruptedException e) {
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
                msg.setSubject(getSubject());
                Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
                boolean isHTML = htmlPattern.matcher(getBody()).matches();
                if(isHTML){
                    System.out.println("HTML body was identified");
                    msg.setContent(getBody(), "text/html");
                }
                else{
                    System.out.println("text body was identified");
                    msg.setText(getBody(), "utf-8");
                }
                msg.setFrom(sender);
                msg.setReplyTo(InternetAddress.parse(rt, false));
                msg.setSentDate(new Date());
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(getEmailList(), false));
                Transport.send(msg);     

            System.out.println("EMail Sent Successfully!!");
            }
            catch (Exception e) {
            e.printStackTrace();
            }
        }

    }

}
