package nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** LaunchDarkly properties */
@Configuration
@ConfigurationProperties(prefix = "launchdarkly")
public class LaunchDarklyProperties {

    private String sdkKey;

    private String defaultUser;

    private boolean offline;

    private DataSource dataSource;

    public String getSdkKey() {
        return sdkKey;
    }

    public void setSdkKey(String sdkKey) {
        this.sdkKey = sdkKey;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public void setDefaultUser(String defaultUser) {
        this.defaultUser = defaultUser;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Configuration for datasource related attributes */
    public static class DataSource {

        public enum Type {
            STREAM,
            FILE
        }

        private Type type = Type.STREAM;

        private FileConfig file;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public FileConfig getFile() {
            return file;
        }

        public void setFile(FileConfig file) {
            this.file = file;
        }

        /** Configuration for file based data source */
        public static class FileConfig {

            private String location;

            private boolean autoReload;

            public String getLocation() {
                return location;
            }

            public void setLocation(String location) {
                this.location = location;
            }

            public boolean isAutoReload() {
                return autoReload;
            }

            public void setAutoReload(boolean autoReload) {
                this.autoReload = autoReload;
            }
        }
    }
}
