package com.cfpamf.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.*;
import hudson.util.FormValidation;
import lombok.Data;
import lombok.Getter;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;

/**
 * 消息推送通知器，继承{@link Notifier}
 * <p> 在构建后的操作中生效，其内部参数通过{@link DataBoundConstructor} 进行双向绑定和存储 </p>
 * <p> 数据绑定方式是在resources目录下同样层级的WatchDogNotifier文件夹下面</p>
 * <p>Notifier本身的参数通过config.jelly作为项目变量进行绑定</p>
 * <p>Notifier内部的Descriptor的参数通过global.jelly作为全局变量进行绑定</p>
 *
 * @author flavone
 * @date 2019/03/27
 */
public class WatchDogNotifier extends Notifier {

    @Getter
    private String microServiceId;

    @Getter
    private String signature;


    private static final Logger logger = LoggerFactory.getLogger(WatchDogNotifier.class);

    /**
     * 触发依赖的监听器，一般用NONE就足够了，详见{@link BuildStep#getRequiredMonitorService()}
     *
     * @return
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * 使用{@link DataBoundConstructor} 将参数进行双向绑定的构造函数
     *
     * @param microServiceId
     * @param signature
     */
    @DataBoundConstructor
    public WatchDogNotifier(String microServiceId, String signature) {
        super();
        this.microServiceId = microServiceId;
        this.signature = signature;
    }


    /**
     * 参数校验方法，不知道为什么在这里没有生效
     *
     * @param value
     * @return
     */
    public FormValidation doCheckMicroServiceId(@QueryParameter String value) {
        if (value.length() == 0) {
            return FormValidation.error("microServiceId不能为空");
        }
        try {
            Integer i = Integer.valueOf(value);
        } catch (Exception e) {
            return FormValidation.error("microServiceId必须为整数");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckSignature(@QueryParameter String value) {
        if (value.length() == 0) {
            return FormValidation.error("signature不能为空");
        }
        return FormValidation.ok();
    }

    /**
     * 通过通知器的参数获取通知的服务，如果不用监听器{@link JobListener}, 也可以在perform()中进行事务操作
     *
     * @param build
     * @param listener
     * @return
     */
    public WatchDogService newWatchDogService(AbstractBuild build, TaskListener listener) {
        WatchDogNotifierDescriptor descriptor = getDescriptor();
        String apiUrl = descriptor.getApiUrl();
        logger.debug(apiUrl);
        logger.debug(microServiceId);
        logger.debug(signature);
        return new WatchDogServiceImpl(listener, build, apiUrl, microServiceId, signature);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Override
    public WatchDogNotifierDescriptor getDescriptor() {
        return (WatchDogNotifierDescriptor) super.getDescriptor();
    }

    /**
     * 通知器的描述类，主要告知Jenkins该通知器是否可用，以及通知器的全局参数配置保存与加载
     * <p>可以循环嵌套多层Descriptor</p>
     */
    @Symbol("watchDog")
    @Extension
    @Data
    public static final class WatchDogNotifierDescriptor extends BuildStepDescriptor<Publisher> {
        private String apiUrl;

        public WatchDogNotifierDescriptor() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "推送构建消息到报表";
        }

        /**
         * 这里面的参数校验有效
         *
         * @param value
         * @return
         */
        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("URL不能为空");
            }
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                return FormValidation.error("URL开始必须是http:// or https://");
            }
            try {
                new URL(value).toURI();
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(StaplerRequest request, JSONObject json) throws FormException {
            logger.debug(json.getJSONObject("watch-dog").toString());
            request.bindJSON(this, json.getJSONObject("watch-dog"));
            save();
            return true;
        }
    }

}
