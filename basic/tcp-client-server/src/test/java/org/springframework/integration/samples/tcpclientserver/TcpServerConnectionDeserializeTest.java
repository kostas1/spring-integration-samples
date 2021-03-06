/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.samples.tcpclientserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;
import org.springframework.integration.samples.tcpclientserver.support.CustomTestContextLoader;
import org.springframework.integration.samples.tcpclientserver.support.ServerUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

/**
 * Shows an example of using the Stx/Etx stream framing serializers that are included with
 * Spring Integration. We can be confident that the streams are properly handled because we
 * explicitly send a stream with the Stx/Etx frame and the beginning and end of the actual
 * content and the Server is configured to be able to handle the frame. In the asserts, we
 * assert that the payload, once it reaches a component (in this case, the message listener
 * we create and attach to the incomingServerChannel), does not have any of the Stx/Etx bytes.
 *
 * @author: ceposta
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=CustomTestContextLoader.class,
	locations = {"/META-INF/spring/integration/tcpServerConnectionDeserialize-context.xml"})
@DirtiesContext
public class TcpServerConnectionDeserializeTest {

    @Autowired
	SimpleGateway gw;

    @Autowired
    @Qualifier("incomingServerChannel")
    MessageChannel incomingServerChannel;

	@Autowired
	AbstractServerConnectionFactory crLfServer;

	@Before
	public void setup() {
		ServerUtils.waitListening(this.crLfServer);
	}

    @Test
    public void testHappyPath() {

        // add a listener to this channel, otherwise there is not one defined
        // the reason we use a listener here is so we can assert truths on the
        // message and/or payload
        SubscribableChannel channel = (SubscribableChannel) incomingServerChannel;
        channel.subscribe(new AbstractReplyProducingMessageHandler(){

            @Override
            protected Object handleRequestMessage(Message<?> requestMessage) {
                byte[] payload = (byte[]) requestMessage.getPayload();

                // we assert during the processing of the messaging that the
                // payload is just the content we wanted to send without the
                // framing bytes (STX/ETX)
                assertEquals("Hello World!", new String(payload));
                return requestMessage;
            }
        });

        String sourceMessage = wrapWithStxEtx("Hello World!");
        String result = gw.send(sourceMessage);
        System.out.println(result);
        assertEquals("Hello World!", result);
    }

    /**
     * Show, explicitly, how the stream would look if you had to manually create it.
     *
     * See more about TCP synchronous communication for more about framing the stream
     * with STX/ETX:  http://en.wikipedia.org/wiki/Binary_Synchronous_Communications
     *
     * @param content
     * @return a string that is wrapped with the STX/ETX framing bytes
     */
    private String wrapWithStxEtx(String content) {
        StringWriter writer = new StringWriter();
        writer.write(ByteArrayStxEtxSerializer.STX);
        writer.write(content);
        writer.write(ByteArrayStxEtxSerializer.ETX);
        return writer.toString();
    }
}
