
package com.which.hudson.plugins.chef.buildwrapper;

import com.google.common.collect.Lists;
import com.which.hudson.plugins.chef.DeployableArtifact;
import com.which.hudson.plugins.chef.Deployment;
import com.which.hudson.plugins.chef.api.ChefApiBuilder;
import com.which.hudson.plugins.chef.chefreports.ChefReportBuildAction;
import com.which.hudson.plugins.chef.chefreports.ChefReportProjectAction;
import com.which.hudson.plugins.chef.credentials.ChefCredentials;
import com.which.hudson.plugins.chef.util.DatabagRetriever;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import jenkins.model.CauseOfInterruption;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.domain.DatabagItem;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;


// TODO convert the input artifact field to: myapp:${BUILD_ID}.0.0.zip, otherapp:foobar-${BUILD_ID}
// or similar syntax

public class UpdateDatabagItem extends BuildWrapper {
    public static final String STATUS = "status";
    public static final String ARTIFACT = "artifact";
    public static final String REPORTDIR = "chef_report";
    private final String artifactVars;
    private final String envVar;
    private String databag;
    private String databagItem;
    private final String credentialId;

    @DataBoundConstructor
    public UpdateDatabagItem(String artifactVars, String envVar, String credentialId, String databag, String databagItem) {
        this.artifactVars = artifactVars;
        this.envVar = envVar;
        this.databag = databag;
        this.databagItem = databagItem;
        this.credentialId = credentialId;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public Action getProjectAction(AbstractProject job) {
        return new ChefReportProjectAction(new WeakReference<AbstractProject>(job));
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        build.getEnvironment(listener).overrideAll(build.getBuildVariables());

        String environment = null;
        try {
            environment = TokenMacro.expandAll(build, listener, envVar);
        } catch (MacroEvaluationException e) {
            failOnParse(listener, e, envVar);
        }

        String databagItemValue = null;
        try {
            databagItemValue = TokenMacro.expandAll(build, listener, databagItem);
        } catch (MacroEvaluationException e) {
            failOnParse(listener, e, databagItem);
        }

        List<DeployableArtifact> artifacts = null;
        try {
            artifacts = getDeployableArtifacts(build, listener);
        } catch (MacroEvaluationException e) {
            e.printStackTrace();
            failOnParse(listener, e, "(" + StringUtils.join(artifacts, ",") + ")");
        }
        final Deployment deployment = new Deployment();

        deployment.setArtifacts(artifacts);
        deployment.setBuildNumber(build.number);
        deployment.setEnvironment(environment);
        deployment.setStatus(evaluateResult(build.getResult()));
        deployment.setStarted(formattedTimestamp(build.getStartTimeInMillis()));
        final ChefReportBuildAction buildAction = new ChefReportBuildAction();
        buildAction.setDeployment(deployment);
        build.addAction(buildAction);
        try {
            deployment.setStarted(formattedTimestamp(build.getStartTimeInMillis()));
            saveToDataBagItem(databagItemValue, deployment, true);
            listener.getLogger().printf("[chef-buildwrapper] package version(s) saved to data bag item %s/%s\n", databag, databagItemValue);
        } catch (ChefApiBuilder.ConfigurationException e) {
            listener.getLogger().printf("[chef-buildwrapper] (set-up) failed to update data bag item '%s/%s'\n", databag, databagItemValue);
            listener.getLogger().printf("[chef-buildwrapper] Error:: '%s'\n", ExceptionUtils.getRootCauseMessage(e));
            build.getExecutor().interrupt(Result.FAILURE);
        }
        final String newDatabagItemValue = databagItemValue;
        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                try {
                    long finished = new Date().getTime();
                    deployment.setFinished(formattedTimestamp(finished));
                    deployment.setDuration(Util.getPastTimeString(finished - build.getStartTimeInMillis()));
                    deployment.setStatus(evaluateResult(build.getResult()));
                    buildAction.setDeployment(deployment);
                    build.replaceAction(buildAction);
                    saveToDataBagItem(newDatabagItemValue, deployment, false);
                    listener.getLogger().printf("[chef-buildwrapper] build status saved to data bag item %s/%s\n", databag, newDatabagItemValue);
                } catch (ChefApiBuilder.ConfigurationException e) {
                    listener.getLogger().printf("[chef-buildwrapper] (tear-down) failed to update data bag item '%s/%s'\n", databag, newDatabagItemValue);
                    listener.getLogger().printf("[chef-buildwrapper] Error:: '%s'\n", ExceptionUtils.getRootCauseMessage(e));
                    // ignore
                }

                return true;

            }
        };
    }

    private void failOnParse(BuildListener listener, Exception e, String envVar) throws InterruptedException {
        e.printStackTrace();
        String msg = String.format("[chef-buildwrapper] Cannot evaluate %s: %s ", envVar, e.getMessage());
        listener.getLogger().print(msg);
        throw new InterruptedException(msg);
    }

    private String formattedTimestamp(long startTimeInMillis) {
        return DateFormatUtils.format(new Date(startTimeInMillis), "dd-MM-yyyy HH:mm:ss");
    }


    private HashMap<String, String> pairEntry(String key, String val) {
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(key, val);
        return result;
    }

    private String evaluateResult(Result result) {
        return result == null ? "SUCCESS" : result.toString();
    }

    private void saveToDataBagItem(String databagItem, Deployment deployment, boolean cleanup) throws ChefApiBuilder.ConfigurationException, IOException, InterruptedException {
        ChefApi chefApi = ChefApiBuilder.build(credentialId);
        DatabagItem dbi = chefApi.getDatabagItem(databag, databagItem);
        if(dbi == null){
            throw new ChefApiBuilder.ConfigurationException(String.format("Data Bag Item %s/%s not found!", databag, databagItem ));
        }
        JSONObject obj = JSONObject.fromObject(dbi.toString());
        String key = deployment.getEnvironment();
        if (cleanup) {
            obj.discard(key);
            obj.put(key, new JSONObject());
        }
        JSONObject oldData = (JSONObject) obj.get(key);
        oldData.putAll(JSONObject.fromObject(deployment));
        obj.put(key, oldData);
        dbi = new DatabagItem(databagItem, obj.toString());
        chefApi.updateDatabagItem(databag, dbi);
    }

    public String getArtifactVars() {
        return artifactVars;
    }

    public String getEnvVar() {
        return envVar;
    }


    public String getDatabag() {
        return databag;
    }

    public String getDatabagItem() {
        return databagItem;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public List<DeployableArtifact> getDeployableArtifacts(AbstractBuild build, BuildListener listener) throws IOException, MacroEvaluationException, InterruptedException {
        List<String> artifactParameters = Arrays.asList(artifactVars.trim().split("\\s*,\\s*"));
        List<DeployableArtifact> artifacts = Lists.newArrayList();

        for (String artifactName : artifactParameters) {
            String version = TokenMacro.expandAll(build, listener, artifactName);

            artifacts.add(new DeployableArtifact(artifactName.replaceAll("\\$|\\{|\\}", ""), version));
        }

        return artifacts;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        /**
         * validates job config > artifact var name
         *
         * @param artifactVars
         * @return
         */
        public FormValidation doCheckArtifactVars(@QueryParameter String artifactVars) {
            if (artifactVars == null || artifactVars.isEmpty()){
                return FormValidation.error("Value(s) missing");
            }
            if (artifactVars.contains(" ")) {
                //comma separated list
                if (artifactVars.split(",").length > 1) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error("Comma separated list; e.g: ARTIFACT1, myapp-$app_version");
                }

            }
            return FormValidation.ok();
        }


        /**
         * validates job config > env var name
         *
         * @param envVar
         * @return
         */
        public FormValidation doCheckEnvVar(@QueryParameter String envVar) {
            if (!StringUtils.isEmpty(envVar)) return FormValidation.ok();
            else return FormValidation.error("Specify value");
        }

        @Override
        public String getDisplayName() {
            return new Localizable(ResourceBundleHolder.get(UpdateDatabagItem.class), "DisplayName").toString();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }


        public List<ChefCredentials> getCredentials() {
            return (List<ChefCredentials>) CredentialsProvider.lookupCredentials(ChefCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, new LinkedList<DomainRequirement>());
        }

        public FormValidation doCheckCredentialId(@QueryParameter("credentialId") String credentialId) {
            return FormValidation.validateRequired(credentialId);
        }


        @JavaScriptMethod
        public JSONObject getDatabagsForConfig(String credentialId) {
            Map<String, List<String>> jsondata = new TreeMap<String, List<String>>();
            try {
                ChefApi api = ChefApiBuilder.build(credentialId);
                Map<String, Collection<String>> dbis = DatabagRetriever.getDatabags(api).asMap();
                for (String databag : dbis.keySet()) {
                    jsondata.put(databag, new LinkedList<String>(dbis.get(databag)));
                }
            } catch (Exception e) {
                jsondata.put("error", Arrays.asList(ExceptionUtils.getRootCauseMessage(e)));
            }
            return JSONObject.fromObject(jsondata);
        }

    }

}
