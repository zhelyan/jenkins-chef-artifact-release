package com.which.hudson.plugins.chef;

/**
 * Created by zpanche on 10/09/2014.
 */
public class DeployableArtifact {
    private String name, version;

    public DeployableArtifact() {
        super();
    }

    public DeployableArtifact(String name, String version) {
        this();
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeployableArtifact that = (DeployableArtifact) o;

        if (!name.equals(that.name)) return false;
        if (!version.equals(that.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return name + " => " + version;
    }
}
