//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0, com.atlassian.jira:jira-rest-java-client-api:3.0.0, com.atlassian.jira:jira-rest-java-client-core:3.0.0, org.json:json:20200518, com.konghq:unirest-java:3.7.04, com.sun.mail:javax.mail:1.6.2

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

import com.sun.mail.smtp.SMTPTransport;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

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


    public static void main(String... args) {
        int exitCode = new CommandLine(new run()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        /*
            Initialise
         */
        final JiraRestClient restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(new URI(jiraServerURL), jiraUsername, jiraPassword);

        Iterable<Field> fields =  restClient.getMetadataClient().getFields().claim();
        for (Field field : fields) {
            try {
                //System.out.println("getFieldType: " + field.getSchema());
                //String fieldName = "Git Pull Request";
                String jiraQueryPerUser = "project = " + jiraProject + " AND '" + field.getName() + "' is not EMPTY";
                //System.out.println("Running: " + jiraQueryPerUser);
                SearchResult searchResult = restClient.getSearchClient().searchJql(jiraQueryPerUser).claim();

                if (searchResult.getTotal() > 0 || !ignoreUnused)
                {
                    System.out.println(field.getName() + "," + searchResult.getTotal());
                }
            } catch (Exception e) {
                //System.out.println(e.getMessage());
            }
        }



        return 0;
    }
}
