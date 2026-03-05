package tn.esprit.utils;

import tn.esprit.plugins.IMentorshipPlugin;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class PluginManager {

    private static final String PLUGINS_DIR = "plugins";
    private final List<IMentorshipPlugin> loadedPlugins = new ArrayList<>();

    private static PluginManager instance;

    private PluginManager() {
    }

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void loadPlugins() {
        File dir = new File(PLUGINS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            return;
        }

        try {
            List<URL> urls = new ArrayList<>();
            for (File f : files) {
                urls.add(f.toURI().toURL());
            }

            URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
            ServiceLoader<IMentorshipPlugin> loader = ServiceLoader.load(IMentorshipPlugin.class, cl);

            for (IMentorshipPlugin plugin : loader) {
                loadedPlugins.add(plugin);
                plugin.start();
                AuditLogger.log("Loaded plugin: " + plugin.getName() + " v" + plugin.getVersion());
            }
        } catch (Exception e) {
            AuditLogger.logWarning("Error loading plugins: " + e.getMessage());
        }
    }

    public void unloadPlugins() {
        for (IMentorshipPlugin plugin : loadedPlugins) {
            try {
                plugin.stop();
                AuditLogger.log("Stopped plugin: " + plugin.getName());
            } catch (Exception e) {
                AuditLogger.logWarning("Error stopping plugin " + plugin.getName() + ": " + e.getMessage());
            }
        }
        loadedPlugins.clear();
    }

    public List<IMentorshipPlugin> getLoadedPlugins() {
        return loadedPlugins;
    }
}
