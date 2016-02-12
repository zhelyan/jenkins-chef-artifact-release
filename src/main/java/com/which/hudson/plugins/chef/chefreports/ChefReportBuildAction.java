package com.which.hudson.plugins.chef.chefreports;

import com.which.hudson.plugins.chef.Deployment;
import hudson.model.Action;

/**
 * Created by zhelyan.panchev on 21/05/14.
 */
public class ChefReportBuildAction implements Action {
    private static final String PLUGIN_NAME = "chef-build-report";
    private Deployment deployment;

    public ChefReportBuildAction() {
        super();
      }

    public String getUrlName() {
        return PLUGIN_NAME;
    }

    public String getDisplayName() {
        return "Deployed artifacts";
    }

    public String getIconFileName() {
        return "/plugin/chef-artifact-release/report.png";
    }

    public Deployment getTarget() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    public Deployment getDeployment() {
        return deployment;
    }
}
