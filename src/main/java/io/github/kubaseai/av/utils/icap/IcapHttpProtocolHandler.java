package io.github.kubaseai.av.utils.icap;

import org.apache.coyote.Processor;

public class IcapHttpProtocolHandler extends org.apache.coyote.http11.Http11Nio2Protocol {
    public IcapHttpProtocolHandler() {
        super();        
    }

    @Override
    protected Processor createProcessor() {
        return new Http11IcapProcessor(this, adapter);
    }    
}
