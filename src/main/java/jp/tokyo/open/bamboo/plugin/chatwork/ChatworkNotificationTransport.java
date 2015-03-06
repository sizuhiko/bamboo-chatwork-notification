package jp.tokyo.open.bamboo.plugin.chatwork;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.builder.LifeCycleState;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.utils.HttpUtils;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.event.Event;

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

    public ChatworkNotificationTransport(String apiToken,
            String room,
            boolean notify,
            @Nullable ImmutablePlan plan,
            @Nullable ResultsSummary resultsSummary,
            @Nullable DeploymentResult deploymentResult,
            Event event,
            CustomVariableContext customVariableContext)
    {
        this.apiToken = customVariableContext.substituteString(apiToken);
        this.room = customVariableContext.substituteString(room);
        this.notify = notify;
        this.event = event;
        this.plan = plan;
        this.resultsSummary = resultsSummary;
        this.deploymentResult = deploymentResult;
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

        String message = (notification instanceof Notification.HtmlImContentProvidingNotification)
                ? ((Notification.HtmlImContentProvidingNotification) notification).getHtmlImContent()
                : notification.getIMContent();

        if (!StringUtils.isEmpty(message))
        {
        	log.debug("[ChatworkNotificationTransport] sendNotification");
            PostMethod method = setupPostMethod();
            method.setParameter("body", message);
            if (resultsSummary != null)
            {
                setMessageColor(method, resultsSummary);
            }
            else if (deploymentResult != null)
            {
                setMessageColor(method, deploymentResult);
            }
            else
            {
                setMessageColor(method, COLOR_UNKNOWN_STATE); //todo: might need to use different color in some cases
            }

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

    private void setMessageColor(PostMethod method, ResultsSummary result)
    {
        String color = COLOR_UNKNOWN_STATE;

        if (result.getBuildState() == BuildState.FAILED)
        {
            color = COLOR_FAILED;
        }
        else if (result.getBuildState() == BuildState.SUCCESS)
        {
            color = COLOR_SUCCESSFUL;
        }
        else if (LifeCycleState.isActive(result.getLifeCycleState()))
        {
            color = COLOR_IN_PROGRESS;
        }

        setMessageColor(method, color);
    }

    private void setMessageColor(PostMethod method, DeploymentResult deploymentResult)
    {
        String color = COLOR_UNKNOWN_STATE;

        if (deploymentResult.getDeploymentState() == BuildState.FAILED)
        {
            color = COLOR_FAILED;
        }
        else if (deploymentResult.getDeploymentState() == BuildState.SUCCESS)
        {
            color = COLOR_SUCCESSFUL;
        }
        else if (LifeCycleState.isActive(deploymentResult.getLifeCycleState()))
        {
            color = COLOR_IN_PROGRESS;
        }

        setMessageColor(method, color);
    }

    private void setMessageColor(PostMethod method, String colour)
    {
//        method.addParameter("color", colour);
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
}
