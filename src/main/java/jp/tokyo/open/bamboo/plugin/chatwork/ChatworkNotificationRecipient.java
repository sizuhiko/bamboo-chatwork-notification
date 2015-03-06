package jp.tokyo.open.bamboo.plugin.chatwork;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.deployments.notification.DeploymentResultAwareNotificationRecipient;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.NotificationRecipient;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.notification.recipients.AbstractNotificationRecipient;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plugin.descriptor.NotificationRecipientModuleDescriptor;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.event.Event;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ChatworkNotificationRecipient extends AbstractNotificationRecipient implements DeploymentResultAwareNotificationRecipient,
                                                                                           NotificationRecipient.RequiresPlan,
                                                                                           NotificationRecipient.RequiresResultSummary,
                                                                                           NotificationRecipient.RequiresEvent

{
    private static final Logger log = Logger.getLogger(ChatworkNotificationRecipient.class);
    private static String API_TOKEN = "chatWorkApiToken";
    private static String ROOM = "chatWorkRoom";
    private static String NOTIFY_USERS = "chatWorkNotifyUsers";

    private String apiToken = null;
    private String room = null;
    private boolean notify = false;

    private TemplateRenderer templateRenderer;

    private ImmutablePlan plan;
    private ResultsSummary resultsSummary;
    private DeploymentResult deploymentResult;
    private Event event;
    private CustomVariableContext customVariableContext;

    @Override
    public void populate(@NotNull Map<String, String[]> params)
    {
        for (String next : params.keySet())
        {
            System.out.println("next = " + next);
        }
        if (params.containsKey(API_TOKEN))
        {
            this.apiToken = params.get(API_TOKEN)[0];
        }
        if (params.containsKey(ROOM))
        {
            this.room = params.get(ROOM)[0];
        }

        this.notify = params.containsKey(NOTIFY_USERS);

        log.debug("[ChatWork:populate] room: "+this.room);
        log.debug("[ChatWork:populate] notify: "+this.notify);
    }

    @Override
    public void init(@Nullable String configurationData)
    {
        if (configurationData != null)
        {
            int firstIdx = configurationData.indexOf('|');
            if (firstIdx > 0)
            {
                int secondIdx = configurationData.indexOf('|', firstIdx + 1);
                apiToken = configurationData.substring(0, firstIdx);
                notify = configurationData.substring(firstIdx + 1, secondIdx).equals("true");
                room = configurationData.substring(secondIdx + 1);
            }
        }
        log.debug("[ChatWork:init] room: "+this.room);
        log.debug("[ChatWork:init] notify: "+this.notify);
    }

    @NotNull
    @Override
    public String getRecipientConfig()
    {
        // We can do this because API tokens don't have | in them, but it's pretty dodge. Better to JSONify or something?
        return apiToken + '|' + String.valueOf(notify) + '|' + room;
    }

    @NotNull
    @Override
    public String getEditHtml()
    {
        String editTemplateLocation = ((NotificationRecipientModuleDescriptor)getModuleDescriptor()).getEditTemplate();
        return templateRenderer.render(editTemplateLocation, populateContext());
    }

    private Map<String, Object> populateContext()
    {
        Map<String, Object> context = Maps.newHashMap();
        if (apiToken != null)
        {
            context.put(API_TOKEN, apiToken);
        }
        if (room != null)
        {
            context.put(ROOM, room);
        }
        context.put(NOTIFY_USERS, notify);
        return context;
    }

    @NotNull
    @Override
    public String getViewHtml()
    {
        String editTemplateLocation = ((NotificationRecipientModuleDescriptor)getModuleDescriptor()).getViewTemplate();
        return templateRenderer.render(editTemplateLocation, populateContext());
    }



    @NotNull
    @Override
    public List<NotificationTransport> getTransports()
    {
        log.debug("[ChatWork:getTransports] room: "+this.room);
        log.debug("[ChatWork:getTransports] notify: "+this.notify);
        List<NotificationTransport> list = Lists.newArrayList();
        list.add(new ChatworkNotificationTransport(apiToken, room, notify, plan, resultsSummary, deploymentResult, event, customVariableContext));
        return list;
    }

    @Override
    public void setEvent(@Nullable final Event event)
    {
        this.event = event;
    }

    public void setPlan(@Nullable final Plan plan)
    {
        this.plan = plan;
    }

    @Override
    public void setPlan(@Nullable final ImmutablePlan plan)
    {
        this.plan = plan;
    }

    @Override
    public void setDeploymentResult(@Nullable final DeploymentResult deploymentResult)
    {
        this.deploymentResult = deploymentResult;
    }

    @Override
    public void setResultsSummary(@Nullable final ResultsSummary resultsSummary)
    {
        this.resultsSummary = resultsSummary;
    }

    //-----------------------------------Dependencies
    public void setTemplateRenderer(TemplateRenderer templateRenderer)
    {
        this.templateRenderer = templateRenderer;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) { this.customVariableContext = customVariableContext; }
}
