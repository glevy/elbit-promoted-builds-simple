package hudson.plugins.promoted_builds_simple;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.net.URI;
import java.net.URISyntaxException;
import hudson.model.Hudson;
import java.io.IOException;

/**
 * Configured promotion email notification.
 * @author Gilad Levy
 */
public class PromoteNotification {

    private String host;
    private String emailList;
    private String subject;
    private String body;
    private String level;
    private String buildNum;
    private String buildName;


    public PromoteNotification(String host, String emailList, String subject, String body, String level) {
        this.host = host;
        this.emailList = emailList;
        this.subject = subject;
        this.body = body;
    }

    public PromoteNotification() {}


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public String getBuildNum() {
        return buildNum;
    }

    public void setBuildNum(String buildNum) {
        this.buildNum = buildNum;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    private String getSmtpHost() {
        return Hudson.getInstance().getPlugin(PromotedBuildsSimplePlugin.class).getSmtpHost();
    }

    public void notify(String buildURL, String level) {

        setHost(getSmtpHost());
        setLevel(level);
       
          try {
            
            URI uri = new URI(buildURL);
            String[] segments = uri.getPath().split("/");
            setBuildNum(segments[segments.length-1]); 
            setBuildName(segments[segments.length-2]);
        
            String requestURL = buildURL.concat("api/xml?depth=2&xpath=*/action/environment/promotionEmailList");
            System.out.println("Request URL:");
            System.out.println(requestURL);
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(requestURL);
            HttpResponse response = client.execute(request);
            BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
            String emailList = "";
            String line ="";

          while ((line = rd.readLine()) != null) {
              System.out.println(line);
              if (!line.isEmpty()){
                  emailList = line.substring(line.indexOf('>')+1, line.indexOf('<',line.indexOf('>')));
              }

              System.out.println(emailList);
          }
          setEmailList(emailList);
          setSubject("Build Promotion Notification: " + buildName + ":" + buildNum + " was promoted to " + this.level);
          setBody("You are getting this message because you were added to this Jenkins build promotion notification list by the job owner.\n\n " + buildName + " build number " + buildNum + " was promoted to level: " + level + "\n\n Build URL: " + buildURL);
                    
          System.out.println(subject);
          System.out.println(emailList);
          
              sendMail();
      }
      catch(URISyntaxException | IOException e) {
          e.printStackTrace();
      }
          
    }
      
    public void sendMail() {

        if(!this.emailList.isEmpty() && !this.buildName.isEmpty() && !this.buildNum.isEmpty()){
            try
            {
            Properties props = System.getProperties();
            props.put("mail.smtp.host", this.host);

            Session session = Session.getDefaultInstance(props, null);

            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress("no_reply@jenkins.com", "NoReply-JD"));

            msg.setReplyTo(InternetAddress.parse("no_reply@jenkins.com", false));

            msg.setSubject(this.subject, "UTF-8");

            msg.setText(body, "UTF-8");

            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(this.emailList, false));
            System.out.println("Message is ready");
            Transport.send(msg);  

            System.out.println("EMail Sent Successfully!!");
            }
            catch (Exception e) {
            e.printStackTrace();
            }
        }

    }

}
