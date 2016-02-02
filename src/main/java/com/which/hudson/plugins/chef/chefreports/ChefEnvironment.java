package com.which.hudson.plugins.chef.chefreports;

import com.which.hudson.plugins.chef.Deployment;


import java.util.ArrayList;
import java.util.List;

public class ChefEnvironment {
    private List<Deployment> deployments;
    private String name;

    public ChefEnvironment(String app) {
        deployments = new  ArrayList<Deployment>(0);
        this.name = app;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void addOrUpdateDeployment(Deployment deployment) {
        synchronized (this) {
            if (deployments.isEmpty()) {
                deployments = new ArrayList<Deployment>();
                deployments.add(deployment);
            } else {
                if (deployments.contains(deployment)) {
                    deployments.set(deployments.indexOf(deployment), deployment);
                } else {
                    deployments.add(deployment);
                }
            }
        }

    }

    public List<Deployment> getDeployments() {
        synchronized (this) {
            return deployments;
        }
    }
}
