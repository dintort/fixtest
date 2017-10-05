import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.RequestForeignExchangeForwardForward;
import net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Attributes;
import net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.RequestRatesSwapFixedFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix50sp2.SecurityDefinitionRequest;
import quickfix.fix50sp2.component.SecurityXML;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
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
            try {
                log.info("Please enter command");
                String command = input.readLine();
                if (command == null) {
                    continue;
                }
                if (command.startsWith("exit")) {
                    socketInitiator.stop();
                    System.exit(0);
                } else {
                    Method method = this.getClass().getMethod(command);
                    method.invoke(this);
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }
    }

    public void createRates() throws SessionNotFound, JsonProcessingException {

        RequestRatesSwapFixedFloat instrument = new RequestRatesSwapFixedFloat();
        net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Header header = new net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Header();
        instrument.setHeader(header);
        header.setAssetClass(net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Header.AssetClass.RATES);
        header.setInstrumentType(net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Header.InstrumentType.SWAP);
        header.setLevel(net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Header.Level.INST_REF_DATA_REPORTING);
        header.setUseCase(net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Header.UseCase.FIXED_FLOAT);

        net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Attributes attributes = new net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Attributes();
        instrument.setAttributes(attributes);

        attributes.setExpiryDate("2017-12-31");
        attributes.setNotionalCurrency(net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Attributes.NotionalCurrency.EUR);
        attributes.setReferenceRate(Attributes.ReferenceRate.CAD_ISDA_SWAP_RATE);
        attributes.setReferenceRateTermValue(42);
        attributes.setReferenceRateTermUnit(Attributes.ReferenceRateTermUnit.DAYS);
        attributes.setNotionalSchedule(Attributes.NotionalSchedule.ACCRETING);

        sendInstrumentRequest(instrument);
    }

    public void createFx() throws SessionNotFound, JsonProcessingException {

        RequestForeignExchangeForwardForward instrument = new RequestForeignExchangeForwardForward();
        net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header header = new net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header();
        instrument.setHeader(header);
        header.setAssetClass(net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header.AssetClass.FOREIGN_EXCHANGE);
        header.setInstrumentType(net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header.InstrumentType.FORWARD);
        header.setLevel(net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header.Level.INST_REF_DATA_REPORTING);
        header.setUseCase(net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Header.UseCase.FORWARD);

        net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Attributes attributes = new net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Attributes();
        instrument.setAttributes(attributes);

        attributes.setExpiryDate("2017-12-31");
        attributes.setNotionalCurrency(net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Attributes.NotionalCurrency.EUR);
        attributes.setOtherNotionalCurrency(net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.Attributes.OtherNotionalCurrency.DKK);

        sendInstrumentRequest(instrument);
    }

    private void sendInstrumentRequest(Object instrument) throws JsonProcessingException, SessionNotFound {
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
