package io.protobj.hotswap;

import com.sun.tools.attach.VirtualMachine;
import javassist.ClassPool;
import javassist.util.HotSwapAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

public class JavassistUtil {
    private static final Logger log = LoggerFactory.getLogger(JavassistUtil.class);
    private volatile static Instrumentation instrumentation;

    public static ClassPool getClassPool() {
        return ClassPool.getDefault();
    }

    /**
     * 替换class
     */
    public static void swapClass(Class oldClass, byte[] newClass) throws Exception {
        attach();
        final ClassDefinition classDefinition = new ClassDefinition(oldClass, newClass);
        instrumentation.redefineClasses(classDefinition);
    }

    /**
     * 添加agent到虚拟机
     */
    private static void attach() throws Exception {
        if (instrumentation == null) {
            final String pathname = "hotswap.jar";
            try {
                final File file = new File(pathname);
                if (!file.exists()) {
                    HotSwapAgent.createAgentJarFile(pathname);
                }
            } catch (Exception e) {
                log.error("创建 hotswap.jar 失败 ", e);
            }
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                vm.loadAgent(pathname);
            } finally {
                vm.detach();
            }
            final Field instrumentation = HotSwapAgent.class.getDeclaredField("instrumentation");
            instrumentation.setAccessible(true);
            JavassistUtil.instrumentation = (Instrumentation) instrumentation.get(null);
        }
    }


}
