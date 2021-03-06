package com.saucelabs.sod.selenium;


import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.saucelabs.sod.AbstractTestHelper;
import com.thoughtworks.selenium.Selenium;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static org.junit.Assert.assertEquals;

/**
 * @author Ross Rowe
 */
public class SauceConnect2Test extends AbstractTestHelper {
    private Server server;

    /**
     * Start a web server locally, set up an SSH tunnel, and have Sauce OnDemand connect to the local server.
     */
    @Test
    public void fullRun() throws Exception {
        Server server = startWebServer();

        try {
            // start a tunnel
            System.out.println("Starting a tunnel");
            final SauceOnDemandAuthentication c = new SauceOnDemandAuthentication();
            Authenticator.setDefault(
                    new Authenticator() {
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                    c.getUsername(), c.getAccessKey().toCharArray());
                        }
                    }
            );            
            SauceTunnelManager sauceTunnelManager = new SauceConnectTwoManager();
            Process sauceConnect = sauceTunnelManager.openConnection(c.getUsername(), c.getAccessKey(), 4445, null, null, null);

            System.out.println("tunnel established");
            String driver = System.getenv("SELENIUM_DRIVER");
            if (driver == null || driver.equals("")) {
                System.setProperty("SELENIUM_DRIVER", DEFAULT_SAUCE_DRIVER);
            }

            String originalUrl = System.getenv("SELENIUM_STARTING_URL");
            System.setProperty("SELENIUM_STARTING_URL", "http://localhost:" + PORT + "/");
            Selenium selenium = SeleniumFactory.create();            
            try {
                selenium.start();
                selenium.open("/");
                // if the server really hit our Jetty, we should see the same title that includes the secret code.
                assertEquals("test" + code, selenium.getTitle());                
            } finally {
                sauceTunnelManager.closeTunnelsForPlan(c.getUsername(), null);
                selenium.stop();
                if (originalUrl != null && !originalUrl.equals("")) {
                     System.setProperty("SELENIUM_STARTING_URL", originalUrl);
                }
            }
        } finally {
            server.stop();
        }
    }

}
