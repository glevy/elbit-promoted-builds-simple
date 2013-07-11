package hudson.plugins.promoted_builds_simple;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ProminentProjectAction;

import hudson.model.Run;

import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * For customizing project top-level GUI.
 * @author Kohsuke Kawaguchi
 * @author  Yuri Novicow
 *
 *
 *
 *
 * This class provides model for promotions view page.
 */
@ExportedBean(defaultVisibility = 2)
public class PromotedProjectAction implements ProminentProjectAction {

    public final AbstractProject<?, ?> owner;
    private final JobPropertyImpl property;

    public PromotedProjectAction(AbstractProject<?, ?> owner, JobPropertyImpl property) {
        this.owner = owner;
        this.property = property;
    }


    public String getIconFileName() {
        return "star.gif";
    }

    public String getDisplayName() {
        return "Promotions";
    }

    public String getUrlName() {
        return "promotion";
    }

    /**
     */
    public List<PromotionLevel> getAllPromotionLevels() {
        return Hudson.getInstance().getPlugin(PromotedBuildsSimplePlugin.class).getLevels();
    }

    public String[] getPromotionLevelNames() {
        List <PromotionLevel> levels = getAllPromotionLevels();
        String[] levelNames = new String[levels.size()];
        for (int i = 0; i < levelNames.length; i++) {
            levelNames[i] = levels.get(i).getName();
        }
        return levelNames;
    }


    public List<PromotedBuildWrapper> getPromotedBuilds() {
        List builds = owner.getBuilds();
        List<PromotedBuildWrapper> wrappedBuilds = new ArrayList();
        for (Object build : builds) {
            PromoteAction action = ((Run) build).getAction(PromoteAction.class);
            if (action != null) {
                PromotedBuildWrapper promotedBuild = new PromotedBuildWrapper((Run) build, action.getLevel());
                wrappedBuilds.add(promotedBuild);
            }
        }
        return wrappedBuilds;
    }
    
    public class PromotedBuildWrapper {

        private Run build;
        private String promotedLevel;

        public PromotedBuildWrapper(Run build, String promotedLevel) {
            this.build = build;
            this.promotedLevel = promotedLevel;
        }

        public Run getBuild() {
            return build;
        }

        public String getPromotedLevel() {
            return promotedLevel;
        }
    }
}
