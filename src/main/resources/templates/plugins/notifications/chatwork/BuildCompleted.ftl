[#-- @ftlvariable name="build" type="com.atlassian.bamboo.build.Buildable" --]
[#-- @ftlvariable name="buildSummary" type="com.atlassian.bamboo.resultsummary.BuildResultsSummary" --]
[#include "/notification-templates/notificationCommons.ftl"]
[#include "/notification-templates/notificationCommonsText.ftl" ]
[#assign authors = buildSummary.uniqueAuthors/]

[#if buildSummary.successful][#lt]
[info][title][@buildNotificationTitleText build buildSummary/] was SUCCESSFUL[/title]
[@showRestartCount buildSummary/]
[#if buildSummary.testResultsSummary.totalTestCaseCount >0] [@showTestSummary buildSummary.testResultsSummary/][/#if].
[#if authors?has_content] [@showAuthorSummary authors/][/#if][#lt]
${baseUrl}/browse/${buildSummary.planResultKey}/
[/info]
[#else][#lt]
[info][title][@buildNotificationTitleText build buildSummary/] has FAILED[/title]
[@showRestartCount buildSummary/]
[#if buildSummary.testResultsSummary.totalTestCaseCount >0] [@showTestSummary buildSummary.testResultsSummary/][/#if].
[#if authors?has_content] [@showAuthorSummary authors/][/#if][#lt]
${baseUrl}/browse/${buildSummary.planResultKey}/
[/info]
[/#if][#lt]