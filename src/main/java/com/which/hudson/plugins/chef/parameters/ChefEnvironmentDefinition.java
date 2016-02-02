package com.which.hudson.plugins.chef.parameters;

import com.which.hudson.plugins.chef.api.ChefApiBuilder;
import com.which.hudson.plugins.chef.credentials.ChefCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.security.ACL;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import net.sf.json.regexp.RegexpMatcher;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.digester.RegexMatcher;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jclouds.chef.ChefApi;
import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;

/**
 * Responsible for populating Chef environments dropdown
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhelyan.panchev
 * Date: 17/10/13
 * Time: 21:44
 * To change this template use File | Settings | File Templates.
 */
public class ChefEnvironmentDefinition extends ParameterDefinition {
    //must not persist as we want the most up to date data
    private String credentialId;
    private List<Regex> excludes;
    private transient List<String> choices;

    @DataBoundConstructor
    public ChefEnvironmentDefinition(String name, String[] choices, String description, String credentialId, List<Regex> excludes) throws IOException, ChefApiBuilder.ConfigurationException {
        super(name, description);
        this.credentialId = credentialId;
        this.excludes = excludes == null ? new ArrayList<Regex>(0) : excludes;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        StringParameterValue value = req.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String choice = req.getParameter(getName());
        return new StringParameterValue(getName(), choice);
    }

    public List<Regex> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<Regex> excludes) {
        this.excludes = excludes;
    }

    /**
     * data provider to the jelly index page
     *
     * @return
     */
    public List<String> getChoices() {
        return getAllEnvironments();
    }

    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public List<String> getAllEnvironments() {
        //TODO figure out how to fire error message instead of returning error as value
        ChefApi chefApi;
        try {
            chefApi = ChefApiBuilder.INSTANCE.build(credentialId);
        } catch (ChefApiBuilder.ConfigurationException e) {
            e.printStackTrace();
            return Arrays.asList(ExceptionUtils.getRootCauseMessage(e));
        }

        List<String> chefEnvs = new ArrayList<String>(chefApi.listEnvironments());
        for (Iterator<String> iterator = chefEnvs.iterator(); iterator.hasNext(); ) {
            String env = iterator.next();
            for (Regex r : excludes) {
                if (env.matches(r.getValue())) {
                    iterator.remove();
                }
            }
        }
        Collections.sort(chefEnvs, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        return chefEnvs;
    }

    @Extension
    public static class DescriptorImpl extends ParameterDefinition.ParameterDescriptor {

        @Override
        public String getHelpFile() {
            return "/help/parameter/chefenvironmentchoice.html";
        }

        public String getDisplayName() {
            return new Localizable(ResourceBundleHolder.get(ChefEnvironmentDefinition.class), "DisplayName").toString();
        }

        public List<ChefCredentials> getCredentials() {
            return (List<ChefCredentials>) CredentialsProvider.lookupCredentials(ChefCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, new LinkedList<DomainRequirement>());
        }

    }
}
