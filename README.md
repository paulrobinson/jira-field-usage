# jira-field-usage

Queries a JIRA project to find out which issue fields are currently being used in a visible issue.

#Pre-reqs
Please install JBang from https://jbang.dev

#Usage

    jbang run.java --jira-server https://issues.redhat.com -u <JIRA username> -p <JIRA password> --project <Project code>

Currently, the list of fields is obtained via a API call on the JIRA server. In a futeure version this list could be specified via configuration.
