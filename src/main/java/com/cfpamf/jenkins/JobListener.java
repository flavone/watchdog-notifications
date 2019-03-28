package com.cfpamf.jenkins;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author flavone
 * @date 2019/03/27
 */
@Extension
public class JobListener extends RunListener<AbstractBuild> {
    /**
     * 直接继承抽象类的构建方法
     */
    public JobListener() {
        super(AbstractBuild.class);
    }

    /**
     * 在构建启动时的监听操作
     *
     * @param r
     * @param listener
     */
    @Override
    public void onStarted(AbstractBuild r, @Nonnull TaskListener listener) {

    }

    /**
     * 在构建结束时的监听操作
     *
     * @param r
     * @param listener
     */
    @Override
    public void onCompleted(AbstractBuild r, @Nonnull TaskListener listener) {
        Result result = r.getResult();
        if (null != result && result.equals(Result.SUCCESS)) {
            getService(r, listener).send(true);
        } else if (null != result && result.equals(Result.FAILURE)) {
            getService(r, listener).send(false);
        } else if (null != result && result.equals(Result.ABORTED)) {
            getService(r, listener).send(false);
        } else {
            getService(r, listener).send(false);
        }
    }

    /**
     * 根据构建的项目信息获取Notifier的内部服务
     *
     * @param build
     * @param listener
     * @return
     */
    private WatchDogService getService(AbstractBuild build, TaskListener listener) {
        Map map = build.getProject().getPublishersList().toMap();
        for (Object publisher : map.values()) {
            if (publisher instanceof WatchDogNotifier) {
                return ((WatchDogNotifier) publisher).newWatchDogService(build, listener);
            }
        }
        return null;
    }

}
