import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ ClientCallTest.class, ClientConnectionTest.class, ClientLoadTest.class, ClientRequestTest.class,
        ClientVariableTest.class, ClientDataTest.class, ClientSubscriptionTest.class,
        ClientMessageRetainDelayTest.class, ClientPresenceTest.class })
public class AllTests {

}
