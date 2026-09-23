package com.song.model;

public class ScanPath {
    private String path;
    private boolean enabled;

    public ScanPath() {}

    public ScanPath(String path, boolean enabled) {
        this.path = path;
        this.enabled = enabled;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() { return path + (enabled ? " [启用]" : " [关闭]"); }
}