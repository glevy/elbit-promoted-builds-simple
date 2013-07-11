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

/**
 * Store promotion level for a build.
 * @author Alan.Harder@sun.com
 * @author yurin@tikalk.com
 */
@ExportedBean(defaultVisibility = 2)
public class PromoteAction implements BuildBadgeAction {

    private String level, icon;
    private int levelValue;

    public PromoteAction() { }

    /* Action methods */
    public String getUrlName() { return "promote"; }
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


        File promotionsDirectory = new File(owner.getRootDir(), "promotions");
        promotionsDirectory.mkdirs();
        List<PromotionLevel> levels = getAllPromotionLevels();
        for (PromotionLevel promotionLevel : levels) {
            File promotionLevelDirectory = new File(promotionsDirectory.getAbsolutePath(), promotionLevel.getName().replaceAll(" ", "_"));
            promotionLevelDirectory.mkdir();
        }
        File[] levelDirectories = promotionsDirectory.listFiles();



        levelValue = Integer.parseInt(req.getParameter("level"));
        if (levelValue == 0) {
            level = icon = null;
            Run currentBuild = req.findAncestorObject(Run.class);
            currentBuild.save();
            deleteOldLink(levelDirectories, currentBuild);
        } else {
            PromotionLevel src = getAllPromotionLevels().get(levelValue - 1);
            level = src.getName();

            icon = src.getIcon();

            // Mark as keep-forever when promoting; this also does save()

            Run currentBuild = req.findAncestorObject(Run.class);
            
            if (src.isAutoKeep())
                currentBuild.keepLog(true);
            else
                currentBuild.save();

            PromoteAction promoteAction = currentBuild.getAction(PromoteAction.class);

            for (int i = 0; i < levelDirectories.length; i++) {
                //create new link
                if (promoteAction.getLevel().replaceAll(" ", "_").equalsIgnoreCase(levelDirectories[i].getName())) {
                    File buildDirectory = currentBuild.getRootDir();
                    createLinkToBuildDirectory(buildDirectory, levelDirectories[i], currentBuild.number);
                }

            }

            deleteOldLink(levelDirectories, currentBuild);            
           
        }
        rsp.forwardToPreviousPage(req);
    }
    /** Called when already promoted build promoted to another promotion level.
     * This cause to change filestystem structure -- old symlink is deleted.
     *
     *
     */
    private void deleteOldLink(File[] levelDirectories, Run build) {
        for (File levelDirectory : levelDirectories) {
            File[] links = levelDirectory.listFiles();
            for (File link : links) {
                String levelName = build.getAction(PromoteAction.class).getLevel();
                String linkName = link.getName();
                String levelDirectoryName = levelDirectory.getName();
                if ((build.number + "").equalsIgnoreCase(linkName) && (levelName == null || !levelName.equalsIgnoreCase(levelDirectoryName.replaceAll("_", " ")))) {
                    link.delete();
                    return;
                }
            }
        }
    }

    /**Creates symlink on Linux/Unix? os. On Windows does nothing.
     */
    private void createLinkToBuildDirectory(File targetBuildDirectory, File promotionDirectory, int targetBuildNumber) throws IOException {
        if (Functions.isWindows()){
            return;
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            String command = "ln " + "-s " + targetBuildDirectory.getAbsolutePath() + " " + promotionDirectory.getAbsolutePath() + "/" + targetBuildNumber;
            Process process = runtime.exec(command);
            int exitValue = process.waitFor();
            System.out.println("ExitValue: " + exitValue);
            return ;
        } catch (InterruptedException ex) {
            Logger.getLogger(PromoteAction.class.getName()).log(Level.SEVERE, null, ex);
            return ;
        }
    }
}
