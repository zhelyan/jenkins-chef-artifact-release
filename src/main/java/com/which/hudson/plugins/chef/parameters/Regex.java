package com.which.hudson.plugins.chef.parameters;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by zpanche on 13/11/2014.
 */
public class Regex extends AbstractDescribableImpl<Regex> {

    private String value;

    public Regex() {
        super();
        this.value="";
    }

    @DataBoundConstructor
    public Regex(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Regex regex = (Regex) o;

        if (value != null ? !value.equals(regex.value) : regex.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<Regex> {

        @Override
        public String getDisplayName() {
            return StringUtils.EMPTY;
        }

        public FormValidation doCheckValue(@QueryParameter String value) {
            try{
                Pattern.compile(value);
            } catch (PatternSyntaxException e){
                return FormValidation.error("Invalid regex!:: %s", e.getMessage());
            }
            return FormValidation.ok();
        }
    }


}
