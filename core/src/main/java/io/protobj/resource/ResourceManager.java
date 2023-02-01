package io.protobj.resource;

import io.protobj.BeanContainer;
import io.protobj.IServer;
import io.protobj.Module;
import io.protobj.resource.single.SingleResourceManager;
import io.protobj.resource.table.TableResourceManager;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 支持重载，依赖注入，单值配置，表配置
 */
public class ResourceManager {

    private static final Logger log = LoggerFactory.getLogger(ResourceManager.class);

    private final ResourceConfig resourceConfig;

    private final SingleResourceManager singleResourceManager;

    private final TableResourceManager tableResourceManager;

    public ResourceManager(ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        this.singleResourceManager = new SingleResourceManager(this);
        this.tableResourceManager = new TableResourceManager(this);
    }


    public void loadResource(List<Module> moduleList,IServer server, boolean reload) {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.FieldsAnnotated);
        for (Module module : moduleList) {
            configurationBuilder.forPackages(module.getClass().getPackage().getName() + "." + IServer.SERVICE_PACKAGE);
        }
        Reflections reflections = new Reflections(configurationBuilder);

        singleResourceManager.loadSingleResource(server, reflections, reload);

        tableResourceManager.loadTableResource(server, reflections, reload);
    }

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    public SingleResourceManager getSingleResourceManager() {
        return singleResourceManager;
    }

    public TableResourceManager getTableResourceManager() {
        return tableResourceManager;
    }
}
