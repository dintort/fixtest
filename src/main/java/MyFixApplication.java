import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Attributes;
import net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header;
import net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.RequestForeignExchangeForwardForward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix50sp2.SecurityDefinitionRequest;
import quickfix.fix50sp2.component.SecurityXML;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

public class MyFixApplication implements Application {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final Object monitor = new Object();
    private final ObjectWriter jsonWriter = new ObjectMapper().writer();
    private volatile SocketInitiator socketInitiator;
    private volatile SessionID sessionId;


    public static void main(String[] args) {
        new MyFixApplication();
    }

    public MyFixApplication() {
        init();
        command();
    }

    private void command() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            log.info("Please enter command");
            try {
                String command = input.readLine();
                if (command == null) {
                    continue;
                }
                if (command.startsWith("createfx")) {
                    createFx();
                } else if (command.startsWith("exit")) {
                    socketInitiator.stop();
                    System.exit(0);
                } else {
                    log.info("Unsupported command: " + command);
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return;
            }

        }
    }

    private void createFx() throws SessionNotFound, JsonProcessingException {

        RequestForeignExchangeForwardForward instrument = new RequestForeignExchangeForwardForward();
        Header header = new Header();
        instrument.setHeader(header);
        header.setAssetClass(Header.AssetClass.FOREIGN_EXCHANGE);
        header.setInstrumentType(Header.InstrumentType.FORWARD);
        header.setLevel(Header.Level.INST_REF_DATA_REPORTING);
        header.setUseCase(Header.UseCase.FORWARD);

        Attributes attributes = new Attributes();
        instrument.setAttributes(attributes);

        attributes.setExpiryDate("2017-12-31");
        attributes.setNotionalCurrency(Attributes.NotionalCurrency.EUR);
        attributes.setOtherNotionalCurrency(Attributes.OtherNotionalCurrency.DKK);

        String requestJson = jsonWriter.forType(instrument.getClass()).writeValueAsString(instrument);

        SecurityDefinitionRequest request = new SecurityDefinitionRequest();

        request.set(new SecurityReqID(UUID.randomUUID().toString()));
        request.set(new SecurityRequestType(SecurityRequestType.REQUEST_SECURITY_IDENTITY_FOR_THE_SPECIFICATIONS_PROVIDED));
        request.set(new Symbol("N/A"));

        SecurityXML securityXml = new SecurityXML();
        securityXml.set(new SecurityXMLData(requestJson));
        securityXml.set(new SecurityXMLLen(requestJson.length()));
        request.set(securityXml);

        Session.sendToTarget(request, sessionId);
    }


    private void init() {
        SocketInitiator socketInitiatorLocal = null;
        boolean success = false;
        try {
            SessionSettings sessionSettings = new SessionSettings(this.getClass().getResourceAsStream("fix-session.txt"));
            sessionSettings.setBool("ScreenIncludeMilliseconds", true);
            sessionSettings.setBool("ScreenLogShowHeartBeats", false);
            LogFactory logFactory = new ScreenLogFactory(sessionSettings);
            socketInitiator = new SocketInitiator(this, new NoopStoreFactory(), sessionSettings,
                    logFactory, new DefaultMessageFactory());
            socketInitiatorLocal = socketInitiator;
            socketInitiator.start();
            int attempts = 0;
            while ((!socketInitiator.isLoggedOn()) && (attempts < 60)) {
                try {
                    synchronized (monitor) {
                        monitor.wait(1000);
                    }
                    log.info("Logged on: " + socketInitiator.isLoggedOn());
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                attempts++;
            }
            if (!socketInitiator.isLoggedOn()) {
                throw new IllegalStateException("Failed to logon to ANNA DSB");
            }
            success = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (socketInitiatorLocal != null) {
                try {
                    socketInitiatorLocal.stop(true);
                } catch (Exception ee) {
                    log.error("Failed closing socket initiator", ee);
                }
            }
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {

    }

    @Override
    public void onLogon(SessionID sessionId) {
        this.sessionId = sessionId;

    }

    @Override
    public void onLogout(SessionID sessionId) {

    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        try {
            if (message.getHeader().getString(MsgType.FIELD).equals(MsgType.LOGON)) {
                message.setString(Username.FIELD, System.getProperty("fixUsername"));
                message.setString(Password.FIELD, System.getProperty("fixPassword"));
            }
        } catch (FieldNotFound fieldNotFound) {
            fieldNotFound.printStackTrace();
        }

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {

    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

    }
}
