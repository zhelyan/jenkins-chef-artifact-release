package com.which.hudson.plugins.chef.chefreports;

import com.which.hudson.plugins.chef.Deployment;
import hudson.model.*;

import java.lang.ref.WeakReference;
import java.util.*;

import org.jclouds.chef.ChefApi;

/**
 * Created by zhelyan.panchev on 21/05/14.
 */
public class ChefReportProjectAction implements ProminentProjectAction {
    private static final String PLUGIN_NAME = "chef-project-report";
    private final WeakReference<AbstractProject> project;

    public ChefReportProjectAction(WeakReference<AbstractProject> project) {
        super();
        this.project = project;
    }

    public String getUrlName() {
        return PLUGIN_NAME;
    }

    public String getDisplayName() {
        return "Environment report";
    }

    public String getIconFileName() {
        return "/plugin/chef-wrapper/report.png";
    }


    public List<ChefEnvironment> getEnvironments() {
        EnvWrapper envWrapper = new EnvWrapper();

        final List<? extends AbstractBuild<?, ?>> builds = project.get().getBuilds();
        for (AbstractBuild<?, ?> currentBuild : builds) {
            final ChefReportBuildAction chefBuildAction = currentBuild.getAction(ChefReportBuildAction.class);
            if (chefBuildAction == null) {
                continue;
            }
            final Deployment deployment = chefBuildAction.getTarget();
            if (deployment == null) {
                continue;
            }
            envWrapper.getEnvByName(deployment.getEnvironment()).addOrUpdateDeployment(deployment);
        }
        List<ChefEnvironment> envs = envWrapper.getEnvironments();
        Collections.sort(envs, new Comparator<ChefEnvironment>() {
            public int compare(ChefEnvironment current, ChefEnvironment other) {
                return current.getName().compareToIgnoreCase(other.getName());
            }
        });

        return envs;
    }


    public class EnvWrapper{
        private List<ChefEnvironment> environments;

        public EnvWrapper(){
            environments = new LinkedList<ChefEnvironment>();
        }


        public ChefEnvironment getEnvByName(String name){
            for(ChefEnvironment env : environments){
                if(env.getName().equalsIgnoreCase(name)){
                    return env;
                }

            }
            ChefEnvironment newEnv = new ChefEnvironment(name);
            environments.add(newEnv);
            return newEnv;
        }

        public List<ChefEnvironment> getEnvironments() {
            return environments;
        }
    }



}
