package net.sourceforge.subsonic.command;

import net.sourceforge.subsonic.controller.AdvancedSettingsController;

/**
 * Command used in {@link AdvancedSettingsController}.
 *
 * @author Sindre Mehus
 */
public class AdvancedSettingsCommand {
    private String downsampleCommand;
    private String coverArtLimit;
    private String downloadLimit;
    private String uploadLimit;
    private String streamPort;
    private boolean ldapEnabled;
    private String ldapUrl;
    private String ldapSearchFilter;
    private boolean isReloadNeeded;

    public String getDownsampleCommand() {
        return downsampleCommand;
    }

    public void setDownsampleCommand(String downsampleCommand) {
        this.downsampleCommand = downsampleCommand;
    }

    public String getCoverArtLimit() {
        return coverArtLimit;
    }

    public void setCoverArtLimit(String coverArtLimit) {
        this.coverArtLimit = coverArtLimit;
    }

    public String getDownloadLimit() {
        return downloadLimit;
    }

    public void setDownloadLimit(String downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    public String getUploadLimit() {
        return uploadLimit;
    }

    public String getStreamPort() {
        return streamPort;
    }

    public void setStreamPort(String streamPort) {
        this.streamPort = streamPort;
    }

    public void setUploadLimit(String uploadLimit) {
        this.uploadLimit = uploadLimit;
    }

    public boolean isReloadNeeded() {
        return isReloadNeeded;
    }

    public boolean isLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        this.ldapEnabled = ldapEnabled;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getLdapSearchFilter() {
        return ldapSearchFilter;
    }

    public void setLdapSearchFilter(String ldapSearchFilter) {
        this.ldapSearchFilter = ldapSearchFilter;
    }

    public void setReloadNeeded(boolean reloadNeeded) {
        isReloadNeeded = reloadNeeded;
    }
}
