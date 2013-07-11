package hudson.plugins.promoted_builds_simple;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Promotion processes defined for a project.
 *
 * <p>
 * TODO: a possible performance problem as every time the owner job is reconfigured,
 * all the promotion processes get reloaded from the disk.
 *
 *
 *
 * @author Kohsuke Kawaguchi
 * @author Yuri Novicow
 * In this form JobPropertyImpl uses only for adding PromoteProjectAction to project.
 *
 */
public final class JobPropertyImpl extends JobProperty<AbstractProject<?, ?>> {

    private final Set<String> activeProcessNames = new HashSet<String>();

    /**
     * Programmatic construction.
     */
    public JobPropertyImpl(AbstractProject<?, ?> owner) throws Descriptor.FormException, IOException {
        this.owner = owner;
    }

    private JobPropertyImpl(StaplerRequest req, JSONObject json) throws Descriptor.FormException, IOException {
        // a hack to get the owning AbstractProject.
        // this is needed here so that we can load items
        List<Ancestor> ancs = req.getAncestors();
        owner = (AbstractProject) ancs.get(ancs.size() - 1).getObject();
        Job project = req.findAncestorObject(Job.class);

    }

    protected void setOwner(AbstractProject<?, ?> owner) {
        super.setOwner(owner);


    }

    /**
     * Gets {@link AbstractProject} that contains us.
     */
    public AbstractProject<?, ?> getOwner() {
        return owner;
    }

    public File getRootDir() {
        return new File(getOwner().getRootDir(), "promotions");
    }

    public void save() throws IOException {
        // there's nothing to save, actually
    }

    public String getUrl() {
        return getOwner().getUrl() + "promotion/";
    }

    public String getFullName() {
        return getOwner().getFullName() + "/promotion";
    }

    public String getFullDisplayName() {
        return getOwner().getFullDisplayName() + " \u00BB promotion";
    }

    public String getUrlChildPrefix() {
        return "";
    }

    public String getDisplayName() {
        return "promotion";
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        //build.addAction(new PromotedBuildAction(build));
        return true;
    }

    @Deprecated
    public Action getJobAction(AbstractProject<?,?> job) {
        return new PromotedProjectAction(job,this);
    }
    
    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public String getDisplayName() {
            return "Promote Builds When...";
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public JobPropertyImpl newInstance(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            try {
                if (json.has("promotions")) {
                    return new JobPropertyImpl(req, json);
                }
                return null;
            } catch (IOException e) {
                throw new FormException("Failed to create", e, null); // TODO:hmm
            }
        }
    }
}
