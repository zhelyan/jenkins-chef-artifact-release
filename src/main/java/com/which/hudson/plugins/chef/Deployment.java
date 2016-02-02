package com.which.hudson.plugins.chef;


import java.util.List;

public class Deployment {
    private List<DeployableArtifact> artifacts;
    private String environment;
    private String status;
    private String started;
    private String finished;
    private String duration;
    private int buildNumber;

    public Deployment(List<DeployableArtifact>  artifacts, String environment, String status, String started, String finished, String duration) {
        this.artifacts = artifacts;
        this.environment = environment;
        this.status = status;
        this.started = started;
        this.finished = finished;
        this.duration = duration;
    }

    public Deployment() {
    }

    public List<DeployableArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<DeployableArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public String getFinished() {
        return finished;
    }

    public void setFinished(String finished) {
        this.finished = finished;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Deployment that = (Deployment) o;

        if (!artifacts.equals(that.artifacts)) return false;
        if (!environment.equals(that.environment)) return false;
        if (!(buildNumber  ==  that.buildNumber)) return false;
        if (!started.equals(that.started)) return false;
        if (!status.equals(that.status)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = artifacts.hashCode();
        result = 31 * result + environment.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + started.hashCode();
        result = 31 * result + buildNumber;
        return result;
    }


    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public int getBuildNumber() {
        return buildNumber;
    }
}