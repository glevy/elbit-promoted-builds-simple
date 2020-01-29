/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Alan Harder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.promoted_builds_simple;

import hudson.PluginWrapper;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.Job;
import hudson.model.Run;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Date;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

/**
 * Store promotion level for a build.
 * @author Alan.Harder@sun.com
 * @author gilad.levy@elbitsystems.com
 */
@ExportedBean(defaultVisibility = 2)
public class PromoteAction implements BuildBadgeAction {

    private String level, icon;
    private int levelValue;

    public PromoteAction() { }


    private static void sendMail(String host, String toEmail, String subject, String body) {
        try
	    {
          Properties props = System.getProperties();
          props.put("mail.smtp.host", host);
          Session session = Session.getDefaultInstance(props, null);

          MimeMessage msg = new MimeMessage(session);
	      //set message headers
	      msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
	      msg.addHeader("format", "flowed");
	      msg.addHeader("Content-Transfer-Encoding", "8bit");

	      msg.setFrom(new InternetAddress("no_reply@jenkins.com", "NoReply-JD"));

	      msg.setReplyTo(InternetAddress.parse("no_reply@jenkins.com", false));

	      msg.setSubject(subject, "UTF-8");

	      msg.setText(body, "UTF-8");

	      msg.setSentDate(new Date());

	      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
	      System.out.println("Message is ready");
    	  Transport.send(msg);  

	      System.out.println("EMail Sent Successfully!!");
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }

    }

    /* Action methods */
    public String getUrlName() { return "epromote"; }
    public String getDisplayName() { return ""; }
    public String getIconFileName() { return null; }

    /* Promotion details */
    @Exported public String getLevel() { return level; }
    @Exported public int getLevelValue() { return levelValue; }
    public String getIconPath() {
        if (icon == null || icon.startsWith("/")) return icon;
        // Try plugin images dir, fallback to main images dir
        PluginWrapper wrapper =
            Hudson.getInstance().getPluginManager().getPlugin(PromotedBuildsSimplePlugin.class);
        return new File(wrapper.baseResourceURL.getPath() + "/images/" + icon).exists()
            ? "/plugin/" + wrapper.getShortName() + "/images/" + icon
            : Hudson.RESOURCE_PATH + "/images/16x16/" + icon;
    }
    public static List<PromotionLevel> getAllPromotionLevels() {
        return Hudson.getInstance().getPlugin(PromotedBuildsSimplePlugin.class).getLevels();
    }

    /** Save change to promotion level for this build and redirect back to build page
     *  Also called methods to creates/delete symlinks.
     */
    public void doIndex(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        List<Ancestor> ancs = req.getAncestors();
        AbstractProject owner = req.findAncestorObject(AbstractProject.class);

        Job project = req.findAncestorObject(Job.class);
        project.checkPermission(Run.UPDATE);


        levelValue = Integer.parseInt(req.getParameter("level"));
        if (levelValue == 0) {
            level = icon = null;
            req.findAncestorObject(Run.class).save();
        } else {
            PromotionLevel src = getAllPromotionLevels().get(levelValue - 1);
            level = src.getName();

            icon = src.getIcon();

            // Mark as keep-forever when promoting; this also does save()
            
            if (src.isAutoKeep())
            req.findAncestorObject(Run.class).keepLog(true);
            else
            req.findAncestorObject(Run.class).save();
            
                    
                if (src.isPromoteArtifacts())
                  rsp.sendRedirect2("../promote");

                if (src.isEnableNotification()){
                    String emailList = req.findAncestorObject(Run.class).getBuildVariables().get('promotionEmailList')
                    String jobName = req.findAncestorObject(Run.class).getBuildVariables().get('JOB_BASE_NAME')
                    String buildNumber = req.findAncestorObject(Run.class).getBuildVariables().get('BUILD_NUMBER')
                    String subject = "$jobName:$buildNumber was promoted to $level"
                    String body = req.findAncestorObject(Run.class).getBuildVariables().get('promotionEmailBody')
                    String host = 
                    sendMail(host, emailList, subject, body)
                }

        }
       
        rsp.forwardToPreviousPage(req);
    }

}
