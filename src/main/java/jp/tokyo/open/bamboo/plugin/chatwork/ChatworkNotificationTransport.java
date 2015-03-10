package jp.tokyo.open.bamboo.plugin.chatwork;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.builder.LifeCycleState;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plugin.descriptor.NotificationRecipientModuleDescriptor;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.utils.HttpUtils;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.event.Event;
import com.google.common.collect.Maps;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ChatworkNotificationTransport implements NotificationTransport
{
    private static final Logger log = Logger.getLogger(ChatworkNotificationTransport.class);
    public static final String CHATWORK_API_URL = "https://api.chatwork.com/v1/rooms/{room_id}/messages";

    //colors, available options (as of 18.06.2013) are: yellow, red, green, purple and grey
    public static final String COLOR_UNKNOWN_STATE = "gray";
    public static final String COLOR_FAILED = "red";
    public static final String COLOR_SUCCESSFUL = "green";
    public static final String COLOR_IN_PROGRESS = "yellow";

    private final String apiToken;
    private final String room;
    private final String from = "Bamboo";
    private final boolean notify;

    private final HttpClient client;

    @Nullable
    private final ImmutablePlan plan;
    @Nullable
    private final ResultsSummary resultsSummary;
    @Nullable
    private final DeploymentResult deploymentResult;
    private final Event event;
    private TemplateRenderer templateRenderer;

    public ChatworkNotificationTransport(String apiToken,
            String room,
            boolean notify,
            @Nullable ImmutablePlan plan,
            @Nullable ResultsSummary resultsSummary,
            @Nullable DeploymentResult deploymentResult,
            Event event,
            CustomVariableContext customVariableContext,
            TemplateRenderer templateRenderer)
    {
        this.apiToken = customVariableContext.substituteString(apiToken);
        this.room = customVariableContext.substituteString(room);
        this.notify = notify;
        this.event = event;
        this.plan = plan;
        this.resultsSummary = resultsSummary;
        this.deploymentResult = deploymentResult;
        this.templateRenderer = templateRenderer;
        client = new HttpClient();

        try
        {
            URI uri = new URI(getChatworkApiURL(this.room));
            setProxy(client, uri.getScheme());
            log.debug("Set proxy client to "+uri.getScheme());
            log.debug("room id : "+this.room);
        }
        catch (URIException e)
        {
            log.error("Unable to set up proxy settings, invalid URI encountered: " + e);
        }
        catch (URISyntaxException e)
        {
            log.error("Unable to set up proxy settings, invalid URI encountered: " + e);
        }
    }

    @Override
    public void sendNotification(Notification notification)
    {
        String message = getChatworkContent();

        if (!StringUtils.isEmpty(message))
        {
            log.debug("[ChatworkNotificationTransport] sendNotification");
            PostMethod method = setupPostMethod();
            method.setParameter("body", message);
            try
            {
                int status = client.executeMethod(method);
            	log.debug("[ChatworkNotificationTransport] sendNotification respose is "+String.valueOf(status));
            }
            catch (IOException e)
            {
                log.error("Error using ChatWork API: " + e.getMessage(), e);
            }
        }
        else {
        	log.debug("[ChatworkNotificationTransport] sendNotification is ** NO MESSAGE **");
        }
    }

    private PostMethod setupPostMethod()
    {
        PostMethod m = new PostMethod(getChatworkApiURL(this.room));
        m.addRequestHeader("X-ChatWorkToken", apiToken);
        return m;
    }

    static void setProxy(@NotNull final HttpClient client, @Nullable final String scheme) throws URIException
    {
        HttpUtils.EndpointSpec proxyForScheme = HttpUtils.getProxyForScheme(scheme);
        if (proxyForScheme!=null)
        {
            client.getHostConfiguration().setProxy(proxyForScheme.host, proxyForScheme.port);
        }
    }

    /**
     * @return
     */
    static String getChatworkApiURL(String room) {
        return CHATWORK_API_URL.replaceAll("\\{room_id\\}", room);
    }


    private String getChatworkContent() {
        String templateLocation = "templates/plugins/notifications/chatwork/BuildCompleted.ftl";
        return templateRenderer.render(templateLocation, populateContext());
    }

    private Map<String, Object> populateContext()
    {
        Map<String, Object> context = Maps.newHashMap();
        context.put("build", plan);
        context.put("buildSummary", resultsSummary);
        return context;
    }

}
