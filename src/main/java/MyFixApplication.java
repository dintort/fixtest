import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.ReferenceRate;
import net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.RequestCommoditiesForwardMultiExoticForward;
import net.disactor.fixtest.Credit.RequestCreditSwapIndex.Header;
import net.disactor.fixtest.Credit.RequestCreditSwapIndex.RequestCreditSwapIndex;
import net.disactor.fixtest.Equity.RequestEquityOptionSingleName.RequestEquityOptionSingleName;
import net.disactor.fixtest.Foreign_Exchange.RequestForeignExchangeForwardForward.RequestForeignExchangeForwardForward;
import net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.Attributes;
import net.disactor.fixtest.Rates.RequestRatesSwapFixedFloat.RequestRatesSwapFixedFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix50sp2.SecurityDefinitionRequest;
import quickfix.fix50sp2.SecurityListRequest;
import quickfix.fix50sp2.component.SecurityXML;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashSet;
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
                    String[] split = command.split(" ");
                    String commandName = split[0];
                    if (split.length > 1) {
                        String[] args = new String[split.length - 1];
                        System.arraycopy(split, 1, args, 0, args.length);
                        Class[] argTypes = new Class[args.length];
                        for (int i = 0; i < argTypes.length; i++) {
                            argTypes[i] = String.class;
                        }
                        Method method = this.getClass().getMethod(commandName, argTypes);
                        method.invoke(this, args);
                    } else {
                        Method method = this.getClass().getMethod(commandName);
                        method.invoke(this);
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }
    }

    public void subscribe(String assetClass, String requestType) throws SessionNotFound {
        SecurityListRequest request = new SecurityListRequest();

        request.set(new SecurityReqID("123"));
        request.set(new SecurityListRequestType(SecurityRequestType.REQUEST_LIST_SECURITY_TYPES));
        request.setString(1938, assetClass);
        request.set(new SubscriptionRequestType(requestType.charAt(0)));

        Session.sendToTarget(request, sessionId);
    }

    public void subscribeAll(String requestType) throws SessionNotFound {
        SecurityListRequest request = new SecurityListRequest();

        request.set(new SecurityReqID("123"));
        request.set(new SecurityListRequestType(4));
        request.set(new SubscriptionRequestType(requestType.charAt(0)));

        Session.sendToTarget(request, sessionId);
    }


    public void search(String isin) throws SessionNotFound {

        SecurityDefinitionRequest request = new SecurityDefinitionRequest();

        request.set(new SecurityReqID(UUID.randomUUID().toString()));
        request.set(new SecurityRequestType(SecurityRequestType.REQUEST_SECURITY_IDENTITY_AND_SPECIFICATIONS));
        request.set(new Symbol("N/A"));
        request.set(new SecurityID(isin));
        request.set(new SecurityIDSource(SecurityIDSource.ISIN_NUMBER));

        Session.sendToTarget(request, sessionId);

    }



    public void createCommodity() throws SessionNotFound, JsonProcessingException {

        RequestCommoditiesForwardMultiExoticForward instrument = new RequestCommoditiesForwardMultiExoticForward();
        instrument.setHeader(new net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Header());
        instrument.getHeader().setAssetClass(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Header.AssetClass.COMMODITIES);
        instrument.getHeader().setInstrumentType(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Header.InstrumentType.FORWARD);
        instrument.getHeader().setLevel(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Header.Level.INST_REF_DATA_REPORTING);
        instrument.getHeader().setUseCase(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Header.UseCase.MULTI_EXOTIC_FORWARD);

        instrument.setAttributes(new net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Attributes());

        instrument.getAttributes().setExpiryDate("2017-12-31");
        instrument.getAttributes().setNotionalCurrency(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Attributes.NotionalCurrency.DKK);
        instrument.getAttributes().setDeliveryType(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Attributes.DeliveryType.CASH);
        instrument.getAttributes().setReturnorPayoutTrigger(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Attributes.ReturnorPayoutTrigger.CONTRACT_FOR_DIFFERENCE_CFD);
        instrument.getAttributes().setTransactionType(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Attributes.TransactionType.CRCK);
        instrument.getAttributes().setFinalPriceType(net.disactor.fixtest.Commodities.RequestCommoditiesForwardMultiExoticForward.Attributes.FinalPriceType.ARGM);
        instrument.getAttributes().setReferenceRate(new HashSet<ReferenceRate>() {{
            add(ReferenceRate.ELECTRICITY_NYISO_ZONE_E_DAY_AHEAD);
        }});

        sendInstrumentRequest(instrument);
    }

    public void createEquity() throws SessionNotFound, JsonProcessingException {

        RequestEquityOptionSingleName instrument = new RequestEquityOptionSingleName();
        instrument.setHeader(new net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Header());
        instrument.getHeader().setAssetClass(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Header.AssetClass.EQUITY);
        instrument.getHeader().setInstrumentType(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Header.InstrumentType.OPTION);
        instrument.getHeader().setLevel(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Header.Level.INST_REF_DATA_REPORTING);
        instrument.getHeader().setUseCase(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Header.UseCase.SINGLE_NAME);

        instrument.setAttributes(new net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Attributes());

        instrument.getAttributes().setExpiryDate("2017-12-31");
        instrument.getAttributes().setNotionalCurrency(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Attributes.NotionalCurrency.DKK);
        instrument.getAttributes().setOptionType(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Attributes.OptionType.CALL);
        instrument.getAttributes().setOptionExerciseStyle(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Attributes.OptionExerciseStyle.AMER);
        instrument.getAttributes().setValuationMethodorTrigger(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Attributes.ValuationMethodorTrigger.ASIAN);
        instrument.getAttributes().setDeliveryType(net.disactor.fixtest.Equity.RequestEquityOptionSingleName.Attributes.DeliveryType.CASH);
        instrument.getAttributes().setUnderlyingInstrumentISIN(new HashSet<String>() {{
            add("EZ7B57CQ1M32");
        }});
        instrument.getAttributes().setStrikePrice(3.14159);

        sendInstrumentRequest(instrument);
    }

    public void createCredit() throws SessionNotFound, JsonProcessingException {

        RequestCreditSwapIndex instrument = new RequestCreditSwapIndex();
        instrument.setHeader(new Header());
        instrument.getHeader().setAssetClass(Header.AssetClass.CREDIT);
        instrument.getHeader().setInstrumentType(Header.InstrumentType.SWAP);
        instrument.getHeader().setLevel(Header.Level.INST_REF_DATA_REPORTING);
        instrument.getHeader().setUseCase(Header.UseCase.INDEX);

        instrument.setAttributes(new net.disactor.fixtest.Credit.RequestCreditSwapIndex.Attributes());

        instrument.getAttributes().setExpiryDate("2017-12-31");
        instrument.getAttributes().setNotionalCurrency(net.disactor.fixtest.Credit.RequestCreditSwapIndex.Attributes.NotionalCurrency.DKK);
        instrument.getAttributes().setUnderlyingInstrumentIndex(net.disactor.fixtest.Credit.RequestCreditSwapIndex.Attributes.UnderlyingInstrumentIndex.CUSTOM_INDEX);
        instrument.getAttributes().setUnderlyingInstrumentIndexTermUnit(net.disactor.fixtest.Credit.RequestCreditSwapIndex.Attributes.UnderlyingInstrumentIndexTermUnit.DAYS);
        instrument.getAttributes().setUnderlyingInstrumentIndexTermValue(42);
        instrument.getAttributes().setUnderlyingCreditIndexSeries(13);
        instrument.getAttributes().setUnderlyingCreditIndexVersion(31);
        instrument.getAttributes().setUnderlyingIssuerType(net.disactor.fixtest.Credit.RequestCreditSwapIndex.Attributes.UnderlyingIssuerType.CORPORATE);

        sendInstrumentRequest(instrument);
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
