package org.protobj.mock.plan;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.module.annotation.CliMsgMethod;
import com.pv.framework.gs.core.module.msgproc.IRqstMsg;
import com.pv.framework.gs.core.module.msgproc.NullRqstMsg;
import io.reactivex.rxjava3.core.Observable;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RandomRqstPlan extends Plan {

    private static List<Object[]> allRqsts = new ArrayList<>();
    public static Map<Integer, Object[]> rqstMap = new HashMap<>();

    public static Map<Integer, String> rqstDescMap = new HashMap<>();

    static {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages("com.guangyu" + ".")
                .addScanners(Scanners.MethodsAnnotated)
                .addScanners(Scanners.TypesAnnotated)
        );
        Collection<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(CliMsgMethod.class);
        for (Method method : methodsAnnotatedWith) {
            CliMsgMethod annotation = method.getAnnotation(CliMsgMethod.class);
            int value = annotation.value();
            Class<? extends IRqstMsg> clazz = annotation.rqstType();
            Object[] objects;
            if (clazz == NullRqstMsg.class) {
                objects = new Object[]{method.getName(), value};
            } else {
                objects = new Object[]{method.getName(), value, clazz.getName()};
            }
            allRqsts.add(objects);
            rqstMap.put((Integer) objects[1], objects);
            rqstDescMap.put(value, annotation.desc4Cli().replaceAll(",", " "));
        }
    }

    private EasyRandom easyRanDom;

    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
        if (easyRanDom == null) {
            EasyRandomParameters easyRandomParameters = new EasyRandomParameters();
            easyRandomParameters.seed(connect.hashCode());
            easyRandomParameters.setObjectPoolSize(100000);
            easyRandomParameters.collectionSizeRange(5, 10);
            easyRandomParameters.stringLengthRange(0, 50);
            easyRanDom = new EasyRandom(easyRandomParameters);
        }
        int interval = 200;
        List<Object[]> temps = new ArrayList<>(allRqsts);
        connect.executor().scheduleAtFixedRate(() -> {
            if (connect.isOffLine()) {
                return;
            }
//            long andIncrement = easyRanDom.nextInt(allRqsts.size());
//            send(connect, allRqsts.get((int) andIncrement));
            if (temps.isEmpty()) {
                temps.addAll(allRqsts);
            }
            Object[] remove = temps.remove(0);
            if (remove[1].equals(Commands.EXPDN_CRE_CONST)) {
                return;
            }
            send(connect, remove);
        }, interval, interval, TimeUnit.MILLISECONDS);
        return Observable.empty();
    }

    private void send(MockConnect connect, Object[] objects) {
        if (objects.length == 3) {
            try {
                Object o = easyRanDom.nextObject(Class.forName((String) objects[2]));
                connect.send((Integer) objects[1], o);
            } catch (Exception e) {
                if (!e.getMessage().startsWith("不支持同时发送两个相同cmd请求")) {
                    e.printStackTrace();
                }

            }
        } else {
            try {
                connect.send((Integer) objects[1], null);
            } catch (Exception e) {
                if (!e.getMessage().startsWith("不支持同时发送两个相同cmd请求")) {
                    e.printStackTrace();
                }
            }
        }
    }

}
