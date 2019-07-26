package gfuzan.hotstart;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import gfuzan.hotstart.ClassLoaderFactory.CustomClassLoader;

public class HotStartApplication {

    /**
     * properties 文件列表
     */
    private static final String[] PROPERTIES = new String[] { "application.properties" };

    public static void main(String[] args) {
        ConfigInfo config = new ConfigInfo(PROPERTIES);

        MyRunnable myRunnable = new MyRunnable(ClassLoaderFactory.getClassLoader(config.classesPath, config.libPath));
        myRunnable.startClass = config.startClass;

        Thread thread = new Thread(myRunnable);
        thread.setContextClassLoader(myRunnable.ccl.getURLClassLoader());
        thread.start();

        hotStart(thread, myRunnable, config);
    }

    /**
     * 热启动
     * @param thread 线程
     * @param myRunnable 
     * @param config 配置信息
     */
    @SuppressWarnings("deprecation")
    private static void hotStart(Thread thread, MyRunnable myRunnable, ConfigInfo config) {
        // 初始化文件信息
        final Map<String, Long> fileInfo = InitFileInfo(config.classesPath);

        // 检测文件变化并重启
        try {
            for (;;) {
                boolean newLoad = false;
                for (String key : fileInfo.keySet()) {
                    File file = new File(key);
                    Long time = fileInfo.get(key);
                    if (FileUtils.isFileNewer(file, time)) {
                        fileInfo.put(key, file.lastModified());
                        if (!newLoad) {
                            System.out.println("-----------重新加载----------");
                            newLoad = true;
                            // 停止不了运行中的线程
                            {
                                thread.interrupt();
                                thread.stop();
                            }
                            myRunnable.ccl.getURLClassLoader().close();
                            myRunnable.ccl = ClassLoaderFactory.getClassLoader(config.classesPath, config.libPath);
                            thread = new Thread(myRunnable);
                            thread.setContextClassLoader(myRunnable.ccl.getURLClassLoader());
                            thread.start();
                        }
                    }
                }
                newLoad = false;
                Thread.sleep(config.scanInterval * 1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化文件信息
     * @param classesPath 类路径
     * @param libPath jar包路径
     */
    private static Map<String, Long> InitFileInfo(String[] classesPaths) {
        Map<String, Long> fileInfo = new HashMap<>();
        for (String classesPath : classesPaths) {
            Collection<File> listFiles = FileUtils.listFiles(new File(classesPath), new String[] { "class" }, true);
            for (File file : listFiles) {
                if (file.canRead()) {
                    fileInfo.put(file.getAbsolutePath(), file.lastModified());
                }
            }
        }

        return fileInfo;
    }

    /**
     * 配置信息
     * @author GFuZan
     *
     */
    private static class ConfigInfo {

        /**
         * 扫描间隔(单位s)
         */
        private static final String SCAN_INTERVAL = "ScanInterval";
        final int scanInterval;

        /**
         * 启动类
         */
        private static final String START_CLASS = "StartClass";
        final String startClass;

        /**
         * 类路径
         */
        private static final String CLASSES_PATH = "ClassesPath";
        final String[] classesPath;

        /**
         * jar包路径
         */
        private static final String LIB_PATH = "LibPath";
        final String[] libPath;

        ConfigInfo(String[] files) {

            Configuration configuration = getConfigurations(PROPERTIES);

            // 获取配置文件内容
            this.scanInterval = configuration.getInt(SCAN_INTERVAL, 30);
            this.startClass = configuration.getString(START_CLASS);
            this.classesPath = configuration.getStringArray(CLASSES_PATH);
            this.libPath = configuration.getStringArray(LIB_PATH);

            // 配置信息校验
            if (startClass == null || startClass.isEmpty() || classesPath == null || classesPath.length == 0) {
                throw new NullPointerException("请配置参数: '" + START_CLASS + "','" + CLASSES_PATH + "'");
            }
        }

        /**
         * 获取配置文件
         * @param file 文件名
         * @return
         */
        private static Configuration getConfigurations(String[] files) {

            Configuration configurations = new Configuration();

            ClassLoader classLoader = HotStartApplication.class.getClassLoader();
            for (String file : files) {
                Configuration properties = Configuration.properties(classLoader.getResource(file));
                configurations.append(properties);
            }

            return configurations;
        }
    }

    /**
     * 运行接口
     * @author GFuZan
     *
     */
    private static class MyRunnable implements Runnable {
        public CustomClassLoader ccl = null;
        public String startClass = null;

        public MyRunnable(CustomClassLoader ccl) {
            this.ccl = ccl;
        }

        @Override
        public void run() {
            try {
                ccl.findClass(startClass).getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
