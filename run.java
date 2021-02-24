//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0, com.atlassian.jira:jira-rest-java-client-api:3.0.0, com.atlassian.jira:jira-rest-java-client-core:3.0.0, org.json:json:20200518, com.konghq:unirest-java:3.7.04, com.sun.mail:javax.mail:1.6.2

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

@Command(name = "run", mixinStandardHelpOptions = true, version = "run 0.1",
        description = "GitHub to Jira issue replicator")
class run implements Callable<Integer> {

    @CommandLine.Option(names = {"--project"}, description = "The JIRA project to query", required = true)
    private String jiraProject;

    @CommandLine.Option(names = {"-j", "--jira-server"}, description = "The JIRA server to connect to", required = true)
    private String jiraServerURL;

    @CommandLine.Option(names = {"-u", "--username"}, description = "The username to use when connecting to the JIRA server", required = true)
    private String jiraUsername;

    @CommandLine.Option(names = {"-p", "--password"}, description = "The password to use when connecting to the JIRA server", required = true)
    private String jiraPassword;

    @CommandLine.Option(names = {"-i", "--ignore_unused"}, description = "Don't output results for unused fields", defaultValue = "true")
    private boolean ignoreUnused;

    private static String CUSTOM = "CUSTOM";

    public static void main(String... args) {
        int exitCode = new CommandLine(new run()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        /*
            Initialise
         */
        final JiraRestClient jiraClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(new URI(jiraServerURL), jiraUsername, jiraPassword);

        Iterable<Field> fields =  jiraClient.getMetadataClient().getFields().claim();
        for (Field field : fields) {

            if (!field.getFieldType().name().equals(CUSTOM)) continue; //Skip if not a custom field

            try {

                String fieldUsageQuery = "project = " + jiraProject + " AND '" + field.getName() + "' is not EMPTY";
                SearchResult searchResult = jiraClient.getSearchClient().searchJql(fieldUsageQuery).claim();

                if (searchResult.getTotal() > 0 || !ignoreUnused) //Only log zero results if configured to do so
                {
                    String queryURL = jiraServerURL + "/issues/?jql=" + URLEncoder.encode(fieldUsageQuery, "UTF-8");
                    System.out.println(field.getName() + "," + searchResult.getTotal() + "," + queryURL);
                }
            } catch (Exception e) {
                //todo: be more aware of how this can fail, and make sure we're not missing fields that are actually used.
                /*System.out.println();
                System.out.println();
                System.out.println("==Field==");
                System.out.println(field);
                System.out.println("==ERROR MESSAGE==");
                System.out.println(e.getMessage());*/
            }
        }

        return 0;
    }
}
