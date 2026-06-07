package com.paymentswitch.acquirer.handler;

import com.paymentswitch.common.model.MessageDirection;
import com.paymentswitch.common.model.MessageType;
import com.paymentswitch.common.model.ResponseCode;
import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.issuer.IssuerClientPool;
import com.paymentswitch.routing.RoutingService;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

public class AcquirerMessageHandlerTest {

    private EmbeddedChannel channel;

    @After
    public void tearDown() {
        if (channel != null) {
            channel.close();
        }
    }

    @Test
    public void networkManagementRequestIsAnsweredLocallyAs0810() {
        RoutingService routingService = mock(RoutingService.class);
        IssuerClientPool issuerClientPool = mock(IssuerClientPool.class);
        channel = new EmbeddedChannel(new AcquirerMessageHandler(routingService, issuerClientPool, "9009"));

        SwitchMessage request = new SwitchMessage();
        request.setMessageType(MessageType.NETWORK_MANAGEMENT_REQUEST);
        request.setDirection(MessageDirection.INBOUND);
        request.setSystemTraceAuditNumber("000123");
        request.setField(11, "000123");
        request.setField(7, "0605225421");
        request.setField(12, "260605");
        request.setField(13, "0605");
        request.setField(41, "20390059");

        channel.writeInbound(request);
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        SwitchMessage response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(MessageType.NETWORK_MANAGEMENT_RESPONSE, response.getMessageType());
        assertEquals(ResponseCode.APPROVED, response.getResponseCode());
        assertEquals("000123", response.getSystemTraceAuditNumber());
        assertEquals("0605225421", response.getField(7));
        assertEquals("20390059", response.getField(41));

        verifyNoInteractions(routingService);
        verifyNoInteractions(issuerClientPool);
    }

    @Test
    public void authorizationRequestRoutesToIssuerAndReturnsIssuerResponse() {
        RoutingService routingService = mock(RoutingService.class);
        IssuerClientPool issuerClientPool = mock(IssuerClientPool.class);
        channel = new EmbeddedChannel(new AcquirerMessageHandler(routingService, issuerClientPool, "9009"));

        SwitchMessage issuerResponse = new SwitchMessage();
        issuerResponse.setMessageType(MessageType.AUTHORIZATION_RESPONSE);
        issuerResponse.setDirection(MessageDirection.OUTBOUND);
        issuerResponse.setSystemTraceAuditNumber("000451");
        issuerResponse.setResponseCode(ResponseCode.APPROVED);

        when(routingService.route(any(SwitchMessage.class))).thenAnswer(invocation -> {
            SwitchMessage routed = invocation.getArgument(0);
            routed.setIssuerId("9009");
            return routed;
        });
        when(issuerClientPool.send(any(SwitchMessage.class))).thenReturn(CompletableFuture.completedFuture(issuerResponse));

        SwitchMessage request = new SwitchMessage();
        request.setMessageType(MessageType.AUTHORIZATION_REQUEST);
        request.setDirection(MessageDirection.INBOUND);
        request.setPan("9876500000306084");
        request.setSystemTraceAuditNumber("000451");
        request.setProcessingCode(com.paymentswitch.common.model.ProcessingCode.CASH_ADVANCE);
        request.setTransactionAmount(5000L);
        request.setCurrencyCode("566");
        request.setField(7, "0605225421");
        request.setField(11, "000451");
        request.setField(12, "260605");
        request.setField(13, "0605");
        request.setField(14, "3012");
        request.setField(18, "5961");
        request.setField(22, "020");
        request.setField(23, "000");
        request.setField(26, "53");
        request.setField(35, "9876500000306084=30121011123123000");
        request.setField(37, "542131500000");
        request.setField(38, "631513");
        request.setField(39, "00");
        request.setField(41, "20390059");
        request.setField(42, "111120000012000");
        request.setField(49, "566");
        request.setField(51, "566");

        channel.writeInbound(request);
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        SwitchMessage response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(MessageType.AUTHORIZATION_RESPONSE, response.getMessageType());
        assertEquals(ResponseCode.APPROVED, response.getResponseCode());
        assertEquals("000451", response.getSystemTraceAuditNumber());
        assertEquals(MessageDirection.OUTBOUND, response.getDirection());

        ArgumentCaptor<SwitchMessage> captor = ArgumentCaptor.forClass(SwitchMessage.class);
        verify(routingService, times(1)).route(captor.capture());
        verify(issuerClientPool, times(1)).send(any(SwitchMessage.class));
        assertEquals("9009", captor.getValue().getIssuerId());
        assertEquals("000451", captor.getValue().getSystemTraceAuditNumber());
        assertEquals("9876500000306084", captor.getValue().getPan());
    }
}
