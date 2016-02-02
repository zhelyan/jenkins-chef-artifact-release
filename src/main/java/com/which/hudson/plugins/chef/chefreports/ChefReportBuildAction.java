package com.which.hudson.plugins.chef.chefreports;

import com.which.hudson.plugins.chef.Deployment;
import com.which.hudson.plugins.chef.api.ChefApiBuilder;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ProminentProjectAction;
import net.sf.json.JSONObject;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.domain.DatabagItem;
import org.kohsuke.stapler.StaplerProxy;

import java.util.*;

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
        return "/plugin/chef-wrapper/report.png";
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
